package com.gk.kwikpass.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import com.gk.kwikpass.IdfaAid.IdfaAidModule
import com.gk.kwikpass.config.KwikPassKeys
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

    @SuppressLint("HardwareIds")
    suspend fun getDeviceInfo(): Map<String, String> {
            val context = ApplicationCtx.get()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)

            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val batteryManager =
                context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

            val idfaAidModule: IdfaAidModule = IdfaAidModule.getInstance(context)
            val adId = when (val adInfo = idfaAidModule.getAdvertisingInfo()) {
                is IdfaAidModule.AdvertisingInfoResult.Success -> adInfo.id
                is IdfaAidModule.AdvertisingInfoResult.Error -> ""
            }

            return mapOf(
                KwikPassKeys.GK_DEVICE_MODEL to Build.MODEL,
                KwikPassKeys.GK_APP_DOMAIN to context.packageName,
                KwikPassKeys.GK_OPERATING_SYSTEM to "Android ${Build.VERSION.RELEASE}",
                KwikPassKeys.GK_DEVICE_ID to Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                ),
                KwikPassKeys.GK_DEVICE_UNIQUE_ID to Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                ),
                KwikPassKeys.GK_APP_VERSION to getHostAppVersion(),
                KwikPassKeys.GK_APP_VERSION_CODE to context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                ).longVersionCode.toString(),
                KwikPassKeys.GK_SCREEN_RESOLUTION to "${metrics.widthPixels}x${metrics.heightPixels}",
                KwikPassKeys.GK_CARRIER_INFO to (telephonyManager.networkOperatorName ?: ""),
                KwikPassKeys.GK_BATTERY_STATUS to batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    .toString(),
                KwikPassKeys.GK_LANGUAGE to Locale.getDefault().toString(),
                KwikPassKeys.GK_TIME_ZONE to TimeZone.getDefault().id.toString(),
                KwikPassKeys.GK_GOOGLE_AD_ID to adId.toString()
            )
    }

    fun getHostName(url: String): String {
        return url.replace(Regex("^(?:https?://)?(?:www\\.)?([^/]+).*$"), "$1")
    }
} 