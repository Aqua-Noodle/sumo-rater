package com.aquanoodle.sumorater.ui.bubble

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aquanoodle.sumorater.MainActivity
import com.aquanoodle.sumorater.R
import com.aquanoodle.sumorater.sumoController
import kotlin.math.roundToInt

/**
 * Floating rating bubble drawn over other apps (including full-screen video).
 * Runs as a foreground service so Android doesn't kill it while another app is in front.
 */
class BubbleService : Service() {

    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val owner = OverlayLifecycleOwner()
    private var view: ComposeView? = null
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // Not focusable: touches outside the bubble/panel keep going to the video app.
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private var expanded by mutableStateOf(false)
    private var bubbleX = 0
    private var bubbleY = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        val metrics = resources.displayMetrics
        bubbleX = metrics.widthPixels - dp(BUBBLE_DP + 16)
        bubbleY = metrics.heightPixels / 3
        params.x = bubbleX
        params.y = bubbleY

        owner.start()
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                BubbleOverlay(
                    controller = sumoController,
                    expanded = expanded,
                    onExpand = ::expand,
                    onCollapse = ::collapse,
                    onClose = { stopSelf() },
                    onOpenApp = ::openApp,
                    onDragBy = ::moveBy,
                )
            }
        }
        windowManager.addView(composeView, params)
        view = composeView
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // e.g. rotating into landscape for full-screen video: keep the bubble on screen.
        clampAndApply()
    }

    override fun onDestroy() {
        view?.let { windowManager.removeView(it) }
        view = null
        owner.destroy()
        super.onDestroy()
    }

    private fun expand() {
        bubbleX = params.x
        bubbleY = params.y
        expanded = true
        clampAndApply()
    }

    private fun collapse() {
        expanded = false
        params.x = bubbleX
        params.y = bubbleY
        clampAndApply()
    }

    private fun moveBy(dx: Float, dy: Float) {
        params.x += dx.roundToInt()
        params.y += dy.roundToInt()
        clampAndApply()
        if (!expanded) {
            bubbleX = params.x
            bubbleY = params.y
        }
    }

    private fun clampAndApply() {
        val v = view ?: return
        val metrics = resources.displayMetrics
        val w = if (expanded) dp(PANEL_WIDTH_DP) else dp(BUBBLE_DP)
        val h = if (expanded) dp(PANEL_HEIGHT_ESTIMATE_DP) else dp(BUBBLE_DP)
        params.x = params.x.coerceIn(0, (metrics.widthPixels - w).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (metrics.heightPixels - h).coerceAtLeast(0))
        windowManager.updateViewLayout(v, params)
    }

    private fun openApp() {
        collapse()
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Rating bubble", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Shown while the floating rating bubble is on screen"
            }
        )
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, BubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bubble)
            .setContentTitle("Rating bubble is on")
            .setContentText("Tap to open Sumo Rater")
            .setOngoing(true)
            .setContentIntent(openApp)
            .addAction(0, "Close bubble", stop)
            .build()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val CHANNEL_ID = "bubble"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.aquanoodle.sumorater.STOP_BUBBLE"
        const val BUBBLE_DP = 56
        const val PANEL_WIDTH_DP = 340
        private const val PANEL_HEIGHT_ESTIMATE_DP = 260

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, BubbleService::class.java))
        }
    }
}
