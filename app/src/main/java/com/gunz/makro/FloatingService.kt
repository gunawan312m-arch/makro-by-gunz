package com.gunz.makro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager

    private lateinit var bubbleView: View
    private lateinit var bubbleParams: WindowManager.LayoutParams

    private var confirmView: View? = null
    private var confirmParams: WindowManager.LayoutParams? = null

    private var stopView: View? = null
    private var stopParams: WindowManager.LayoutParams? = null

    private val handler = Handler(Looper.getMainLooper())

    private var autoTapIntervalMs = 150L
    private var isAutoTapping = false
    private var isEditMode = false
    private var isDragging = false

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private val touchSlopPx = 18

    private lateinit var gestureDetector: GestureDetector

    private val autoTapRunnable = object : Runnable {
        override fun run() {
            if (!isAutoTapping) return
            val x = bubbleParams.x + (bubbleView.width / 2f)
            val y = bubbleParams.y + (bubbleView.height / 2f)
            MacroAccessibilityService.instance?.tap(x, y)
            handler.postDelayed(this, autoTapIntervalMs)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        autoTapIntervalMs = Prefs.getTapIntervalMs(this).toLong()
        startForegroundServiceNotification()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Ambil kecepatan terbaru setiap kali service di-restart (misal setelah ganti setting)
        autoTapIntervalMs = Prefs.getTapIntervalMs(this).toLong()
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "makro_by_gunz_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Makro by Gunz - Mode Mengambang",
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Makro by Gunz aktif")
            .setContentText("Mode mengambang sedang berjalan")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Matikan", stopPendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1, notification)
        }
    }

    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun setupBubble() {
        bubbleView = View.inflate(this, R.layout.floating_bubble, null)

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        bubbleParams.gravity = Gravity.TOP or Gravity.START
        bubbleParams.x = Prefs.getBubbleX(this, 100)
        bubbleParams.y = Prefs.getBubbleY(this, 300)

        windowManager.addView(bubbleView, bubbleParams)

        val icon: TextView = bubbleView.findViewById(R.id.bubbleIcon)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!isEditMode) toggleAutoTap()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (!isDragging) enterEditMode()
            }
        })

        icon.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (!isDragging && (abs(dx) > touchSlopPx || abs(dy) > touchSlopPx)) {
                        isDragging = true
                    }

                    if (isDragging) {
                        bubbleParams.x = initialX + dx.toInt()
                        bubbleParams.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(bubbleView, bubbleParams)
                        if (isEditMode) repositionEditButtons()
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        Prefs.saveBubblePosition(this@FloatingService, bubbleParams.x, bubbleParams.y)
                    }
                    isDragging = false
                    true
                }

                else -> false
            }
        }

        updateBubbleColor()
    }

    private fun toggleAutoTap() {
        isAutoTapping = !isAutoTapping
        if (isAutoTapping) {
            if (MacroAccessibilityService.instance == null) {
                isAutoTapping = false
                return
            }
            autoTapIntervalMs = Prefs.getTapIntervalMs(this).toLong()
            handler.post(autoTapRunnable)
        } else {
            handler.removeCallbacks(autoTapRunnable)
        }
        updateBubbleColor()
    }

    private fun updateBubbleColor() {
        val icon: TextView = bubbleView.findViewById(R.id.bubbleIcon)
        icon.text = if (isAutoTapping) "●" else "M"
    }

    private fun enterEditMode() {
        if (isEditMode) return
        isEditMode = true
        if (isAutoTapping) {
            handler.removeCallbacks(autoTapRunnable)
        }
        showEditButtons()
    }

    private fun showEditButtons() {
        val overlay = overlayType()

        confirmView = View.inflate(this, R.layout.floating_confirm, null)
        confirmParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlay,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        windowManager.addView(confirmView, confirmParams)
        confirmView!!.findViewById<View>(R.id.confirmIcon).setOnClickListener { confirmPosition() }

        stopView = View.inflate(this, R.layout.floating_stop, null)
        stopParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlay,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        windowManager.addView(stopView, stopParams)
        stopView!!.findViewById<View>(R.id.stopIcon).setOnClickListener { stopSelf() }

        repositionEditButtons()
    }

    private fun repositionEditButtons() {
        val bubbleWidth = bubbleView.width.takeIf { it > 0 } ?: 60
        confirmParams?.let {
            it.x = bubbleParams.x
            it.y = bubbleParams.y + bubbleWidth + 16
            confirmView?.let { v -> windowManager.updateViewLayout(v, it) }
        }
        stopParams?.let {
            it.x = bubbleParams.x + bubbleWidth + 16
            it.y = bubbleParams.y + bubbleWidth + 16
            stopView?.let { v -> windowManager.updateViewLayout(v, it) }
        }
    }

    private fun confirmPosition() {
        Prefs.saveBubblePosition(this, bubbleParams.x, bubbleParams.y)
        isEditMode = false
        hideEditButtons()
    }

    private fun hideEditButtons() {
        confirmView?.let { windowManager.removeView(it) }
        confirmView = null
        confirmParams = null

        stopView?.let { windowManager.removeView(it) }
        stopView = null
        stopParams = null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)

        if (::bubbleView.isInitialized) {
            windowManager.removeView(bubbleView)
        }
        confirmView?.let { windowManager.removeView(it) }
        stopView?.let { windowManager.removeView(it) }
    }

    companion object {
        const val ACTION_STOP = "com.gunz.makro.action.STOP"
    }
}
