package com.gk.kwikpass.snowplow

import android.content.Context
import com.gk.kwikpass.config.KwikPassCache
import com.gk.kwikpass.config.KwikPassConfig
import com.gk.kwikpass.config.KwikPassKeys
import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.tracker.DevicePlatform
import com.snowplowanalytics.snowplow.tracker.LogLevel
import com.snowplowanalytics.snowplow.util.TimeMeasure
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SnowplowClient {
    companion object {
        private var snowplowTracker: Tracker? = null

        private fun generateUUID(): String {
            return UUID.randomUUID().toString()
        }

        suspend fun initializeSnowplowClient(
            context: Context,
            environment: String,
            mid: String
        ) {
            withContext(Dispatchers.IO) {
                val collectorUrl = KwikPassConfig.getConfig(environment).snowplowUrl
                val cache = KwikPassCache.getInstance(context)

                println("collectorUrl $collectorUrl")

                val shopDomain = cache.getValue(KwikPassKeys.GK_MERCHANT_URL)
                val appId = if (!shopDomain.isNullOrEmpty() && mid.isNotEmpty()) {
                    "$shopDomain-$mid"
                } else {
                    ""
                }

                val networkConfig = NetworkConfiguration(
                    collectorUrl,
                    HttpMethod.POST
                )
                val trackerConfig = TrackerConfiguration("appId")
                    .base64encoding(false)
                    .sessionContext(true)
                    .platformContext(true)
                    .lifecycleAutotracking(true)
                    .screenViewAutotracking(true)
                    .screenContext(true)
                    .applicationContext(true)
                    .exceptionAutotracking(true)
                    .installAutotracking(true)
                    .userAnonymisation(false)
                    .logLevel(LogLevel.OFF)
                val sessionConfig = SessionConfiguration(
                    TimeMeasure(30, TimeUnit.SECONDS),
                    TimeMeasure(30, TimeUnit.SECONDS)
                )
                    .continueSessionOnRestart(false)

                createTracker(
                    context,
                    "appTracker",
                    networkConfig,
                    trackerConfig,
                    sessionConfig
                )

//                val uuid = generateUUID()
//                snowplowTracker?.subject?.userId = uuid
            }
        }

        suspend fun getSnowplowClient(
            context: Context,
            environment: String? = null,
            mid: String? = null
        ): Tracker? {
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
