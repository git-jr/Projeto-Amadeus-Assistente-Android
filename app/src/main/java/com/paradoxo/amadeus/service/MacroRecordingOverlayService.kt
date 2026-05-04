package com.paradoxo.amadeus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.activity.EditarMacroActivity
import com.paradoxo.amadeus.activity.MainActivity
import com.paradoxo.amadeus.cognicao.AgentMemoryManager
import com.paradoxo.amadeus.util.Toasts.meuToast

class MacroRecordingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var recordingOverlayView: View? = null
    private var tvActionCount: TextView? = null

    private val memoryManager by lazy { AgentMemoryManager(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotification()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecordingMode()
        }
        return START_NOT_STICKY
    }

    private fun createNotification() {
        val channelId = "macro_recording_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gravação de Macro",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Gravando ações...")
            .setSmallIcon(R.drawable.ic_logo_white)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // ── Recording mode ────────────────────────────────────────────────────────

    private fun startRecordingMode() {
        if (recordingOverlayView != null) return
        val started = AmadeusAccessibilityService.startRecording()
        if (!started) {
            meuToast("Serviço de acessibilidade não está ativo", this)
            stopSelf()
            return
        }
        AmadeusAccessibilityService.setOnActionRecordedCallback { count ->
            tvActionCount?.text = if (count == 1) "1 ação gravada" else "$count ações gravadas"
        }
        showRecordingOverlay()
    }

    private fun stopRecordingMode() {
        val actions = AmadeusAccessibilityService.stopRecording()
        AmadeusAccessibilityService.setOnActionRecordedCallback(null)
        hideRecordingOverlay()
        if (actions.isEmpty()) {
            meuToast("Nenhuma ação foi gravada", this)
            stopSelf()
            return
        }
        val json = memoryManager.serializeActions(actions)
        val intent = Intent(this, EditarMacroActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EditarMacroActivity.EXTRA_RECORDED_ACTIONS_JSON, json)
        }
        startActivity(intent)
        stopSelf()
    }

    private fun showRecordingOverlay() {
        val dp = resources.displayMetrics.density
        val themedContext = android.view.ContextThemeWrapper(this, R.style.AppTheme)
        val view = LayoutInflater.from(themedContext).inflate(R.layout.layout_recording_overlay, null)
        tvActionCount = view.findViewById(R.id.tvActionCount)
        view.findViewById<View>(R.id.btnStopRecording).setOnClickListener { stopRecordingMode() }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (16 * dp).toInt()
            y = (80 * dp).toInt()
        }

        // Drag via handle area only — button keeps its own click
        var dX = 0; var dY = 0
        view.findViewById<View>(R.id.dragHandle).setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = params.x - event.rawX.toInt()
                    dY = params.y - event.rawY.toInt()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = event.rawX.toInt() + dX
                    params.y = event.rawY.toInt() + dY
                    try { windowManager.updateViewLayout(view, params) } catch (_: Exception) {}
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(view, params)
            recordingOverlayView = view
        } catch (_: Exception) {}

        val dot = view.findViewById<View>(R.id.viewRecDot)
        val blink = android.view.animation.AlphaAnimation(1f, 0.15f).apply {
            duration = 600
            repeatMode = android.view.animation.Animation.REVERSE
            repeatCount = android.view.animation.Animation.INFINITE
        }
        dot?.startAnimation(blink)
    }

    private fun hideRecordingOverlay() {
        recordingOverlayView?.let {
            it.findViewById<View>(R.id.viewRecDot)?.clearAnimation()
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        recordingOverlayView = null
        tvActionCount = null
    }

    @Suppress("DEPRECATION")
    private fun overlayType() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        WindowManager.LayoutParams.TYPE_PHONE
    }

    override fun onDestroy() {
        super.onDestroy()
        hideRecordingOverlay()
        AmadeusAccessibilityService.setOnActionRecordedCallback(null)
    }

    companion object {
        private const val NOTIFICATION_ID = 202
        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
    }
}
