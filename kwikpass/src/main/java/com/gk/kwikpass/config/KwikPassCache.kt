package com.gk.kwikpass.config

import android.content.Context
import android.content.SharedPreferences
import java.util.LinkedHashMap

class KwikPassCache private constructor(context: Context) {
    private val cache: LinkedHashMap<String, String>
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
    private val maxCacheSize: Int = 100 // Maximum number of items in cache

    init {
        // Initialize LRU cache with access order
        cache = object : LinkedHashMap<String, String>(maxCacheSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
                return size > maxCacheSize
            }
        }
        // Load initial data from SharedPreferences
        loadInitialData()
    }

    private fun loadInitialData() {
        try {
            val allPrefs = prefs.all
            allPrefs.forEach { (key, value) ->
                if (value is String) {
                    cache[key] = value
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Retrieves a value from the cache or SharedPreferences.
     */
    fun getValue(key: String): String? {
        // Check if value is in cache
        cache[key]?.let { return it }

        // If not in cache, fetch from SharedPreferences
        return try {
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
    fun setValue(key: String, value: String) {
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
        println("CACHE CLEAR FUNCTION CALLED")
        cache.clear()
    }

    /**
     * Removes a value from both cache and SharedPreferences.
     */
    fun removeValue(key: String) {
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