package com.gk.kwikpass.snowplow

import android.content.Context
import android.util.Log
import com.gk.kwikpass.IdfaAid.IdfaAidModule
import com.gk.kwikpass.config.KwikPassCache
import com.gk.kwikpass.config.KwikPassConfig
import com.gk.kwikpass.config.KwikPassKeys
import com.gk.kwikpass.initializer.ApplicationCtx
import com.gk.kwikpass.utils.CoroutineUtils
import com.snowplowanalytics.snowplow.event.PageView
import com.snowplowanalytics.snowplow.event.SelfDescribing
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
        val cart_id: String?,
        val product_id: String,
        val pageUrl: String,
        val variant_id: String?,
        val img_url: String? = null,
        val name: String? = null,
        val price: String? = null,
        val handle: String? = null
    )

    data class TrackCartEventArgs(
        val cart_id: String?
    )

    data class TrackCollectionsEventArgs(
        val cart_id: String?,
        val collection_id: String?,
        val name: String,
        val image_url: String? = null,
        val handle: String? = null
    )

    data class TrackOtherEventArgs(
        val cart_id: String? = null,
        val pageUrl: String,
    )

    data class StructuredProps(
        val category: String,
        val action: String,
        val label: String? = null,
        val property: String? = null,
        val value: Double? = null
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
        return SelfDescribingJson(schema, data)
    }

    private suspend fun getCartContext(cartId: String?): SelfDescribingJson? {
        if (cartId.isNullOrBlank()) return null
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

        val user = try {
            if (!userJson.isNullOrEmpty()) {
                JSONObject(userJson)
            } else {
                JSONObject()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            JSONObject()
        }

        val phone = user.optString("phone", "").replace("^+91".toRegex(), "").replace("[^0-9]".toRegex(), "")
        val numericPhoneNumber = phone.toLongOrNull()
        val email = user.optString("email", "")

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
            "img_url" to args.img_url,
            "variant_id" to args.variant_id,
            "product_name" to args.product_name,
            "product_price" to args.product_price,
            "product_handle" to args.product_handle,
            "type" to args.type
        ))
    }

    private suspend fun getDeviceInfoContext(): SelfDescribingJson {
        val context = ApplicationCtx.get()
        val deviceFCM = KwikPassCache.getInstance(context).getValue(KwikPassKeys.GK_NOTIFICATION_TOKEN)
        val deviceInfoJson = KwikPassCache.getInstance(context).getValue(KwikPassKeys.GK_DEVICE_INFO)
        
        val deviceInfo = try {
            if (!deviceInfoJson.isNullOrEmpty()) {
                JSONObject(deviceInfoJson)
            } else {
                JSONObject()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            JSONObject()
        }

        val advertisingInfo = IdfaAidModule.getInstance(context).getAdvertisingInfo()
        val androidAdId = when (advertisingInfo) {
            is IdfaAidModule.AdvertisingInfoResult.Success -> advertisingInfo.id
            is IdfaAidModule.AdvertisingInfoResult.Error -> ""
        }

        return createContext("user_device/jsonschema/1-0-0", mapOf(
            "device_id" to deviceInfo.optString(KwikPassKeys.GK_DEVICE_UNIQUE_ID, ""),
            "android_ad_id" to androidAdId,
            "ios_ad_id" to "",
            "fcm_token" to (deviceFCM ?: "").toString(),
            "app_domain" to deviceInfo.optString(KwikPassKeys.GK_APP_DOMAIN, ""),
            "device_type" to "android",
            "app_version" to deviceInfo.optString(KwikPassKeys.GK_APP_VERSION, "")
        ))
    }

    private suspend fun getCollectionsContext(collectionId: String?, imgUrl: String?, collectionName: String, collectionHandle: String): SelfDescribingJson {
        return createContext("product/jsonschema/1-1-0", mapOf(
            "collection_id" to (collectionId ?: ""),
            "img_url" to (imgUrl ?: ""),
            "collection_name" to collectionName,
            "collection_handle" to collectionHandle,
            "type" to "collection"
        ))
    }

    private suspend fun getOtherEventsContext(): SelfDescribingJson {
        return createContext("product/jsonschema/1-1-0", mapOf(
            "type" to "other"
        ))
    }

    private fun trimCartId(cartId: String): String {
        val cartIdArray = Regex("gid://shopify/Cart/([^?]+)").find(cartId)
        return cartIdArray?.groupValues?.get(1) ?: cartId
    }

    // Event tracking functions
    fun trackProductEvent(args: TrackProductEventArgs) {
        val context = ApplicationCtx.get()

        CoroutineUtils.coroutine.launch(Dispatchers.IO) {
            try {
                val isTrackingEnabled = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"
                if (!isTrackingEnabled) return@launch

                val tracker = SnowplowClient.getSnowplowClient(context) ?: return@launch

                var cartId = args.cart_id
                if (cartId?.contains("gid://shopify/Cart/") == true) {
                    cartId = trimCartId(cartId)
                }

                val pageView = PageView(args.pageUrl)
                    .pageTitle(args.name ?: "")

                val contextDetails = TrackProductEventContext(
                    product_id = args.product_id,
                    img_url = args.img_url ?: "",
                    variant_id = args.variant_id ?: "",
                    product_name = args.name ?: "",
                    product_price = args.price ?: "",
                    product_handle = args.handle ?: "",
                    type = "product"
                )

                val productJson = getProductContext(contextDetails)
                pageView.entities.add(productJson)

                val deviceJson = getDeviceInfoContext()
                pageView.entities.add(deviceJson)

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

                tracker.track(pageView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun trackCartEvent(args: TrackCartEventArgs) {
        val context = ApplicationCtx.get()

        CoroutineUtils.coroutine.launch(Dispatchers.IO) {
            try {
                val isTrackingEnabled = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"
                if (!isTrackingEnabled) return@launch

                val merchantUrl = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.GK_MERCHANT_URL) ?: ""
                val pageUrl = "https://$merchantUrl/cart"

                val tracker = SnowplowClient.getSnowplowClient(context) ?: return@launch

                var cartId = args.cart_id
                if (cartId?.contains("gid://shopify/Cart/") == true) {
                    cartId = trimCartId(cartId)
                }

                val pageView = PageView(pageUrl)

                val deviceJson = getDeviceInfoContext()
                pageView.entities.add(deviceJson)

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

                val otherEventsJson = getOtherEventsContext()
                pageView.entities.add(otherEventsJson)

                tracker.track(pageView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun trackCollectionsEvent(args: TrackCollectionsEventArgs) {
        val context = ApplicationCtx.get()

        CoroutineUtils.coroutine.launch(Dispatchers.IO) {
            try {
                val isTrackingEnabled = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"
                if (!isTrackingEnabled) return@launch

                val merchantUrl = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.GK_MERCHANT_URL) ?: ""
                val pageUrl = if (args.handle != null && merchantUrl.isNotEmpty()) {
                    "https://$merchantUrl/collections/${args.handle}"
                } else ""

                val tracker = SnowplowClient.getSnowplowClient(context) ?: return@launch

                var cartId = args.cart_id
                if (cartId?.contains("gid://shopify/Cart/") == true) {
                    cartId = trimCartId(cartId)
                }

                val pageView = PageView(pageUrl)
                    .pageTitle(args.name)

                val collectionsJson = getCollectionsContext(
                    args.collection_id,
                    args.image_url,
                    args.name,
                    args.handle ?: ""
                )
                pageView.entities.add(collectionsJson)

                val deviceJson = getDeviceInfoContext()
                pageView.entities.add(deviceJson)

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

                tracker.track(pageView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun trackOtherEvent(args: TrackOtherEventArgs) {
        val context = ApplicationCtx.get()

        CoroutineUtils.coroutine.launch(Dispatchers.IO) {
            try {
                val isTrackingEnabled = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"
                if (!isTrackingEnabled) return@launch

                val tracker = SnowplowClient.getSnowplowClient(context) ?: return@launch

                var cartId = args.cart_id
                if (cartId?.contains("gid://shopify/Cart/") == true) {
                    cartId = trimCartId(cartId)
                }

                val pageView = PageView(args.pageUrl)

                val deviceJson = getDeviceInfoContext()
                pageView.entities.add(deviceJson)

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

                val otherEventsJson = getOtherEventsContext()
                pageView.entities.add(otherEventsJson)

                tracker.track(pageView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendCustomEventToSnowPlow(eventObject: Map<String, Any>) {
        val context = ApplicationCtx.get()

        CoroutineUtils.coroutine.launch(Dispatchers.IO) {
            try {
                val isTrackingEnabled = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"
                if (!isTrackingEnabled) return@launch

                val tracker = SnowplowClient.getSnowplowClient(context) ?: return@launch

                val environment = getEnvironment(context)
                val schema = "iglu:${KwikPassConfig.getConfig(environment).schemaVendor}/structured/jsonschema/1-0-0"

                val filteredEvents = filterEventValuesAsPerStructSchema(eventObject)
                val eventData = SelfDescribingJson(schema, filteredEvents)
                val event = SelfDescribing(eventData)

                val deviceJson = getDeviceInfoContext()
                event.entities.add(deviceJson)

                val userJson = getUserContext()
                if (userJson != null) {
                    event.entities.add(userJson)
                }

                tracker.track(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun snowplowStructuredEvent(context: Context, args: StructuredProps) {
        CoroutineUtils.coroutine.launch(Dispatchers.IO) {
            try {
                val isTrackingEnabled = KwikPassCache.getInstance(context)
                    .getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED) == "true"
                if (!isTrackingEnabled) return@launch

                val tracker = SnowplowClient.getSnowplowClient(context) ?: return@launch

                val environment = getEnvironment(context)
                val schema = "iglu:${KwikPassConfig.getConfig(environment).schemaVendor}/structured/jsonschema/1-0-0"

                val eventData = SelfDescribingJson(
                    schema,
                    mapOf(
                        "category" to args.category,
                        "action" to args.action,
                        "label" to (args.label ?: ""),
                        "property" to (args.property ?: ""),
                        "value" to (args.value ?: 0)
                    )
                )

                val event = SelfDescribing(eventData)

                val deviceJson = getDeviceInfoContext()
                event.entities.add(deviceJson)

                val userJson = getUserContext()
                if (userJson != null) {
                    event.entities.add(userJson)
                }

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
