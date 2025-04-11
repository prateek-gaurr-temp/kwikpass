package com.gk.kwikpass.IdfaAid

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IdfaLogger private constructor() {
    companion object {
        private const val TAG = "IdfaAid"
        
        @Volatile
        private var instance: IdfaLogger? = null
        
        fun getInstance(): IdfaLogger {
            return instance ?: synchronized(this) {
                instance ?: IdfaLogger().also { instance = it }
            }
        }
    }

    enum class LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    private var isDebugEnabled = true
    private var networkPublisher: ((String, LogLevel) -> Unit)? = null

    fun setDebugEnabled(enabled: Boolean) {
        isDebugEnabled = enabled
    }

    fun setNetworkPublisher(publisher: (String, LogLevel) -> Unit) {
        networkPublisher = publisher
    }

    fun d(message: String) = log(LogLevel.DEBUG, message)
    fun i(message: String) = log(LogLevel.INFO, message)
    fun w(message: String) = log(LogLevel.WARN, message)
    fun e(message: String) = log(LogLevel.ERROR, message)

    private fun log(level: LogLevel, message: String) {
        if (isDebugEnabled) {
            when (level) {
                LogLevel.DEBUG -> Log.d(TAG, message)
                LogLevel.INFO -> Log.i(TAG, message)
                LogLevel.WARN -> Log.w(TAG, message)
                LogLevel.ERROR -> Log.e(TAG, message)
            }
        }

        // Publish to network if configured
        networkPublisher?.let { publisher ->
            CoroutineScope(Dispatchers.IO).launch {
                publisher(message, level)
            }
        }
    }
} 