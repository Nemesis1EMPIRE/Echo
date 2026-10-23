package com.echo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.echo.MainActivity
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat
import com.echo.core.EchoSessionManager
import com.echo.core.EchoState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun canDrawOverlays(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true

class EchoFloatingBubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubble: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel()
        startInForeground()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        observeSessionState()
        if (canDrawOverlays(this)) addBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        if (bubble == null && canDrawOverlays(this)) addBubble()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        bubble?.let { runCatching { windowManager.removeViewImmediate(it) } }
        bubble = null
        super.onDestroy()
    }

    private fun startInForeground() {
        val notification = buildNotification()
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopService = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            Intent(this, EchoFloatingBubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("ECHO est active")
            .setContentText("La bulle vocale reste disponible")
            .setContentIntent(openApp)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Arrêter", stopService)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Service ECHO", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @Suppress("DEPRECATION")
    private fun addBubble() {
        if (bubble != null) return
        val size = (56 * resources.displayMetrics.density).toInt()
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        params = WindowManager.LayoutParams(
            size,
            size,
            overlayType,
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

    private fun observeSessionState() {
        serviceScope.launch {
            EchoSessionManager.get(this@EchoFloatingBubbleService).state.collect(::updateBubbleState)
        }
    }

    private fun updateBubbleState(state: EchoState) {
        val color = when (state) {
            EchoState.IDLE -> COLOR_IDLE
            EchoState.LISTENING -> COLOR_LISTENING
            EchoState.THINKING -> COLOR_THINKING
            EchoState.SPEAKING -> COLOR_SPEAKING
        }
        (bubble as? TextView)?.background?.let { background ->
            (background as? GradientDrawable)?.setColor(color)
        }
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
        private const val ACTION_STOP = "com.echo.action.STOP_BUBBLE_SERVICE"
        private const val STOP_REQUEST_CODE = 342
        private const val COLOR_IDLE = 0xFF00BEDC.toInt()
        private const val COLOR_LISTENING = 0xFFFF8A70.toInt()
        private const val COLOR_THINKING = 0xFFFFD166.toInt()
        private const val COLOR_SPEAKING = 0xFF41E2D0.toInt()
    }
}