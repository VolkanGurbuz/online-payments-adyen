package dev.volkangurbuz.onlinepaymentsadyen.api

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import com.adyen.Client;
import com.adyen.enums.Environment;
import com.adyen.model.Amount
import com.adyen.model.PaymentRequest
import com.adyen.model.checkout.*;
import com.adyen.model.nexo.PaymentResponse
import com.adyen.service.Checkout
import com.adyen.service.exception.ApiException;
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value


@RequestMapping("/api")
@RestController
class CheckoutResource {

   private  var log = LoggerFactory.getLogger(CheckoutResource::class.java)
    private lateinit var checkout: Checkout

    @Value("\${adyen.apiKey}")
    private lateinit var apiKey: String

    @Value("\${adyen.merchantAccount}")
    private lateinit var merchantAccount : String

    @PostConstruct
    fun init()  {
    val client = Client(apiKey, Environment.TEST)
    checkout = Checkout(client)
    }


    /**
     * `POST  /getPaymentMethods` : Get valid payment methods.
     *
     * @return the [ResponseEntity] with status `200 (Ok)` and with body the paymentMethods response.
     * @throws IOException  from Adyen API.
     * @throws ApiException from Adyen API.
     */
    @PostMapping("/getPaymentMethods")
    @Throws(IOException::class, ApiException::class)
    fun paymentMethods(): ResponseEntity<PaymentMethodsResponse> {
        val paymentMethodsRequest = PaymentMethodsRequest()
        paymentMethodsRequest.channel = PaymentMethodsRequest.ChannelEnum.WEB
        paymentMethodsRequest.merchantAccount =merchantAccount
        log.info("REST request to get Adyen payment methods {}", paymentMethodsRequest)
        val response: PaymentMethodsResponse? = checkout.paymentMethods(paymentMethodsRequest)
        return ResponseEntity.ok()
            .body(response)
    }

    /**
     * `POST  /initiatePayment` : Make a payment.
     *
     * @return the [ResponseEntity] with status `200 (Ok)` and with body the paymentMethods response.
     * @throws IOException  from Adyen API.
     * @throws ApiException from Adyen API.
     */
    @PostMapping("/initiatePayment")
    @Throws(IOException::class, ApiException::class)
    fun payments(
        @RequestHeader host: String,
        @RequestBody body: PaymentRequest,
        request: HttpServletRequest
    ): ResponseEntity<PaymentResponse> {
        val paymentRequest = PaymentRequest()
        val orderRef = UUID.randomUUID().toString()
        val amount: Amount = Amount()
            .currency("EUR")
            .value(10000L) // value is 10€ in minor units
        paymentRequest.setMerchantAccount(this.applicationProperty.getMerchantAccount()) // required
        paymentRequest.setChannel(PaymentRequest.ChannelEnum.WEB)
        paymentRequest.setReference(orderRef) // required
        paymentRequest.setReturnUrl(request.scheme + "://" + host + "/api/handleShopperRedirect?orderRef=" + orderRef)
        paymentRequest.setAmount(amount)
        // set lineItems required for some payment methods (ie Klarna)
        paymentRequest.setLineItems(
            Arrays.asList(
                LineItem().quantity(1L).amountIncludingTax(5000L).description("Sunglasses"),
                LineItem().quantity(1L).amountIncludingTax(5000L).description("Headphones")
            )
        )
        // required for 3ds2 native flow
        paymentRequest.setAdditionalData(Collections.singletonMap("allow3DS2", "true"))
        // required for 3ds2 native flow
        paymentRequest.setOrigin(request.scheme + "://" + host)
        // required for 3ds2
        paymentRequest.setBrowserInfo(body.getBrowserInfo())
        // required by some issuers for 3ds2
        paymentRequest.setShopperIP(request.remoteAddr)
        paymentRequest.setPaymentMethod(body.getPaymentMethod())
        log.info("REST request to make Adyen payment {}", paymentRequest)
        val response: Unit = paymentsApi.payments(paymentRequest)
        return ResponseEntity.ok()
            .body(response)
    }

    /**
     * `POST  /submitAdditionalDetails` : Make a payment.
     *
     * @return the [ResponseEntity] with status `200 (Ok)` and with body the paymentMethods response.
     * @throws IOException  from Adyen API.
     * @throws ApiException from Adyen API.
     */
    @PostMapping("/submitAdditionalDetails")
    @Throws(IOException::class, ApiException::class)
    fun payments(@RequestBody detailsRequest: PaymentDetailsRequest?): ResponseEntity<PaymentDetailsResponse> {
        log.info("REST request to make Adyen payment details {}", detailsRequest)
        val response: Unit = paymentsApi.paymentsDetails(detailsRequest)
        return ResponseEntity.ok()
            .body(response)
    }

    /**
     * `GET  /handleShopperRedirect` : Handle redirect during payment.
     *
     * @return the [RedirectView] with status `302`
     * @throws IOException  from Adyen API.
     * @throws ApiException from Adyen API.
     */
    @GetMapping("/handleShopperRedirect")
    @Throws(IOException::class, ApiException::class)
    fun redirect(
        @RequestParam(required = false) payload: String?,
        @RequestParam(required = false) redirectResult: String?,
        @RequestParam orderRef: String?
    ): RedirectView {
        val detailsRequest = PaymentDetailsRequest()
        val details = PaymentCompletionDetails()
        if (redirectResult != null && !redirectResult.isEmpty()) {
            details.redirectResult(redirectResult)
        } else if (payload != null && !payload.isEmpty()) {
            details.payload(payload)
        }
        detailsRequest.setDetails(details)
        return getRedirectView(detailsRequest)
    }

    @Throws(ApiException::class, IOException::class)
    private fun getRedirectView(detailsRequest: PaymentDetailsRequest): RedirectView {
        log.info("REST request to handle payment redirect {}", detailsRequest)
        val response: Unit = paymentsApi.paymentsDetails(detailsRequest)
        var redirectURL = "/result/"
        redirectURL += when (response.getResultCode()) {
            AUTHORISED -> "success"
            PENDING, RECEIVED -> "pending"
            REFUSED -> "failed"
            else -> "error"
        }
        return RedirectView(redirectURL + "?reason=" + response.getResultCode())
    }


}