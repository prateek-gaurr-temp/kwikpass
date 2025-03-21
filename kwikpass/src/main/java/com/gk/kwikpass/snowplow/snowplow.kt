package com.gk.kwikpass.snowplow

import android.content.Context
import android.os.Build
import com.gk.kwikpass.IdfaAid.IdfaAidModule
import com.gk.kwikpass.config.KwikPassCache
import com.gk.kwikpass.config.KwikPassConfig
import com.gk.kwikpass.config.KwikPassKeys
import com.gk.kwikpass.initializer.ApplicationCtx
import com.snowplowanalytics.snowplow.event.PageView
import com.snowplowanalytics.snowplow.event.SelfDescribing
import com.snowplowanalytics.snowplow.event.Structured
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
        val product_id: String,
        val pageUrl: String,
        val variant_id: String? = null,
        val img_url: String? = null,
        val name: String? = null,
        val price: String? = null,
        val handle: String? = null
    )

    data class TrackCartEventArgs(
        val cart_id: String
    )

    data class TrackCollectionsEventArgs(
        val collection_id: String,
        val name: String,
        val cart_id: String? = null,
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
        return KwikPassCache.getInstance(context).getValue(KwikPassKeys.GK_ENVIRONMENT) ?: "sandbox"
    }

    private suspend fun createContext(schemaPath: String, data: Map<String, Any>): SelfDescribingJson {
        val environment = getEnvironment(ApplicationCtx.get())
        val schema = "iglu:${KwikPassConfig.getConfig(environment).schemaVendor}/$schemaPath"
        return SelfDescribingJson(schema, JSONObject(data.toString()))
    }

    private suspend fun getCartContext(cartId: String): SelfDescribingJson {
        return createContext("cart/jsonschema/1-0-0", mapOf(
            "id" to cartId,
            "token" to cartId
        ))
    }

    private suspend fun getUserContext(context: Context): SelfDescribingJson? {
        val userJson = KwikPassCache.getInstance(context).getValue(KwikPassKeys.GK_VERIFIED_USER)
        if (userJson.isNullOrEmpty()) return null

        val user = JSONObject(userJson)
        val phone = user.optString("phone", "").replace("^+91".toRegex(), "")
        val numericPhoneNumber = phone.toIntOrNull()

        return createContext("user/jsonschema/1-0-0", mapOf(
            "phone" to (numericPhoneNumber?.toString() ?: ""),
            "email" to user.optString("email", "")
        ))
    }

    private suspend fun getProductContext(args: TrackProductEventArgs): SelfDescribingJson {
        return createContext("product/jsonschema/1-1-0", mapOf(
            "product_id" to args.product_id,
            "img_url" to (args.img_url ?: ""),
            "variant_id" to (args.variant_id ?: ""),
            "product_name" to (args.name ?: ""),
            "product_price" to (args.price ?: ""),
            "product_handle" to (args.handle ?: ""),
            "type" to "product"
        ))
    }

    private suspend fun getDeviceInfoContext(context: Context): SelfDescribingJson {
        val deviceFCM = KwikPassCache.getInstance(context).getValue(KwikPassKeys.GK_NOTIFICATION_TOKEN)
        val deviceInfoJson = KwikPassCache.getInstance(context).getValue(KwikPassKeys.GK_DEVICE_INFO)
        val deviceInfo = if (!deviceInfoJson.isNullOrEmpty()) JSONObject(deviceInfoJson) else JSONObject()

        val advertisingInfo = IdfaAidModule.getInstance(context).getAdvertisingInfo()

        return createContext("user_device/jsonschema/1-0-0", mapOf(
            "device_id" to deviceInfo.optString(KwikPassKeys.GK_DEVICE_UNIQUE_ID, ""),
            "android_ad_id" to (advertisingInfo.id ?: ""),
            "ios_ad_id" to "",
            "fcm_token" to (deviceFCM ?: ""),
            "app_domain" to deviceInfo.optString(KwikPassKeys.GK_APP_DOMAIN, ""),
            "device_type" to "android",
            "app_version" to deviceInfo.optString(KwikPassKeys.GK_APP_VERSION, "")
        ))
    }

    // Event tracking functions
    suspend fun trackProductEvent(context: Context, args: TrackProductEventArgs) {
        withContext(Dispatchers.IO) {
            try {
                val isTrackingEnabled = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"
                if (!isTrackingEnabled) return@withContext

                val tracker = SnowplowClient.getSnowplowClient(context)
                if (tracker == null) return@withContext

//                val pageView = PageView()
//                    .pageUrl(args.pageUrl)
//                    .pageTitle(args.name ?: "")
//                    .productId(args.product_id)
//                    .variantId(args.variant_id)
//
//                val contexts = listOfNotNull(
//                    getProductContext(args),
//                    getUserContext(context),
//                    getDeviceInfoContext(context)
//                )
//
//                tracker.track(pageView, contexts)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun trackCartEvent(context: Context, args: TrackCartEventArgs) {
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

//                val pageView = PageView()
//                    .pageUrl(pageUrl)
//                    .pageTitle("Cart")
//
//                val contexts = listOfNotNull(
//                    getCartContext(args.cart_id),
//                    getUserContext(context),
//                    getDeviceInfoContext(context)
//                )
//
//                tracker.track(pageView, contexts)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun trackCollectionsEvent(context: Context, args: TrackCollectionsEventArgs) {
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

//                val pageView = PageView()
//                    .pageUrl(pageUrl)
//                    .pageTitle(args.name)
//
//                val contexts = listOfNotNull(
//                    getUserContext(context),
//                    getDeviceInfoContext(context)
//                )
//
//                tracker.track(pageView, contexts)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun trackOtherEvent(context: Context, args: TrackOtherEventArgs? = null) {
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

//                val pageView = PageView()
//                    .pageUrl(pageUrl)
//                    .pageTitle("Other")
//
//                val contexts = listOfNotNull(
//                    args?.cart_id?.let { getCartContext(it) },
//                    getUserContext(context),
//                    getDeviceInfoContext(context)
//                )
//
//                tracker.track(pageView, contexts)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun sendCustomEventToSnowPlow(context: Context, eventObject: Map<String, Any>) {
        withContext(Dispatchers.IO) {
            try {
                val isTrackingEnabled = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"
                if (!isTrackingEnabled) return@withContext

                val tracker = SnowplowClient.getSnowplowClient(context)
                if (tracker == null) return@withContext

                val environment = getEnvironment(context)
                val schema = "iglu:${KwikPassConfig.getConfig(environment).schemaVendor}/structured/jsonschema/1-0-0"

                val filteredEvents = filterEventValuesAsPerStructSchema(eventObject)
                val selfDescribingJson = SelfDescribingJson(schema, JSONObject(filteredEvents.toString()))

                val contexts = listOfNotNull(
                    getUserContext(context),
                    getDeviceInfoContext(context)
                )

                val event = SelfDescribing(selfDescribingJson)
//                tracker.track(event, contexts)
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

//                val structured = Structured()
//                    .category(args.category)
//                    .action(args.action)
//                    .apply {
//                        args.label?.let { label(it) }
//                        args.property?.let { property(it) }
//                        args.value?.let { value(it) }
//                        args.property_1?.let { property(it) }
//                        args.value_1?.let { value(it) }
//                        args.property_2?.let { property(it) }
//                        args.value_2?.let { value(it) }
//                        args.property_3?.let { property(it) }
//                        args.value_3?.let { value(it) }
//                        args.property_4?.let { property(it) }
//                        args.value_4?.let { value(it) }
//                        args.property_5?.let { property(it) }
//                        args.value_5?.let { value(it) }
//                    }

//                tracker.track(structured)
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
