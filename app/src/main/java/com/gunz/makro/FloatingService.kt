package com.gunz.makro

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private var sidebarView: View? = null
    private var mainMenuView: View? = null
    private var crosshairView: View? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var autoHideRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        setupSidebarIndicator()
        setupMainFloatingMenu()
        setupCrosshair()
    }

    private fun setupSidebarIndicator() {
        val indicator = View(this).apply {
            setBackgroundColor(Color.parseColor("#80FF0000"))
        }

        val params = createLayoutParams(
            width = 15,
            height = 300,
            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
        )

        indicator.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0f
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (event.rawX - startX > 50) {
                            showMainMenu()
                        }
                    }
                }
                return false
            }
        })

        sidebarView = indicator
        windowManager.addView(sidebarView, params)
    }

    private fun setupMainFloatingMenu() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(20, 20, 20, 20)
            visibility = View.GONE
        }

        val btnAutoClick = Button(this).apply {
            text = "Tahan u/ Auto Click"
            setOnLongClickListener {
                true
            }
        }

        val btnCrosshair = Button(this).apply {
            text = "Toggle Crosshair"
            setOnClickListener {
                crosshairView?.let {
                    it.visibility = if (it.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }
            }
        }

        layout.addView(btnAutoClick)
        layout.addView(btnCrosshair)

        val params = createLayoutParams(
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
        )

        mainMenuView = layout
        windowManager.addView(mainMenuView, params)
    }

    private fun setupCrosshair() {
        val crosshair = FrameLayout(this).apply {
            val dot = View(context).apply {
                setBackgroundColor(Color.GREEN)
            }
            val size = 20
            val lp = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
            }
            addView(dot, lp)
            visibility = View.GONE
        }

        val params = createLayoutParams(
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            gravity = Gravity.CENTER
        )

        crosshairView = crosshair
        windowManager.addView(crosshairView, params)
    }

    private fun showMainMenu() {
        mainMenuView?.visibility = View.VISIBLE
        
        autoHideRunnable?.let { handler.removeCallbacks(it) }
        autoHideRunnable = Runnable {
            mainMenuView?.visibility = View.GONE
        }
        handler.postDelayed(autoHideRunnable!!, 3000)
    }

    private fun createLayoutParams(width: Int, height: Int, gravity: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            width, height, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sidebarView?.let { windowManager.removeView(it) }
        mainMenuView?.let { windowManager.removeView(it) }
        crosshairView?.let { windowManager.removeView(it) }
    }
}
