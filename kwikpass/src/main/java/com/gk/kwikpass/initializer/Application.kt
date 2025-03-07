package com.gk.kwikpass.initializer


import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

@SuppressLint("StaticFieldLeak")
public object ApplicationCtx {
    lateinit var instance: Context

    fun get(): Context {
        return instance;
    }
}