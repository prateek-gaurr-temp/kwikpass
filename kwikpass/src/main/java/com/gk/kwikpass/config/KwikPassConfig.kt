package com.gk.kwikpass.config

object KwikPassConfig {
    data class CheckoutUrls(
        val shopify: String,
        val custom: String
    )

    data class Environment(
        val baseUrl: String,
        val snowplowUrl: String,
        val schemaVendor: String,
        val checkoutUrl: CheckoutUrls
    )

    val production = Environment(
        baseUrl = "https://gkx.gokwik.co/kp/api/v1/",
        snowplowUrl = "https://sp-kf-collector-prod.gokwik.io",
        schemaVendor = "co.gokwik",
        checkoutUrl = CheckoutUrls(
            shopify = "https://pdp.gokwik.co/app/appmaker-kwik-checkout.html?storeInfo=",
            custom = "https://pdp.gokwik.co/v4/auto.html"
        )
    )

    val sandbox = Environment(
        baseUrl = "https://api-gw-v4.dev.gokwik.io/sandbox/kp/api/v1/",
        snowplowUrl = "https://sp-kf-collector.dev.gokwik.io/",
        schemaVendor = "in.gokwik.kwikpass",
        checkoutUrl = CheckoutUrls(
            shopify = "https://sandbox.pdp.gokwik.co/app/appmaker-kwik-checkout.html?storeInfo=",
            custom = "https://sandbox.pdp.gokwik.co/v4/auto.html"
        )
    )

    fun getConfig(environment: String): Environment {
        return when (environment.lowercase()) {
            "sandbox" -> sandbox
            else -> production
        }
    }
}