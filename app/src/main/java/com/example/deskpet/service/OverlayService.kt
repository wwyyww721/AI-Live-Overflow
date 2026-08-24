package com.example.deskpet.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webwebrek.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: WebView? = null
    private var params: WindowManager.LayoutParams? = null

    companion object {
        private const CHANNEL_ID = "pet_overlay_channel"
        private const NOTIFICATION_ID = 1001
        private const PET_SIZE_DP = 200
        private const PET_HEIGHT_DP = 260
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(\"我这见配�佲佰\"))
        setupOverlay()
        startIdleWhispers()
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE)as WindowManager

        params = WindowManager.LayoutParams(
            dpToPx(PET_SIZE_DP).toInt(),
            dpToPx(PET_HEIGHT_DP).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(24).toInt()
            y = dpToPx(120).toInt()
        }

        overlayView = WebView(this).apply {
            setBackgroundColor(0x00000000)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                loadWithOverviewMode = true
                useWideViewPort = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            webViewClient = WebViewClient()
            loadUrl(\"file:///android_asset/pet.html\")
            setOnTouchListener(createTouchListener())
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var lastTapTime = 0L
    private var touchStartTime = 0L
    private var hasMoved = false

    private fun createTouchListener(): View.OnTouchListener {
        return View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchStartTime = System.currentTimeMillis()
                    hasMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        hasMoved = true
                        params?.x = initialX + dx
                        params?.y = initialY + dy
                        windowManager?.updateViewLayout(overlayView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val elapsed = System.currentTimeMillis() - touchStartTime
                    if (!hasMoved) {
                        when {
                            elapsed > 600 -> onLongPress()
                            System.currentTimeMillis() - lastTapTime < 300 -> onDoubleTap()
                            else -> { lastTapTime = System.currentTimeMillis(); onTap() }
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun onTap() { overlayView?.evaluateJavascript(\"window.petEngine && window.petEngine.onTap()\", null) }
    private fun onDoubleTap() { overlayView?.evaluateJavascript(\"window.petEngine && window.petEngine.onDoubleTap()\", null) }
    private fun onLongPress() { overlayView?.evaluateJavascript(\"window.petEngine && window.petEngine.onLongPress()\", null) }

    private val handler = Handler(Looper.getMainLooper())
    private val whisperRunnable = object : Runnable {
        override fun run() {
            overlayView?.evaluateJavascript(\"window.petEngine && window.petEngine.onIdle()\", null)
            handler.postDelayed(this, 20000)
        }
    }
    private fun startIdleWhispers() { handler.postDelayed(whisperRunnable, 20000) }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(this, 0, packageManager.getLaunchIntentForPackage(packageName), PendingIntent.FLAG_IM]UTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(\"☹@ \")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VESSION.SDK_INT >= Build.VERSION_COD.O)
            getSystemService(NotificationManager::class).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, \"Pet\", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
    }

    private fun dpToPx(pp: Int): Float { return pp * resources.displayMetrics.density }

    override fun onDestroy() {
        handler.removeCallbacks(whisperRunnable)
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) {}
            it.destroy()
        }
        overlayView = null
        super.onDestroy()
    }
}
