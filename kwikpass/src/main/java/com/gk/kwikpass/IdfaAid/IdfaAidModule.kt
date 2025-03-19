package com.gk.kwikpass.IdfaAid

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IdfaAidModule(private val context: Context) {
    private val TAG = "IdfaAidModule"

    data class AdvertisingInfo(
        val id: String?,
        val isAdTrackingLimited: Boolean
    )

    suspend fun getAdvertisingInfo(): AdvertisingInfo = withContext(Dispatchers.IO) {
        try {
            val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
            
            if (adInfo != null) {
                if (adInfo.isLimitAdTrackingEnabled) {
                    // User has opted out of ad tracking
                    Log.d(TAG, "User has opted out of ad tracking")
                    AdvertisingInfo(adInfo.id, true)
                } else {
                    // User has allowed ad tracking
                    Log.d(TAG, "User has allowed ad tracking")
                    AdvertisingInfo(adInfo.id, false)
                }
            } else {
                // No advertising info available
                Log.w(TAG, "No advertising info available")
                AdvertisingInfo(null, true)
            }
        } catch (e: GooglePlayServicesNotAvailableException) {
            Log.e(TAG, "Google Play Services not available: ${e.message}")
            AdvertisingInfo(null, true)
        } catch (e: GooglePlayServicesRepairableException) {
            Log.e(TAG, "Google Play Services needs repair: ${e.message}")
            AdvertisingInfo(null, true)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting advertising ID: ${e.message}")
            AdvertisingInfo(null, true)
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
