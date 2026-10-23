package com.echo

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import com.echo.service.EchoFloatingBubbleService
import com.echo.service.canDrawOverlays
import com.echo.ui.EchoScreen

class MainActivity : ComponentActivity() {
    private var waitingForOverlayPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EchoScreen(
                onStartService = ::startEchoService,
                onEnableOverlay = ::enableOverlay
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingForOverlayPermission && canDrawOverlays(this)) {
            waitingForOverlayPermission = false
            startEchoService()
        }
    }

    private fun startEchoService() {
        val intent = Intent(this, EchoFloatingBubbleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ContextCompat.startForegroundService(this, intent)
        else startService(intent)
    }

    private fun enableOverlay() {
        if (canDrawOverlays(this)) {
            startEchoService()
        } else {
            waitingForOverlayPermission = true
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }
}