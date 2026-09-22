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
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager

    private lateinit var bubbleView: View
    private lateinit var bubbleParams: WindowManager.LayoutParams

    private var confirmView: View? = null
    private var confirmParams: WindowManager.LayoutParams? = null

    private val handler = Handler(Looper.getMainLooper())

    // Interval antar ketukan otomatis saat mode aktif (ms). Bisa disesuaikan.
    private val autoTapIntervalMs = 150L
    private var isAutoTapping = false

    private var isEditMode = false

    // Untuk mendeteksi tap vs tahan-lama vs geser
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var downTime = 0L
    private val longPressThresholdMs = 500L
    private val touchSlopPx = 20
    private var longPressTriggered = false

    private var longPressRunnable: Runnable? = null

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
        startForegroundServiceNotification()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupBubble()
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
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Makro by Gunz aktif")
            .setContentText("Mode mengambang sedang berjalan")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
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

    private fun setupBubble() {
        bubbleView = View.inflate(this, R.layout.floating_bubble, null)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        bubbleParams.gravity = Gravity.TOP or Gravity.START
        bubbleParams.x = Prefs.getBubbleX(this, 100)
        bubbleParams.y = Prefs.getBubbleY(this, 300)

        windowManager.addView(bubbleView, bubbleParams)

        val icon: TextView = bubbleView.findViewById(R.id.bubbleIcon)

        icon.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (isEditMode) return@setOnTouchListener true

                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    downTime = System.currentTimeMillis()
                    longPressTriggered = false

                    longPressRunnable = Runnable {
                        longPressTriggered = true
                        enterEditMode()
                    }
                    handler.postDelayed(longPressRunnable!!, longPressThresholdMs)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isEditMode) {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        bubbleParams.x = initialX + dx
                        bubbleParams.y = initialY + dy
                        windowManager.updateViewLayout(bubbleView, bubbleParams)
                    } else {
                        val movedX = Math.abs(event.rawX - initialTouchX)
                        val movedY = Math.abs(event.rawY - initialTouchY)
                        if (movedX > touchSlopPx || movedY > touchSlopPx) {
                            longPressRunnable?.let { handler.removeCallbacks(it) }
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }

                    if (!isEditMode && !longPressTriggered) {
                        val elapsed = System.currentTimeMillis() - downTime
                        val movedX = Math.abs(event.rawX - initialTouchX)
                        val movedY = Math.abs(event.rawY - initialTouchY)
                        if (elapsed < longPressThresholdMs && movedX < touchSlopPx && movedY < touchSlopPx) {
                            toggleAutoTap()
                        }
                    }
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
        isEditMode = true
        // Hentikan auto-tap sementara saat mengedit posisi
        if (isAutoTapping) {
            handler.removeCallbacks(autoTapRunnable)
        }
        showConfirmButton()
    }

    private fun showConfirmButton() {
        if (confirmView != null) return

        confirmView = View.inflate(this, R.layout.floating_confirm, null)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        confirmParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        confirmParams!!.gravity = Gravity.TOP or Gravity.START
        confirmParams!!.x = bubbleParams.x
        confirmParams!!.y = bubbleParams.y + bubbleView.height + 24

        windowManager.addView(confirmView, confirmParams)

        confirmView!!.findViewById<View>(R.id.confirmIcon).setOnClickListener {
            confirmPosition()
        }
    }

    private fun confirmPosition() {
        Prefs.saveBubblePosition(this, bubbleParams.x, bubbleParams.y)
        isEditMode = false

        confirmView?.let { windowManager.removeView(it) }
        confirmView = null
        confirmParams = null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(autoTapRunnable)
        longPressRunnable?.let { handler.removeCallbacks(it) }

        if (::bubbleView.isInitialized) {
            windowManager.removeView(bubbleView)
        }
        confirmView?.let { windowManager.removeView(it) }
    }
}
