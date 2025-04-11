package com.gk.kwikpass.IdfaAid

import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException

class IdfaAidModule(private val context: Context) {
    private val logger = IdfaLogger.getInstance()

    sealed class AdvertisingInfoResult {
        data class Success(
            val id: String,
            val isAdTrackingLimited: Boolean
        ) : AdvertisingInfoResult()

        data class Error(
            val message: String,
            val isAdTrackingLimited: Boolean = true
        ) : AdvertisingInfoResult()
    }

    fun getAdvertisingInfo(): AdvertisingInfoResult {
        try {
            val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
            
            if (adInfo != null) {
                if (adInfo.isLimitAdTrackingEnabled) {
                    // User has opted out of ad tracking
                    logger.i("User has opted out of ad tracking")
                    return AdvertisingInfoResult.Success(adInfo.id ?: "", true)
                } else {
                    // User has allowed ad tracking
                    logger.i("User has allowed ad tracking")
                    return AdvertisingInfoResult.Success(adInfo.id ?: "", false)
                }
            } else {
                // No advertising info available
                logger.w("No advertising info available")
                return AdvertisingInfoResult.Error("No advertising info available")
            }
        } catch (e: GooglePlayServicesNotAvailableException) {
            logger.e("Google Play Services not available: ${e.message}")
            return AdvertisingInfoResult.Error("Google Play Services not available: ${e.message}")
        } catch (e: GooglePlayServicesRepairableException) {
            logger.e("Google Play Services needs repair: ${e.message}")
            return AdvertisingInfoResult.Error("Google Play Services needs repair: ${e.message}")
        } catch (e: Exception) {
            logger.e("Error getting advertising ID: ${e.message}")
            return AdvertisingInfoResult.Error("Error getting advertising ID: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var instance: IdfaAidModule? = null

        fun getInstance(context: Context): IdfaAidModule {
            return instance ?: synchronized(this) {
                instance ?: IdfaAidModule(context.applicationContext).also { instance = it }
            }
        }
    }
}
