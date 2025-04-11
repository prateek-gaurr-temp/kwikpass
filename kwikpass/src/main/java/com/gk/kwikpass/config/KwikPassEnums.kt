package com.gk.kwikpass.config

enum class KwikPassEnvironment {
    SANDBOX,
    PRODUCTION;

    companion object {
        fun fromString(value: String): KwikPassEnvironment {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                SANDBOX // Default to sandbox if invalid
            }
        }
    }
}

sealed class KwikPassMerchantType {
    object SHOPIFY : KwikPassMerchantType()
    object OTHER : KwikPassMerchantType()

    companion object {
        fun fromString(value: String): KwikPassMerchantType {
            return when (value.lowercase()) {
                "shopify" -> SHOPIFY
                else -> OTHER
            }
        }
    }
}

sealed class KwikPassUserState {
    object ENABLED : KwikPassUserState()
    object DISABLED : KwikPassUserState()

    companion object {
        fun fromString(value: String): KwikPassUserState {
            return when (value.uppercase()) {
                "ENABLED" -> ENABLED
                "DISABLED" -> DISABLED
                else -> ENABLED // Default to enabled if invalid
            }
        }
    }
} 