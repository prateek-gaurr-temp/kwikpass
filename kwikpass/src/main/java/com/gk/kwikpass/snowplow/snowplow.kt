package com.gk.kwikpass.snowplow

import android.content.Context
import com.gk.kwikpass.IdfaAid.IdfaAidModule
import com.gk.kwikpass.config.KwikPassCache
import com.gk.kwikpass.config.KwikPassConfig
import com.gk.kwikpass.config.KwikPassKeys
import com.gk.kwikpass.initializer.ApplicationCtx
import com.snowplowanalytics.snowplow.event.PageView
import com.snowplowanalytics.snowplow.event.SelfDescribing
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object Snowplow {
    private val TAG = "Snowplow"

    // Data classes for event arguments
    data class TrackProductEventContext(
        val product_id: String,
        val img_url: String,
        val variant_id: String,
        val product_name: String,
        val product_price: String,
        val product_handle: String,
        val type: String
    )

    data class TrackProductEventArgs(
        val cart_id: String,
        val product_id: String,
        val pageUrl: String,
        val variant_id: String,
        val img_url: String? = null,
        val name: String? = null,
        val price: String? = null,
        val handle: String? = null
    )

    data class TrackCartEventArgs(
        val cart_id: String
    )

    data class TrackCollectionsEventArgs(
        val cart_id: String,
        val collection_id: String,
        val name: String,
        val image_url: String? = null,
        val handle: String? = null
    )

    data class CollectionContext(
        val collection_id: String,
        val name: String,
        val image_url: String? = null,
        val handle: String? = null
    )

    data class TrackOtherEventArgs(
        val cart_id: String? = null
    )

    data class StructuredProps(
        val category: String,
        val action: String,
        val label: String? = null,
        val property: String? = null,
        val value: Double? = null,
        val property_1: String? = null,
        val value_1: String? = null,
        val property_2: String? = null,
        val value_2: String? = null,
        val property_3: String? = null,
        val value_3: String? = null,
        val property_4: String? = null,
        val value_4: String? = null,
        val property_5: String? = null,
        val value_5: String? = null
    )

    // Helper functions
    private suspend fun getEnvironment(context: Context): String {
        val env = KwikPassCache.getInstance(context).getValue(KwikPassKeys.GK_ENVIRONMENT)
        return when (env) {
            "production" -> "production"
            else -> "sandbox"
        }
    }

    private suspend fun createContext(schemaPath: String, data: Map<String, Any>): SelfDescribingJson {
        val environment = getEnvironment(ApplicationCtx.get())
        val schema = "iglu:${KwikPassConfig.getConfig(environment).schemaVendor}/$schemaPath"

        val json = SelfDescribingJson(
            schema,
            data
        )

        return json
    }

    private suspend fun getCartContext(cartId: String): SelfDescribingJson? {
        if (cartId.isBlank()) return null
        return SelfDescribingJson(
            "iglu:com.shopify/cart/jsonschema/1-0-0",
            mapOf(
                "id" to cartId,
                "token" to cartId
            ))
    }

    private suspend fun getUserContext(): SelfDescribingJson? {
        val context = ApplicationCtx.get()
        val userJson = KwikPassCache.getInstance(context).getValue(KwikPassKeys.GK_VERIFIED_USER)

        if(userJson == null) return null

        // Initialize with empty JSONObject
        val user = try {
            if (!userJson.isNullOrEmpty()) {
                // Try parsing as regular JSON first
                try {
                    JSONObject(userJson)
                } catch (e: Exception) {
                    // If that fails, try parsing as key=value format
                    val map = userJson
                        .trim('{', '}')  // Remove outer braces
                        .split(",")      // Split by comma
                        .map { it.trim() }.associate { pair ->
                            val (key, value) = pair.split("=", limit = 2)
                            key.trim() to value.trim()
                        }
                    JSONObject(map)
                }
            } else {
                JSONObject()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            JSONObject()
        }

        // Helper function to safely get string values
        fun safeGetString(key: String): String {
            return try {
                user.optString(key, "")
            } catch (e: Exception) {
                ""
            }
        }

        val phone = safeGetString("phone")
        val cleanedPhone = phone.replace("^+91".toRegex(), "").replace("[^0-9]".toRegex(), "")
        
        // Try to convert to Long first (since phone numbers can be too large for Int)
        val numericPhoneNumber = try {
            cleanedPhone.toLongOrNull()
        } catch (e: Exception) {
            null
        }

        val email = safeGetString("email")

        if (numericPhoneNumber == null && email.isBlank()) {
            return null
        }

        return createContext("user/jsonschema/1-0-0", mapOf(
            "phone" to (if (numericPhoneNumber != null) numericPhoneNumber.toString() else ""),
            "email" to email
        ))
    }

    private suspend fun getProductContext(args: TrackProductEventContext): SelfDescribingJson {
        return createContext("product/jsonschema/1-1-0", mapOf(
            "product_id" to args.product_id,
            "img_url" to (args.img_url ?: ""),
            "variant_id" to args.variant_id,
            "product_name" to (args.product_name ?: ""),
            "product_price" to (args.product_price ?: ""),
            "product_handle" to (args.product_handle ?: ""),
            "type" to (args.type ?: "product")
        ))
    }

    private suspend fun getDeviceInfoContext(): SelfDescribingJson {
        val context = ApplicationCtx.get()
        val deviceFCM = KwikPassCache.getInstance(context).getValue(KwikPassKeys.GK_NOTIFICATION_TOKEN)
        val deviceInfoJson = KwikPassCache.getInstance(context).getValue(KwikPassKeys.GK_DEVICE_INFO)
        
        // Initialize with empty JSONObject
        val deviceInfo = try {
            if (!deviceInfoJson.isNullOrEmpty()) {
                // Convert the string to a map first
                val map = deviceInfoJson
                    .trim('{', '}')  // Remove outer braces
                    .split(",")      // Split by comma
                    .map { it.trim() }.associate { pair ->
                        val (key, value) = pair.split("=", limit = 2)
                        key.trim() to value.trim()
                    }

                // Convert map to JSONObject
                JSONObject(map)
            } else {
                JSONObject()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            JSONObject()
        }

        val advertisingInfo = IdfaAidModule.getInstance(context).getAdvertisingInfo()

        // Helper function to safely get string values
        fun safeGetString(key: String) = try {
            deviceInfo.optString(key, "")
        } catch (e: Exception) {
            ""
        }

        return createContext("user_device/jsonschema/1-0-0", mapOf(
            "device_id" to safeGetString(KwikPassKeys.GK_DEVICE_UNIQUE_ID),
            "android_ad_id" to (advertisingInfo.id ?: "").toString(),
            "ios_ad_id" to "",
            "fcm_token" to (deviceFCM ?: "dRJ1EO12QgGeWxw2GJkymY:APA91bH2NMhWoJVHDTdvXUNqCoD9-AKUdyLo16iPAIzumwM9J57lAQE7Bw6KPE1tEwfU5Abzi594ZuVvlSApI0lXgmiWNGXqhdJS2n7mo3eNMxwf2yspXx0").toString(),
            "app_domain" to safeGetString(KwikPassKeys.GK_APP_DOMAIN),
            "device_type" to "android",
            "app_version" to safeGetString(KwikPassKeys.GK_APP_VERSION)
        ))
    }

    private fun trimCartId(cartId: String): String {
        return cartId.split("/").lastOrNull() ?: cartId
    }

    // Event tracking functions
    suspend fun trackProductEvent(args: TrackProductEventArgs) {
        val context = ApplicationCtx.get()

        withContext(Dispatchers.IO) {
            try {
                val cache = KwikPassCache.getInstance(context)
                val isTrackingEnabled = cache.getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"
                if (!isTrackingEnabled) return@withContext

                val mid = cache.getValue(KwikPassKeys.GK_MERCHANT_ID)
                val env = cache.getValue(KwikPassKeys.GK_ENVIRONMENT)

                val tracker = SnowplowClient.getSnowplowClient(context, env, mid)
                if (tracker == null) return@withContext

                var cartId = args.cart_id
                if (cartId.contains("gid://shopify/Cart/")) {
                    cartId = trimCartId(cartId)
                }

                val pageView = PageView(args.pageUrl)
                    .pageTitle(args.name ?: "")

                val contextDetails = TrackProductEventContext(
                    product_id = args.product_id,
                    img_url = args.img_url ?: "",
                    variant_id = args.variant_id,
                    product_name = args.name ?: "",
                    product_price = args.price ?: "",
                    product_handle = args.handle ?: "",
                    type = "product"
                )

                // Get all contexts with null safety
                val productJson = getProductContext(contextDetails)
                if (productJson != null) {
                    pageView.entities.add(productJson)
                }

                val deviceJson = getDeviceInfoContext()
                if (deviceJson != null) {
                    pageView.entities.add(deviceJson)
                }

                if (!cartId.isNullOrBlank()) {
                    val cartJson = getCartContext(cartId)
                    if (cartJson != null) {
                        pageView.entities.add(cartJson)
                    }
                }

                val userJson = getUserContext()
                if (userJson != null) {
                    pageView.entities.add(userJson)
                }

                println("PRODUCT EVENT TRACKING ENTITIES ${pageView.entities}")
                tracker.track(pageView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun trackCartEvent(args: TrackCartEventArgs) {
        val context = ApplicationCtx.get()
        withContext(Dispatchers.IO) {
            try {
                val isTrackingEnabled = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"
                if (!isTrackingEnabled) return@withContext

                val merchantUrl = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.GK_MERCHANT_URL) ?: ""
                val pageUrl = "https://$merchantUrl/cart"

                val tracker = SnowplowClient.getSnowplowClient(context)
                if (tracker == null) return@withContext

                var cartId = args.cart_id
                if (cartId.contains("gid://shopify/Cart/")) {
                    cartId = trimCartId(cartId)
                }

                val pageView = PageView(pageUrl)

                // Get all contexts with null safety
                val deviceJson = getDeviceInfoContext()
                if (deviceJson != null) {
                    pageView.entities.add(deviceJson)
                }

                if (!cartId.isNullOrBlank()) {
                    val cartJson = getCartContext(cartId)
                    if (cartJson != null) {
                        pageView.entities.add(cartJson)
                    }
                }

                val userJson = getUserContext()
                if (userJson != null) {
                    pageView.entities.add(userJson)
                }

                println("CART EVENT TRACKING ENTITIES ${pageView.entities}")
                tracker.track(pageView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun trackCollectionsEvent(args: TrackCollectionsEventArgs) {
        val context = ApplicationCtx.get()
        withContext(Dispatchers.IO) {
            try {
                val isTrackingEnabled = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"
                if (!isTrackingEnabled) return@withContext

                val merchantUrl = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.GK_MERCHANT_URL) ?: ""
                val pageUrl = if (args.handle != null && merchantUrl.isNotEmpty()) {
                    "https://$merchantUrl/collections/${args.handle}"
                } else ""

                val tracker = SnowplowClient.getSnowplowClient(context)
                if (tracker == null) return@withContext

                var cartId = args.cart_id
                if (cartId.contains("gid://shopify/Cart/")) {
                    cartId = trimCartId(cartId)
                }

                val pageView = PageView(pageUrl)
                    .pageTitle(args.name)

                // Get all contexts with null safety
                val deviceJson = getDeviceInfoContext()
                if (deviceJson != null) {
                    pageView.entities.add(deviceJson)
                }

                if (!cartId.isNullOrBlank()) {
                    val cartJson = getCartContext(cartId)
                    if (cartJson != null) {
                        pageView.entities.add(cartJson)
                    }
                }

                val userJson = getUserContext()
                if (userJson != null) {
                    pageView.entities.add(userJson)
                }

                println("COLLECTIONS EVENT TRACKING ENTITIES ${pageView.entities}")
                tracker.track(pageView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun trackOtherEvent(args: TrackOtherEventArgs? = null) {
        val context = ApplicationCtx.get()
        withContext(Dispatchers.IO) {
            try {
                val isTrackingEnabled = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"
                if (!isTrackingEnabled) return@withContext

                val merchantUrl = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.GK_MERCHANT_URL) ?: ""
                val pageUrl = if (merchantUrl.isNotEmpty()) "https://$merchantUrl/cart" else ""

                val tracker = SnowplowClient.getSnowplowClient(context)
                if (tracker == null) return@withContext

                var cartId = args?.cart_id ?: ""
                if (cartId.contains("gid://shopify/Cart/")) {
                    cartId = trimCartId(cartId)
                }

                val pageView = PageView(pageUrl)

                // Get all contexts with null safety
                val deviceJson = getDeviceInfoContext()
                if (deviceJson != null) {
                    pageView.entities.add(deviceJson)
                }

                if (!cartId.isNullOrBlank()) {
                    val cartJson = getCartContext(cartId)
                    if (cartJson != null) {
                        pageView.entities.add(cartJson)
                    }
                }

                val userJson = getUserContext()
                if (userJson != null) {
                    pageView.entities.add(userJson)
                }

                println("OTHER EVENT TRACKING ENTITIES ${pageView.entities}")
                tracker.track(pageView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun sendCustomEventToSnowPlow(eventObject: Map<String, Any>) {
        val context = ApplicationCtx.get()
        withContext(Dispatchers.IO) {
            try {
                val isTrackingEnabled = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"
                if (!isTrackingEnabled) return@withContext

                val mid = KwikPassCache.getInstance(context).getValue(KwikPassKeys.GK_MERCHANT_ID) ?: ""
                val env = KwikPassCache.getInstance(context).getValue(KwikPassKeys.GK_ENVIRONMENT) ?: "sandbox"

                val tracker = SnowplowClient.getSnowplowClient(context, env, mid)
                if (tracker == null) return@withContext

                val environment = getEnvironment(context)
                val schema = "iglu:${KwikPassConfig.getConfig(environment).schemaVendor}/structured/jsonschema/1-0-0"

                // Create the event data with filtered values
                val filteredEvents = filterEventValuesAsPerStructSchema(eventObject)
                
                // Create the event data map
                val eventData =  SelfDescribingJson(
                    schema, filteredEvents
                )

                // Create and track the event
                val event = SelfDescribing(eventData)

                // Add common contexts if available
                val deviceJson = getDeviceInfoContext()
                if (deviceJson != null) {
                    event.entities.add(deviceJson)
                }

                val userJson = getUserContext()
                if (userJson != null) {
                    event.entities.add(userJson)
                }

                println("CUSTOM EVENT TRACKING ENTITIES ${event.entities}")
                tracker.track(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun snowplowStructuredEvent(context: Context, args: StructuredProps) {
        withContext(Dispatchers.IO) {
            try {
                val isTrackingEnabled = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"
                if (!isTrackingEnabled) return@withContext

                val tracker = SnowplowClient.getSnowplowClient(context)
                if (tracker == null) return@withContext

                val environment = getEnvironment(context)
                val schema = "iglu:${KwikPassConfig.getConfig(environment).schemaVendor}/structured/jsonschema/1-0-0"

                val eventData = SelfDescribingJson(
                    schema,
                    mapOf(
                        "category" to args.category,
                        "action" to args.action,
                        "label" to (args.label ?: ""),
                        "property" to (args.property ?: ""),
                        "value" to (args.value ?: 0),
                        "property_1" to (args.property_1 ?: ""),
                        "value_1" to (args.value_1 ?: ""),
                        "property_2" to (args.property_2 ?: ""),
                        "value_2" to (args.value_2 ?: ""),
                        "property_3" to (args.property_3 ?: ""),
                        "value_3" to (args.value_3 ?: ""),
                        "property_4" to (args.property_4 ?: ""),
                        "value_4" to (args.value_4 ?: ""),
                        "property_5" to (args.property_5 ?: ""),
                        "value_5" to (args.value_5 ?: "")
                    )
                )

                val event = SelfDescribing(eventData)

                // Add common contexts if available
                val deviceJson = getDeviceInfoContext()
                if (deviceJson != null) {
                    event.entities.add(deviceJson)
                }

                val userJson = getUserContext()
                if (userJson != null) {
                    event.entities.add(userJson)
                }

                println("STRUCTURED EVENT TRACKING ENTITIES ${event.entities}")
                tracker.track(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun filterEventValuesAsPerStructSchema(eventObject: Map<String, Any>): Map<String, Any> {
        val intTypes = setOf("value", "value_1", "value_2", "value_3", "value_4", "value_5")
        return eventObject.mapValues { (key, value) ->
            when {
                intTypes.contains(key) -> {
                    when (value) {
                        is Number -> value.toInt()
                        is String -> value.toIntOrNull() ?: 0
                        else -> 0
                    }
                }
                else -> value.toString()
            }
        }
    }
}
