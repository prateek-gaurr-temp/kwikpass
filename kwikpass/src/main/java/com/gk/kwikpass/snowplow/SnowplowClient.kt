package com.gk.kwikpass.snowplow

import android.content.Context
import com.gk.kwikpass.config.KwikPassCache
import com.gk.kwikpass.config.KwikPassConfig
import com.gk.kwikpass.config.KwikPassKeys
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.RequestCallback
import com.snowplowanalytics.snowplow.tracker.DevicePlatform
import com.snowplowanalytics.snowplow.tracker.LogLevel
import com.snowplowanalytics.snowplow.util.TimeMeasure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

class SnowplowClient {
    companion object {
        private var snowplowTracker: TrackerController? = null

        private fun generateUUID(): String {
            return UUID.randomUUID().toString()
        }

         /**
     * Returns the Emitter Request Callback.
     */
    private val requestCallback: RequestCallback
        private get() = object : RequestCallback {
            override fun onSuccess(successCount: Int) {
                println("Emitter Send Success:\n - Events sent: $successCount\n")
//                updateEventsSent(successCount)
            }

            override fun onFailure(successCount: Int, failureCount: Int) {
                println("Emitter Send Failure:\n - Events sent: $successCount" +
                        " - Events failed: $failureCount")
//                updateEventsSent(successCount)
            }
        }


        suspend fun initializeSnowplowClient(
            context: Context,
            environment: String,
            mid: String
        ) {
            withContext(Dispatchers.IO) {
                val collectorUrl = KwikPassConfig.getConfig(environment).snowplowUrl
//                val cache = KwikPassCache.getInstance(context)

                println("collectorUrl $collectorUrl")

//                val shopDomain = cache.getValue(KwikPassKeys.GK_MERCHANT_URL)
                val appId = mid.toString()

                val networkConfig = NetworkConfiguration(
                    collectorUrl,
                    HttpMethod.POST
                )
                val emitterConfiguration = EmitterConfiguration()
                    .requestCallback(requestCallback)
                    .bufferOption(BufferOption.SmallGroup)
                    .threadPoolSize(20)
                    .byteLimitPost(52000)

                val trackerConfig = TrackerConfiguration(appId)
                    .logLevel(LogLevel.VERBOSE)
                    .base64encoding(false)
                    .devicePlatform(DevicePlatform.Mobile)
                    .sessionContext(true)
                    .platformContext(true)
                    .applicationContext(true)
                    .geoLocationContext(false)
                    .lifecycleAutotracking(true)
                    .screenViewAutotracking(false)
                    .screenContext(true)
                    .exceptionAutotracking(true)
                    .installAutotracking(true)
                    .diagnosticAutotracking(false)

                val sessionConfig = SessionConfiguration(
                    TimeMeasure(30, TimeUnit.MINUTES),
                    TimeMeasure(30, TimeUnit.MINUTES)
                )

                snowplowTracker = createTracker(
                    context,
                    "appTracker",
                    networkConfig,
                    emitterConfiguration,
                    trackerConfig,
                    sessionConfig
                )

                // val uuid = generateUUID()
                // snowplowTracker?.ecommerce?.setEcommerceUser(EcommerceUserEntity(uuid))
            }
        }

        suspend fun getSnowplowClient(
            context: Context,
            environment: String? = null,
            mid: String? = null
        ): TrackerController? {
            val CacheInstance = KwikPassCache.getInstance(context)
            val snowplowTrackingEnabled = CacheInstance.getValue(KwikPassKeys.IS_SNOWPLOW_TRACKING_ENABLED)

            if (snowplowTrackingEnabled == "false") {
                return snowplowTracker
            }

            if (snowplowTracker == null && environment != null && mid != null) {
                initializeSnowplowClient(context, environment, mid)
            }

            return snowplowTracker
        }
    }
}
