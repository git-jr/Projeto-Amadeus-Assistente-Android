package com.paradoxo.amadeus.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.activity.MainActivity
import com.paradoxo.amadeus.cognicao.CommandInterpreter
import com.paradoxo.amadeus.modelo.UiElement
import com.paradoxo.amadeus.view.ScreenHighlightView
import java.util.Locale

class FloatingButtonAmadeusService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var buttonView: View
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var menuOpen = false

    private var voiceOverlayView: View? = null
    private var screenHighlightView: ScreenHighlightView? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val interpreter = CommandInterpreter()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotification()
        createFloatingButton()
    }

    private fun createNotification() {
        val channelId = "floating_button_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Botão Flutuante", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.botao_flutuante_ativo))
                .setSmallIcon(R.drawable.ic_logo_white)
                .setContentIntent(pendingIntent)
                .build()
        )
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
            background = ContextCompat.getDrawable(this@FloatingButtonAmadeusService, R.drawable.bg_floating_menu)
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
            try { windowManager.removeView(tv) } catch (_: Exception) { }
        }, 3000)
    }

    // ── Listen ────────────────────────────────────────────────────────────────

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            showMessage(getString(R.string.permissao_microfone_necessaria))
            return
        }
        if (!AmadeusAccessibilityService.isActive(this)) {
            showMessage(getString(R.string.acessibilidade_nao_ativa))
            AmadeusAccessibilityService.openSettings(this)
            return
        }
        showScreenHighlight()
        showVoiceOverlay()
        startSpeechRecognition()
    }

    private fun showScreenHighlight() {
        if (screenHighlightView != null) return

        val elements = AmadeusAccessibilityService.getVisibleElements()
        if (elements.isEmpty()) return


        val elementsFormated: MutableList<UiElement> = mutableListOf()
        elements.forEach { el ->
            if (el.isClickable || el.isEditable) {
                elementsFormated.add(el)
            }
        }

        println("ELEMENTOS FORMATADOS: ${elementsFormated.size}")

        screenHighlightView = ScreenHighlightView(this).apply {
            this.elements = elements
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(screenHighlightView, params)
    }

    private fun removeScreenHighlight() {
        screenHighlightView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) { }
        }
        screenHighlightView = null
    }

    private fun showVoiceOverlay() {
        if (voiceOverlayView != null) return

        val themedContext = android.view.ContextThemeWrapper(this, R.style.AppTheme)
        voiceOverlayView = LayoutInflater.from(themedContext).inflate(R.layout.layout_voice_overlay, null)

        voiceOverlayView?.findViewById<View>(R.id.cancelarOuvirButton)?.setOnClickListener {
            closeVoiceOverlay()
        }

        val inputField = voiceOverlayView?.findViewById<EditText>(R.id.campoComandoEditText)
        val sendButton = voiceOverlayView?.findViewById<ImageView>(R.id.enviarComandoButton)

        sendButton?.setOnClickListener {
            val text = inputField?.text?.toString()?.trim() ?: return@setOnClickListener
            if (text.isNotEmpty()) {
                inputField.setText("")
                closeVoiceOverlay()
                processCommand(text)
            }
        }

        inputField?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = inputField.text?.toString()?.trim() ?: return@setOnEditorActionListener false
                if (text.isNotEmpty()) {
                    inputField.setText("")
                    closeVoiceOverlay()
                    processCommand(text)
                }
                true
            } else false
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }

        windowManager.addView(voiceOverlayView, params)
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
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

    private fun processCommand(text: String) {
        val actions = interpreter.interpret(text)

        if (actions.isEmpty()) {
            showMessage(getString(R.string.acessibilidade_texto_nao_encontrado, text))
            return
        }

        val sent = AmadeusAccessibilityService.executeActions(
            actions,
            onFeedback = { feedback -> showMessage(feedback) },
            onDone = {}
        )

        if (!sent) {
            showMessage(getString(R.string.acessibilidade_nao_ativa))
        }
    }

    private fun closeVoiceOverlay() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        voiceOverlayView?.let { windowManager.removeView(it) }
        voiceOverlayView = null
        removeScreenHighlight()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        closeVoiceOverlay()
        removeScreenHighlight()
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
    }
}
