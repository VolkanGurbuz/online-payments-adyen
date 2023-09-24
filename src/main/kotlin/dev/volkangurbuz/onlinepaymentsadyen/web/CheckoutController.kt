package dev.volkangurbuz.onlinepaymentsadyen.web

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping


@Controller
class CheckoutController {

    @Value("\${adyen.clientKey}")
    private lateinit var clientKey: String

    private  var log = LoggerFactory.getLogger(CheckoutController::class.java)


    @GetMapping("/")
    fun preview(): String {
        return "preview"
    }

    @GetMapping("/checkout")
    fun checkout(model: Model): String{
        model.addAttribute("clientKey", clientKey)

        return "checkout"
    }




}