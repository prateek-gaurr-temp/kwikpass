package com.gk.kwikpass.config

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KwikPassCache private constructor(context: Context) {
    private val cache: MutableMap<String, String> = mutableMapOf()
    private val prefs: SharedPreferences

    init {
        prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Retrieves a value from the cache or SharedPreferences.
     */
    suspend fun getValue(key: String): String? = withContext(Dispatchers.IO) {
        // Check if value is in cache
        cache[key]?.let { return@withContext it }

        // If not in cache, fetch from SharedPreferences
        return@withContext try {
        prefs.getString(key, null)?.also {
                cache[key] = it // Store in cache
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Sets a value in both cache and SharedPreferences.
     */
    suspend fun setValue(key: String, value: String) = withContext(Dispatchers.IO) {
        try {
            // Update cache
            cache[key] = value

            // Update SharedPreferences
            prefs.edit().apply {
                putString(key, value)
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clears the in-memory cache.
     */
    fun clearCache() {
        println("CHACHE CLEAR FUNCTION CALLED")
        cache.clear()
    }

    /**
     * Removes a value from both cache and SharedPreferences.
     */
    suspend fun removeValue(key: String) = withContext(Dispatchers.IO) {
        try {
            // Remove from cache
            cache.remove(key)

            // Remove from SharedPreferences
            prefs.edit().apply {
                remove(key)
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val PREF_FILE_NAME = "gk_kwikpass_prefs"

        @Volatile
        private var INSTANCE: KwikPassCache? = null

        fun getInstance(context: Context): KwikPassCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KwikPassCache(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
} 