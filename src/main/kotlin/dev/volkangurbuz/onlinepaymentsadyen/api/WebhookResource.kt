package dev.volkangurbuz.onlinepaymentsadyen.api


import com.adyen.model.notification.NotificationRequest;
import com.adyen.model.notification.NotificationRequestItem;
import com.adyen.util.HMACValidator;
import dev.volkangurbuz.onlinepaymentsadyen.ApplicationProperty
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.SignatureException;

class WebhookResource {

    private val log: Logger = LoggerFactory.getLogger(WebhookResource::class.java)

    private val applicationProperty: ApplicationProperty? = null

    @Autowired
    fun WebhookResource(applicationProperty: ApplicationProperty?) {
        this.applicationProperty = applicationProperty
        if (this.applicationProperty.getHmacKey() == null) {
            log.warn("ADYEN_HMAC_KEY is UNDEFINED (Webhook cannot be authenticated)")
            //throw new RuntimeException("ADYEN_HMAC_KEY is UNDEFINED");
        }
    }

    /**
     * Process incoming Webhook notification: get NotificationRequestItem, validate HMAC signature,
     * consume the event asynchronously, send response ["accepted"]
     *
     * @param json Payload of the webhook event
     * @return
     */
    @PostMapping("/webhooks/notifications")
    @Throws(IOException::class)
    fun webhooks(@RequestBody json: String?): ResponseEntity<String> {

        // from JSON string to object
        val notificationRequest: Unit = NotificationRequest.fromJson(json)

        // fetch first (and only) NotificationRequestItem
        val notificationRequestItem: Unit = notificationRequest.getNotificationItems().stream().findFirst()
        if (notificationRequestItem.isPresent()) {
            val item: Unit = notificationRequestItem.get()
            try {
                if (getHmacValidator().validateHMAC(item, applicationProperty.getHmacKey())) {
                    log.info(
                        """
                            Received webhook with event {} : 
                            Merchant Reference: {}
                            Alias : {}
                            PSP reference : {}
                            """
                            .trimIndent(),
                        item.getEventCode(),
                        item.getMerchantReference(),
                        item.getAdditionalData().get("alias"),
                        item.getPspReference()
                    )

                    // consume event asynchronously
                    consumeEvent(item)
                } else {
                    // invalid HMAC signature: do not send [accepted] response
                    log.warn("Could not validate HMAC signature for incoming webhook message: {}", item)
                    throw RuntimeException("Invalid HMAC signature")
                }
            } catch (e: SignatureException) {
                // Unexpected error during HMAC validation: do not send [accepted] response
                log.error("Error while validating HMAC Key", e)
            }
        } else {
            // Unexpected event with no payload: do not send [accepted] response
            log.warn("Empty NotificationItem")
        }

        // Acknowledge event has been consumed
        return ResponseEntity.ok().body("[accepted]")
    }

    // process payload asynchronously
    fun consumeEvent(item: NotificationRequestItem?) {
        // add item to DB, queue or different thread

        // example: send to Kafka consumer

        // producer.send(producerRecord);
        // producer.flush();
        // producer.close();
    }

    @Bean
    fun getHmacValidator(): HMACValidator {
        return HMACValidator()
    }
}