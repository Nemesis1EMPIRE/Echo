package com.echo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.echo.MainActivity
import com.echo.R
import com.echo.core.EchoSessionManager

class EchoFloatingBubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubble: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startInForeground()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (android.provider.Settings.canDrawOverlays(this)) addBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (bubble == null && android.provider.Settings.canDrawOverlays(this)) addBubble()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        bubble?.let { runCatching { windowManager.removeViewImmediate(it) } }
        bubble = null
        super.onDestroy()
    }

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("ECHO est active")
                .setContentText("La bulle vocale reste disponible")
                .setContentIntent(openApp)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("ECHO est active")
                .setContentText("La bulle vocale reste disponible")
                .setContentIntent(openApp)
                .setOngoing(true)
                .build()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Service ECHO", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun addBubble() {
        if (bubble != null) return
        val size = (56 * resources.displayMetrics.density).toInt()
        params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 180
        }
        val view = TextView(this).apply {
            text = "E"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            elevation = 12f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(0, 190, 220))
                setStroke((2 * resources.displayMetrics.density).toInt(), Color.WHITE)
            }
        }
        installTouchBehavior(view)
        bubble = view
        runCatching { windowManager.addView(view, params) }.onFailure { bubble = null }
    }

    private fun installTouchBehavior(view: View) {
        val touchSlop = (8 * resources.displayMetrics.density).toInt()
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragged = false
        var longPressed = false
        val longPress = Runnable {
            longPressed = true
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragged = false
                    longPressed = false
                    // Defer the tap action so a long press can open the main screen instead.
                    handler.postDelayed(longPress, android.view.ViewConfiguration.getLongPressTimeout().toLong())
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) {
                        dragged = true
                        handler.removeCallbacks(longPress)
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        runCatching { windowManager.updateViewLayout(view, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPress)
                    if (event.action == MotionEvent.ACTION_UP && !dragged && !longPressed) {
                        EchoSessionManager.get(this).listen()
                    }
                    true
                }
                else -> false
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "echo_service"
        private const val NOTIFICATION_ID = 341
    }
}