package com.gk.kwikpass.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import com.gk.kwikpass.initializer.ApplicationCtx
import java.util.*

object AppUtils {
    fun getHostAppVersion(): String {
        val appCtx = ApplicationCtx.get()

        return try {
            val packageInfo = appCtx.packageManager.getPackageInfo(appCtx.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "Unknown"
        }
    }

    fun getDeviceInfo(): Map<String, String> {
        val context = ApplicationCtx.get()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager

        return mapOf(
            "device_model" to Build.MODEL,
            "app_domain" to context.packageName,
            "operating_system" to "Android ${Build.VERSION.RELEASE}",
            "device_id" to Build.ID,
            "device_unique_id" to Build.ID,
            "google_analytics_id" to "", // Implement Firebase Analytics ID if needed
            "google_ad_id" to "",
            "app_version" to getHostAppVersion(),
            "app_version_code" to context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toString(),
            "screen_resolution" to "${metrics.widthPixels}x${metrics.heightPixels}",
            "carrier_info" to (telephonyManager.simOperatorName ?: ""),
            "battery_status" to "${batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)}",
            "language" to Locale.getDefault().language,
            "timezone" to TimeZone.getDefault().id
        )
    }

    fun getHostName(url: String): String {
        return url.replace(Regex("^(?:https?://)?(?:www\\.)?([^/]+).*$"), "$1")
    }
} 