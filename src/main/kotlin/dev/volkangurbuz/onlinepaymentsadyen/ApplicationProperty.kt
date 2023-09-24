package dev.volkangurbuz.onlinepaymentsadyen

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class ApplicationProperty {
    @Value("\${server.port}")
    var serverPort = 0

    @Value("\${ADYEN_API_KEY:#{null}}")
    var apiKey: String? = null

    @Value("\${ADYEN_MERCHANT_ACCOUNT:#{null}}")
    var merchantAccount: String? = null

    @Value("\${ADYEN_CLIENT_KEY:#{null}}")
    var clientKey: String? = null

    @Value("\${ADYEN_HMAC_KEY:#{null}}")
    var hmacKey: String? = null

    fun getServerPort(): Int {
        return serverPort
    }

    fun setServerPort(serverPort: Int) {
        this.serverPort = serverPort
    }

    fun getApiKey(): String {
        return apiKey!!
    }

    fun setApiKey(apiKey: String?) {
        this.apiKey = apiKey
    }

    fun getMerchantAccount(): String {
        return merchantAccount!!
    }

    fun setMerchantAccount(merchantAccount: String?) {
        this.merchantAccount = merchantAccount
    }

    fun getClientKey(): String {
        return clientKey!!
    }

    fun setClientKey(clientKey: String?) {
        this.clientKey = clientKey
    }

    fun getHmacKey(): String {
        return hmacKey!!
    }

    fun setHmacKey(hmacKey: String?) {
        this.hmacKey = hmacKey
    }
}

