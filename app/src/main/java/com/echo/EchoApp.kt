package com.echo

import android.app.Application
import com.echo.core.EchoSessionManager

class EchoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        EchoSessionManager.get(this)
    }
}