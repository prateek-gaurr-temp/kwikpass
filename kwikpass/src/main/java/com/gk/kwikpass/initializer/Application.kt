package com.gk.kwikpass.initializer

import android.annotation.SuppressLint
import android.content.Context
import android.app.Application

/**
 * Global application context holder.
 * Only stores the application context to prevent memory leaks.
 * Never use this to store Activity or other types of Context.
 */
@SuppressLint("StaticFieldLeak")
object ApplicationCtx {
    private var instance: Context? = null

    /**
     * Initialize with application context.
     * @throws IllegalArgumentException if context is not an Application context
     */
    fun init(context: Context) {
        if (context !is Application) {
            throw IllegalArgumentException("Context must be an Application context")
        }
        instance = context.applicationContext
    }

    /**
     * @return The application context
     * @throws IllegalStateException if context is not initialized
     */
    fun get(): Context {
        return instance ?: throw IllegalStateException("Application context not initialized. Call init() first.")
    }
}