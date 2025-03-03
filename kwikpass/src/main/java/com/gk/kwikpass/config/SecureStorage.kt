package com.gk.kwikpass.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SecureStorage {
    suspend fun storeSecureData(context: Context, key: String, value: String) {
        withContext(Dispatchers.IO) {
            KwikPassCache.getInstance(context).setValue(key, value)
        }
    }

    suspend fun getSecureData(context: Context, key: String): String? {
        return withContext(Dispatchers.IO) {
            KwikPassCache.getInstance(context).getValue(key)
        }
    }

    suspend fun clearSecureData(context: Context, key: String) {
        withContext(Dispatchers.IO) {
            KwikPassCache.getInstance(context).removeValue(key)
        }
    }
} 