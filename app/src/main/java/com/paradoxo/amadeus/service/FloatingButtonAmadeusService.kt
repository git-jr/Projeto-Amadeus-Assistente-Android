package com.paradoxo.amadeus.service

import android.Manifest
import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.activity.AgentDashboardActivity
import com.paradoxo.amadeus.activity.ConfigPrimariaActivity
import com.paradoxo.amadeus.activity.MainActivity
import com.paradoxo.amadeus.cognicao.AIProviderFactory
import com.paradoxo.amadeus.cognicao.AgentInterpreter
import com.paradoxo.amadeus.cognicao.AgentMemoryManager
import com.paradoxo.amadeus.cognicao.CommandInterpreter
import com.paradoxo.amadeus.cognicao.ScreenMapGenerator
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.ActionType
import com.paradoxo.amadeus.modelo.MacroExample
import com.paradoxo.amadeus.modelo.UiElement
import com.paradoxo.amadeus.util.Preferencias.getPrefBool
import com.paradoxo.amadeus.util.Preferencias.setPrefBool
import com.paradoxo.amadeus.view.BorderGlowView
import com.paradoxo.amadeus.view.ScreenHighlightView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class FloatingButtonAmadeusService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var buttonView: View
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var menuOpen = false

    private var voiceOverlayView: View? = null
    private var screenHighlightView: ScreenHighlightView? = null
    private var agentOverlayView: View? = null
    private var recordingOverlayView: View? = null
    private var tvActionCount: TextView? = null
    private var borderGlowView: BorderGlowView? = null
    private var borderGlowParams: WindowManager.LayoutParams? = null
    private var borderGlowAnimator: ValueAnimator? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val wakeWordManager by lazy {
        WakeWordManager(this) { startListening() }
    }

    private val interpreter = CommandInterpreter()
    private val geminiInterpreter: AgentInterpreter = AIProviderFactory.create()
    private val screenMapGenerator by lazy { ScreenMapGenerator(this) }
    private val memoryManager by lazy { AgentMemoryManager(this) }
    private val goalAnalyticsTracker by lazy { GoalAnalyticsTracker(applicationContext) }
    private var lastElements: List<UiElement> = emptyList()
    private var loopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENABLE_WAKE_WORD -> wakeWordManager.startDetection()
            ACTION_DISABLE_WAKE_WORD -> wakeWordManager.stopDetection()
            ACTION_START_LISTENING -> {
                Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 400)
            }
            ACTION_START_RECORDING -> startRecordingMode()
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotification()
        if (!AmadeusAccessibilityService.isActive(this)) {
            desativarBotaoFlutuantePorFaltaDeAcessibilidade()
            return
        }
        createFloatingButton()
        initBorderGlow()
        if (getPrefBool(ConfigPrimariaActivity.PREF_WAKE_WORD_ATIVO, this, true)) {
            wakeWordManager.initialize()
        }
    }

    private fun createNotification() {
        val channelId = "floating_button_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Botão Flutuante",
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
            .setContentText(getString(R.string.botao_flutuante_ativo))
            .setSmallIcon(R.drawable.ic_logo_white)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasMicPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            val fgsType = if (hasMicPermission) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // ── Floating button ───────────────────────────────────────────────────────

    private fun createFloatingButton() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        buttonView = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null)
        setupMainButton()
        setupMenuOptions()
        windowManager.addView(buttonView, layoutParams)
    }

    private fun setupMainButton() {
        val mainButton = buttonView.findViewById<View>(R.id.botaoPrincipal)
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        mainButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (dx * dx + dy * dy > 25) moved = true
                    layoutParams.x = initialX + dx
                    layoutParams.y = initialY + dy
                    windowManager.updateViewLayout(buttonView, layoutParams)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    startListening()
//                    if (!moved) toggleMenu() // Ativar menu
                    true
                }

                else -> false
            }
        }
    }

    private fun setupMenuOptions() {
        buttonView.findViewById<View>(R.id.opcaoOuvir).setOnClickListener {
            closeMenu()
            startListening()
        }
        buttonView.findViewById<View>(R.id.opcaoTirarPrint).setOnClickListener {
            closeMenu()
        }
        buttonView.findViewById<View>(R.id.opcaoObservarTela).setOnClickListener {
            closeMenu()
        }
    }

    private fun toggleMenu() {
        val menuPanel = buttonView.findViewById<View>(R.id.menuPanel)
        menuOpen = !menuOpen
        if (menuOpen) adjustButtonGravity()
        menuPanel.visibility = if (menuOpen) View.VISIBLE else View.GONE
        layoutParams.flags = if (menuOpen) {
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        windowManager.updateViewLayout(buttonView, layoutParams)
    }

    private fun adjustButtonGravity() {
        val screenWidth = resources.displayMetrics.widthPixels
        val mainButton = buttonView.findViewById<View>(R.id.botaoPrincipal)
        val params = mainButton.layoutParams as LinearLayout.LayoutParams
        params.gravity = if (layoutParams.x > screenWidth / 2) Gravity.END else Gravity.START
        mainButton.layoutParams = params
    }

    private fun closeMenu() {
        val menuPanel = buttonView.findViewById<View>(R.id.menuPanel)
        menuOpen = false
        menuPanel.visibility = View.GONE
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        windowManager.updateViewLayout(buttonView, layoutParams)
    }

    // ── Floating message ──────────────────────────────────────────────────────

    private fun showMessage(message: String) {
        val dp = resources.displayMetrics.density
        val tv = TextView(this).apply {
            text = message
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            setPadding(
                (20 * dp).toInt(), (10 * dp).toInt(),
                (20 * dp).toInt(), (10 * dp).toInt()
            )
            background = ContextCompat.getDrawable(
                this@FloatingButtonAmadeusService,
                R.drawable.bg_floating_menu
            )
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (56 * dp).toInt()
        }

        windowManager.addView(tv, params)
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                windowManager.removeView(tv)
            } catch (_: Exception) {
            }
        }, 3000)
    }

    // ── Agent overlay ─────────────────────────────────────────────────────────

    private fun showAgentOverlay() {
        if (agentOverlayView != null) return
        val themedContext = android.view.ContextThemeWrapper(this, R.style.AppTheme)
        agentOverlayView =
            LayoutInflater.from(themedContext).inflate(R.layout.layout_agent_overlay, null)
        agentOverlayView?.findViewById<View>(R.id.agentCancelButton)?.setOnClickListener {
            loopJob?.cancel()
            hideAgentOverlay()
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }
        windowManager.addView(agentOverlayView, params)
    }

    private fun updateAgentMessage(message: String) {
        agentOverlayView?.findViewById<TextView>(R.id.agentStatusText)?.text = message
    }

    private fun hideAgentOverlay() {
        AmadeusAccessibilityService.cancelPendingActions()
        agentOverlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        agentOverlayView = null
    }

    // ── Recording mode ────────────────────────────────────────────────────────

    private fun startRecordingMode() {
        if (recordingOverlayView != null) return
        if (!::windowManager.isInitialized) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        }
        AmadeusAccessibilityService.setOnActionRecordedCallback { count ->
            tvActionCount?.text = if (count == 1) "1 ação gravada" else "$count ações gravadas"
        }
        AmadeusAccessibilityService.startRecording()
        showRecordingOverlay()
    }

    private fun stopRecordingMode() {
        val actions = AmadeusAccessibilityService.stopRecording()
        AmadeusAccessibilityService.setOnActionRecordedCallback(null)
        hideRecordingOverlay()
        if (actions.isEmpty()) {
            showMessage("Nenhuma ação foi gravada")
            return
        }
        val json = memoryManager.serializeActions(actions)
        val intent = Intent(this, com.paradoxo.amadeus.activity.EditarMacroActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(com.paradoxo.amadeus.activity.EditarMacroActivity.EXTRA_RECORDED_ACTIONS_JSON, json)
        }
        startActivity(intent)
    }

    private fun showRecordingOverlay() {
        val themedContext = android.view.ContextThemeWrapper(this, R.style.AppTheme)
        val view = LayoutInflater.from(themedContext).inflate(R.layout.layout_recording_overlay, null)
        tvActionCount = view.findViewById(R.id.tvActionCount)
        view.findViewById<View>(R.id.btnStopRecording).setOnClickListener { stopRecordingMode() }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }

        try {
            windowManager.addView(view, params)
            recordingOverlayView = view
        } catch (_: Exception) {}

        // Blink animation on REC dot
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

    // ── Wake word ─────────────────────────────────────────────────────────────

    private fun startWakeWordDetection() = wakeWordManager.startDetection()

    private fun stopWakeWordDetection() = wakeWordManager.stopDetection()

    // ── Border glow ───────────────────────────────────────────────────────────

    // Pré-cria o BorderGlowView no onCreate para que o addView com FLAG_LAYOUT_NO_LIMITS
    // aconteça uma única vez na inicialização do serviço (sem usuário olhando),
    // não quando o assistente é acionado. showBorderGlow/hideBorderGlow apenas
    // animam o params.alpha via updateViewLayout — sem novo addView, sem flash.
    private fun initBorderGlow() {
        val glow = BorderGlowView(this)
        borderGlowView = glow

        val size = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            size.x = bounds.width()
            size.y = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(size)
        }

        val params = WindowManager.LayoutParams(
            size.x, size.y,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0; y = 0
            alpha = 0f
        }
        borderGlowParams = params
        windowManager.addView(glow, params)
    }

    private fun showBorderGlow() {
        val glow = borderGlowView ?: return
        val params = borderGlowParams ?: return
        glow.startGlow()
        borderGlowAnimator?.cancel()
        borderGlowAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            startDelay = 100
            duration = 1200
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { anim ->
                params.alpha = anim.animatedValue as Float
                try { windowManager.updateViewLayout(glow, params) } catch (_: Exception) {}
            }
            start()
        }
    }

    private fun hideBorderGlow() {
        borderGlowAnimator?.cancel()
        borderGlowAnimator = null
        val glow = borderGlowView ?: return
        val params = borderGlowParams ?: return
        glow.stopGlow()
        params.alpha = 0f
        try { windowManager.updateViewLayout(glow, params) } catch (_: Exception) {}
    }

    // ── Listen ────────────────────────────────────────────────────────────────

    private fun startListening() {
        stopWakeWordDetection()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            showMessage(getString(R.string.permissao_microfone_necessaria))
            solicitarPermissaoMicrofone()
            return
        }
        if (!AmadeusAccessibilityService.isActive(this)) {
            abrirConfigPrimariaParaAcessibilidade()
            desativarBotaoFlutuantePorFaltaDeAcessibilidade()
            return
        }
        showScreenHighlight()
        startSpeechRecognition()
        showBorderGlow()
        showVoiceOverlay()
    }

    private fun solicitarPermissaoMicrofone() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_SOLICITAR_PERMISSAO_MICROFONE, true)
            putExtra(MainActivity.EXTRA_RETORNAR_ESCUTA_BOTAO_FLUTUANTE, true)
        })
    }

    private fun abrirConfigPrimariaParaAcessibilidade() {
        startActivity(Intent(this, ConfigPrimariaActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ConfigPrimariaActivity.EXTRA_SOLICITAR_ACESSIBILIDADE, true)
            putExtra(
                ConfigPrimariaActivity.EXTRA_ATIVAR_BOTAO_FLUTUANTE_APOS_ACESSIBILIDADE,
                true
            )
        })
    }

    private fun desativarBotaoFlutuantePorFaltaDeAcessibilidade() {
        setPrefBool(ConfigPrimariaActivity.PREF_BOTAO_FLUTUANTE, false, applicationContext)
        setPrefBool(ConfigPrimariaActivity.PREF_WAKE_WORD_ATIVO, false, applicationContext)
        wakeWordManager.stopDetection()
        stopSelf()
    }

    private fun showScreenHighlight() {
        if (screenHighlightView != null) return

        val elements = AmadeusAccessibilityService.getVisibleElements()
        if (elements.isEmpty()) return

        lastElements = elements

        screenHighlightView = ScreenHighlightView(this).apply {
            this.elements = elements
            alpha = 0f
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { alpha = 0f }

        windowManager.addView(screenHighlightView, params)

        Handler(Looper.getMainLooper()).post {
            try {
                params.alpha = 1f
                windowManager.updateViewLayout(screenHighlightView, params)
            } catch (_: Exception) {}
        }

        screenHighlightView?.animate()
            ?.alpha(1f)
            ?.setStartDelay(800)
            ?.setDuration(500)
            ?.setInterpolator(android.view.animation.DecelerateInterpolator())
            ?.start()
    }

    private fun removeScreenHighlight() {
        screenHighlightView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        screenHighlightView = null
    }

    private fun showVoiceOverlay() {
        if (voiceOverlayView != null) return

        val themedContext = android.view.ContextThemeWrapper(this, R.style.AppTheme)
        voiceOverlayView =
            LayoutInflater.from(themedContext).inflate(R.layout.layout_voice_overlay, null)

        voiceOverlayView?.findViewById<View>(R.id.cancelarOuvirButton)?.setOnClickListener {
            setPrefBool(ConfigPrimariaActivity.PREF_BOTAO_FLUTUANTE, false, applicationContext)
            closeVoiceOverlay()
            stopSelf()
        }

        voiceOverlayView?.findViewById<View>(R.id.minimizarOuvirButton)?.setOnClickListener {
            closeVoiceOverlay()
        }

        voiceOverlayView?.findViewById<View>(R.id.irParaAmadeusButton)?.setOnClickListener {
            closeVoiceOverlay()
            startActivity(Intent(this, AgentDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }

        val inputField = voiceOverlayView?.findViewById<EditText>(R.id.campoComandoEditText)
        val sendButton = voiceOverlayView?.findViewById<ImageView>(R.id.enviarComandoButton)

        sendButton?.setOnClickListener {
            val text = inputField?.text?.toString()?.trim() ?: return@setOnClickListener
            if (text.isNotEmpty()) {
                inputField.setText("")
                closeVoiceOverlay()
                processCommand(text)
            } else {
                // só para testes
//                processCommand("Abrir YouTube e pesquisar por one piece")
                processCommand("Abra o WhatsApp e mande uma mensagem de teste para o grupo Nozes")
                closeVoiceOverlay()
            }
        }

        inputField?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text =
                    inputField.text?.toString()?.trim() ?: return@setOnEditorActionListener false
                if (text.isNotEmpty()) {
                    inputField.setText("")
                    closeVoiceOverlay()
                    processCommand(text)
                }
                true
            } else false
        }

        // Primeiro frame: flags passivas + alpha 0 no nível de janela.
        // FLAG_NOT_TOUCH_MODAL sem FLAG_NOT_FOCUSABLE força o WM a recalcular
        // regiões de toque em todas as janelas, causando repaint na nav bar.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            alpha = 0f
        }

        voiceOverlayView?.apply {
            alpha = 0f
            translationY = 120f
        }

        windowManager.addView(voiceOverlayView, params)

        // Frame seguinte: ativa interatividade e libera alpha da janela.
        // Neste ponto a View ainda está em alpha=0 (sua animação ainda não começou),
        // então não há flash visível mesmo com params.alpha = 1f.
        Handler(Looper.getMainLooper()).post {
            try {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                params.alpha = 1f
                windowManager.updateViewLayout(voiceOverlayView, params)
            } catch (_: Exception) {}
        }

        voiceOverlayView?.animate()
            ?.alpha(1f)
            ?.translationY(0f)
            ?.setDuration(500)
            ?.setInterpolator(android.view.animation.OvershootInterpolator(3f))
            ?.start()
    }

    private fun startSpeechRecognition() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                updateRecognizedText(text)
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                updateRecognizedText(text)
                fillFieldAndSend(text)
            }

            override fun onError(error: Int) {
                if (error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT &&
                    error != SpeechRecognizer.ERROR_NO_MATCH
                ) {
                    closeVoiceOverlay()
                }
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun updateRecognizedText(text: String) {
        voiceOverlayView?.findViewById<TextView>(R.id.textoOuvidoTextView)?.text = text
    }

    private fun fillFieldAndSend(text: String) {
        val field = voiceOverlayView?.findViewById<EditText>(R.id.campoComandoEditText)
        field?.setText(text)
        Handler(Looper.getMainLooper()).postDelayed({
            closeVoiceOverlay()
            processCommand(text)
        }, 1000)
    }

    // ── Test mode ─────────────────────────────────────────────────────────────
    // Para ativar: mude AGENT_TEST_MODE para true e edite TEST_PLAN abaixo.
    // Para desativar: mude AGENT_TEST_MODE para false.

    private val AGENT_TEST_MODE = false // <-- true = pula Gemini e executa TEST_PLAN direto

    private val TEST_PLAN = listOf(
        // Exemplos — edite à vontade:
//        ActionIntent(ActionType.CLICK,    target = "Galeria"),
//        ActionIntent(ActionType.TYPE,     value  = "one piece"),
//        ActionIntent(ActionType.IME_ACTION),

//        ActionIntent(ActionType.SCROLL_HORIZONTAL, value = "left"),
//        ActionIntent(ActionType.SCROLL_HORIZONTAL, value = "right"),

//        ActionIntent(ActionType.SCROLL, value = "down"),
//        ActionIntent(ActionType.SCROLL, value = "down"),
//        ActionIntent(ActionType.SCROLL, value = "up"),
//        ActionIntent(ActionType.SCROLL, value = "up"),
//        ActionIntent(ActionType.SCROLL, value = "down"),

        // ActionIntent(ActionType.OPEN_APP, target = "WhatsApp"),

//        ActionIntent(ActionType.BACK),
//        ActionIntent(ActionType.RECENTS),
//        ActionIntent(ActionType.HOME),
//        ActionIntent(ActionType.QUICK_SETTINGS),
        ActionIntent(ActionType.NOTIFICATIONS),
    )

    private fun runTestPlan() {
        updateAgentMessage("[TEST] ${TEST_PLAN.size} ação(ões) → iniciando...")
        val sent = AmadeusAccessibilityService.executeActions(
            TEST_PLAN,
            onFeedback = { updateAgentMessage("[TEST] $it") },
            onDone = {
                updateAgentMessage("[TEST] Concluído")
                Handler(Looper.getMainLooper()).postDelayed({ hideAgentOverlay() }, 1500)
            }
        )
        if (!sent) {
            updateAgentMessage(getString(R.string.acessibilidade_nao_ativa))
            Handler(Looper.getMainLooper()).postDelayed({ hideAgentOverlay() }, 2000)
        }
    }

    // ── Command processing ────────────────────────────────────────────────────

    private fun processCommand(goal: String) {
        loopJob?.cancel()
        AmadeusAccessibilityService.cancelPendingActions()
        showAgentOverlay()
        goalAnalyticsTracker.track(goal)

        if (AGENT_TEST_MODE) {
            runTestPlan()
            return
        }

        val allActions = mutableListOf<ActionIntent>()

        loopJob = CoroutineScope(Dispatchers.IO).launch {
            val macro = memoryManager.recallMacro(goal)
            if (macro != null) {
                val actions = memoryManager.resolveActionsReferences(
                    memoryManager.deserializeActions(macro.actionsJson)
                )
                withContext(Dispatchers.Main) { executeMacro(macro, goal, actions) } // ação conhecida para esse comando → executar direto
            } else {
                withContext(Dispatchers.Main) { runGeminiLoop(goal, allActions) } // ação desconhecida → rodar IA loop, deixar ela executar e aprender
            }
        }
    }

    private fun executeMacro(macro: com.paradoxo.amadeus.dao.room.entities.AgentMacroEntity, goal: String, actions: List<ActionIntent>) {
        if (actions.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch { memoryManager.markMacroFailure(macro) }
            runGeminiLoop(goal, mutableListOf())
            return
        }
        updateAgentMessage("Macro: executando ${actions.size} ação(ões) conhecidas...")
        val sent = AmadeusAccessibilityService.executeActions(
            actions,
            onFeedback = { updateAgentMessage(it) },
            onDone = {
                CoroutineScope(Dispatchers.IO).launch { memoryManager.markMacroSuccess(macro) }
                updateAgentMessage("✓ Concluído via macro")
                Handler(Looper.getMainLooper()).postDelayed({ hideAgentOverlay() }, 1500)
            }
        )
        if (!sent) {
            CoroutineScope(Dispatchers.IO).launch { memoryManager.markMacroFailure(macro) }
            runGeminiLoop(goal, mutableListOf())
        }
    }

    private suspend fun loadFewShot(goal: String): List<MacroExample> {
        val goalWords = goal.lowercase().split(Regex("\\s+")).toSet()
        return AmadeusDatabase.getInstance(this)
            .agentMacroDAO().listAll()
            .filter { it.isConfirmed }
            .map { entity ->
                val overlap = entity.goalKey.lowercase().split(Regex("\\s+")).count { it in goalWords }
                entity to overlap
            }
            .sortedByDescending { it.second }
            .take(3)
            .map { (entity, _) ->
                MacroExample(entity.goalKey, memoryManager.deserializeActions(entity.actionsJson))
            }
    }

    private fun runGeminiLoop(goal: String, allActions: MutableList<ActionIntent>) {
        val sessionId = System.currentTimeMillis().toString()
        val history = mutableListOf<String>()
        val maxSteps = 30
        val reusarMacrosNoPrompt = getPrefBool(
            ConfigPrimariaActivity.PREF_REUSAR_MACROS_PROMPT_GEMINI,
            this,
            false
        )
        var step = 0
        var prevElementsKey = ""
        var fewShot = listOf<MacroExample>()
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels

        fun runStep() {
            if (step >= maxSteps) {
                updateAgentMessage("Limite de $maxSteps passos atingido")
                Handler(Looper.getMainLooper()).postDelayed({ hideAgentOverlay() }, 2000)
                return
            }
            step++
            updateAgentMessage("Passo $step/$maxSteps — analisando...")

            val elements = AmadeusAccessibilityService.getVisibleElements()
            val currentElementsKey = elements.joinToString("|") { it.labelLegivel }
            val screenStuck = prevElementsKey.isNotEmpty() && currentElementsKey == prevElementsKey
            if (screenStuck && history.isNotEmpty()) {
                history[history.lastIndex] = history.last() + " — ⚠ TELA NÃO MUDOU, tente outra abordagem"
            }
            prevElementsKey = currentElementsKey
            val bitmap = screenMapGenerator.generate(elements, screenW, screenH, sessionId, step)

            loopJob = CoroutineScope(Dispatchers.IO).launch {
                if (step == 1 && reusarMacrosNoPrompt) {
                    fewShot = loadFewShot(goal)
                }
                val result = geminiInterpreter.interpretStep(
                    goal, history, elements, bitmap, fewShot,
                    onRetry = { attempt, waitMs ->
                        withContext(Dispatchers.Main) {
                            Log.w( "AgenteLog", "AgenteLog, Serviço ocupado, tentativa $attempt/4, esperando ${waitMs}ms para nova tentativa")
                            updateAgentMessage("⏳ Processando... (tentativa $attempt/4)")
                        }
                    }
                )
                withContext(Dispatchers.Main) {
                    when {
                        result.done -> {
                            if (allActions.isNotEmpty()) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    memoryManager.recordExecution(goal, allActions)
                                }
                            }
                            updateAgentMessage("✓ ${result.reason.ifBlank { "Concluído" }}")
                            Handler(Looper.getMainLooper()).postDelayed(
                                { hideAgentOverlay() },
                                1500
                            )
                        }

                        result.actions.isEmpty() -> {
                            updateAgentMessage(
                                getString(R.string.acessibilidade_texto_nao_encontrado, goal)
                            )
                            Handler(Looper.getMainLooper()).postDelayed(
                                { hideAgentOverlay() },
                                2000
                            )
                        }

                        else -> {
                            history.add("[Passo $step] ${result.actions.joinToString { "${it.type} \"${it.target}\"" }}")
                            allActions.addAll(result.actions)
                            val sent = AmadeusAccessibilityService.executeActions(
                                result.actions,
                                onFeedback = { updateAgentMessage(it) },
                                onDone = {
                                    Handler(Looper.getMainLooper()).postDelayed(
                                        { runStep() },
                                        STEP_DELAY_MS
                                    )
                                }
                            )
                            if (!sent) {
                                updateAgentMessage(getString(R.string.acessibilidade_nao_ativa))
                                Handler(Looper.getMainLooper()).postDelayed(
                                    { hideAgentOverlay() },
                                    2000
                                )
                            }
                        }
                    }
                }
            }
        }

        runStep()
    }

    private fun closeVoiceOverlay() {
        speechRecognizer?.destroy()
        speechRecognizer = null

        val overlay = voiceOverlayView
        val highlight = screenHighlightView
        voiceOverlayView = null
        screenHighlightView = null

        overlay?.animate()?.cancel(); overlay?.alpha = 0f
        highlight?.animate()?.cancel(); highlight?.alpha = 0f

        hideBorderGlow()

        Handler(Looper.getMainLooper()).post {
            listOf(overlay, highlight).forEach { view ->
                view ?: return@forEach
                try { windowManager.removeView(view) } catch (_: Exception) {}
            }
        }

        hideAgentOverlay()
        startWakeWordDetection()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        loopJob?.cancel()
        wakeWordManager.release()
        speechRecognizer?.destroy()
        speechRecognizer = null
        voiceOverlayView?.let { windowManager.removeView(it) }
        voiceOverlayView = null
        removeScreenHighlight()
        hideAgentOverlay()
        hideRecordingOverlay()
        AmadeusAccessibilityService.setOnActionRecordedCallback(null)
        borderGlowAnimator?.cancel()
        borderGlowAnimator = null
        borderGlowView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        borderGlowView = null
        borderGlowParams = null
        if (::buttonView.isInitialized) windowManager.removeView(buttonView)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun overlayType() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        WindowManager.LayoutParams.TYPE_PHONE
    }

    companion object Companion {
        private const val NOTIFICATION_ID = 101
        private const val STEP_DELAY_MS = 100L
        const val ACTION_ENABLE_WAKE_WORD = "ACTION_ENABLE_WAKE_WORD"
        const val ACTION_DISABLE_WAKE_WORD = "ACTION_DISABLE_WAKE_WORD"
        const val ACTION_START_LISTENING = "ACTION_START_LISTENING"
        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
    }
}
