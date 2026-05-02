package com.paradoxo.amadeus.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.service.AmadeusAccessibilityService
import com.paradoxo.amadeus.service.FloatingButtonAmadeusService
import com.paradoxo.amadeus.util.DialogPermissao
import com.paradoxo.amadeus.util.Permissao
import com.paradoxo.amadeus.util.Preferencias.getPrefBool
import com.paradoxo.amadeus.util.Preferencias.getPrefString
import com.paradoxo.amadeus.util.Preferencias.setPrefBool
import com.paradoxo.amadeus.util.Preferencias.setPrefString
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.Util.aplicarInsets
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca
import com.paradoxo.amadeus.util.canDrawOverlays

class ConfigPrimariaActivity : AppCompatActivity() {

    private lateinit var acessoDadosToggleButton: MaterialSwitch
    private lateinit var botaoFlutuanteToggle: MaterialSwitch
    private lateinit var wakeWordToggle: MaterialSwitch
    private lateinit var acessibilidadeToggle: MaterialSwitch
    private lateinit var reusarMacrosPromptToggle: MaterialSwitch
    private lateinit var nomeUsuarioEditText: TextInputEditText
    private lateinit var nomeIaEditText: TextInputEditText
    private var sincronizandoToggles = false
    private var solicitarDialogAcessibilidadeAoEntrar = false
    private var aguardandoRetornoConfiguracaoAcessibilidade = false
    private var ativarBotaoFlutuanteAposAcessibilidade = false
    private var aguardandoRetornoAcessibilidadeAtivacao = false
    private var ativarBotaoAposAtivarAcessibilidade = false

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (canDrawOverlays()) {
            if (AmadeusAccessibilityService.isActive(this)) {
                ativarBotaoFlutuante()
            } else {
                solicitarAcessibilidadeComRationale()
            }
        } else {
            exibirFeedbackEReverterAtivacaoBotaoFlutuante(R.string.permissao_overlay_nao_concedida)
        }
    }

    companion object {
        const val PREF_NOME_IA = "nomeIA"
        const val PREF_NOME_USU = "nomeUsu"
        const val PREF_VOZ_ATIVA = "voz_ativa"
        const val PREF_UPLOAD_DADOS_AUTORIZADO = "upload_dados_autorizado"
        const val PREF_BOTAO_FLUTUANTE = "botao_flutuante"
        const val PREF_WAKE_WORD_ATIVO = "wake_word_ativo"
        const val PREF_REUSAR_MACROS_PROMPT_GEMINI = "reusar_macros_prompt_gemini"
        const val EXTRA_SOLICITAR_ACESSIBILIDADE = "solicitar_acessibilidade"
        const val EXTRA_ATIVAR_BOTAO_FLUTUANTE_APOS_ACESSIBILIDADE =
            "ativar_botao_flutuante_apos_acessibilidade"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_primaria)
        aplicarInsets(findViewById(R.id.configPrimariaLayout))
        processarIntent(intent)
        configurarInterface()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processarIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        sincronizarEstadoBotaoFlutuante()
        processarSolicitacoesPendentes()
    }

    private fun configurarInterface() {
        configurarToolBarBranca(this)
        configurarTextInput()
        configurarBotaoSalvar()
        configurarToggleButton()
        configurarBotaoAssistente()
        configurarBotaoMacros()
    }

    private fun configurarToggleButton() {
        val modoFalaToggleButton = findViewById<MaterialSwitch>(R.id.modoFalaToggleButton)
        acessoDadosToggleButton = findViewById(R.id.acessoDadosToggleButton)
        botaoFlutuanteToggle = findViewById(R.id.botaoFlutuanteToggle)
        acessibilidadeToggle = findViewById(R.id.acessibilidadeToggle)
        wakeWordToggle = findViewById(R.id.wakeWordToggle)
        reusarMacrosPromptToggle = findViewById(R.id.reusarMacrosPromptToggle)

        modoFalaToggleButton.isChecked = getPrefBool(PREF_VOZ_ATIVA, this, false)
        acessoDadosToggleButton.isChecked = getPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO, this, true)
        reusarMacrosPromptToggle.isChecked =
            getPrefBool(PREF_REUSAR_MACROS_PROMPT_GEMINI, this, false)
        sincronizarEstadoBotaoFlutuante()

        modoFalaToggleButton.setOnCheckedChangeListener { _, valor ->
            setPrefBool(PREF_VOZ_ATIVA, valor, applicationContext)
        }
        acessoDadosToggleButton.setOnCheckedChangeListener { _, valor ->
            setPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO, valor, applicationContext)
        }
        acessibilidadeToggle.setOnCheckedChangeListener { _, ativado ->
            if (sincronizandoToggles) return@setOnCheckedChangeListener
            if (ativado) {
                if (!AmadeusAccessibilityService.isActive(this)) {
                    aguardandoRetornoAcessibilidadeAtivacao = true
                    ativarBotaoAposAtivarAcessibilidade = canDrawOverlays()
                    solicitarAcessibilidadeComRationale(ativarBotaoAoConcluir = false)
                }
            } else {
                setPrefBool(ConfigPrimariaActivity.PREF_BOTAO_FLUTUANTE, false, applicationContext)
                desativarBotaoFlutuanteCompleto()
            }
        }
        botaoFlutuanteToggle.setOnCheckedChangeListener { _, ativado ->
            if (sincronizandoToggles) return@setOnCheckedChangeListener
            if (ativado) {
                iniciarFluxoAtivacaoBotaoFlutuante()
            } else {
                desativarBotaoFlutuanteCompleto()
            }
        }

        wakeWordToggle.setOnCheckedChangeListener { _, ativado ->
            if (sincronizandoToggles) return@setOnCheckedChangeListener
            if (!botaoFlutuanteToggle.isChecked) {
                meuToast(getString(R.string.escuta_requer_botao_flutuante), applicationContext)
                atualizarToggles {
                    wakeWordToggle.isChecked = false
                }
                return@setOnCheckedChangeListener
            }
            setPrefBool(PREF_WAKE_WORD_ATIVO, ativado, applicationContext)
            val action = if (ativado) FloatingButtonAmadeusService.ACTION_ENABLE_WAKE_WORD
            else FloatingButtonAmadeusService.ACTION_DISABLE_WAKE_WORD
            startService(Intent(this, FloatingButtonAmadeusService::class.java).apply {
                this.action = action
            })
        }

        reusarMacrosPromptToggle.setOnCheckedChangeListener { _, ativado ->
            setPrefBool(PREF_REUSAR_MACROS_PROMPT_GEMINI, ativado, applicationContext)
        }
    }

    private fun processarIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_SOLICITAR_ACESSIBILIDADE, false) == true) {
            solicitarDialogAcessibilidadeAoEntrar = true
            ativarBotaoFlutuanteAposAcessibilidade = intent.getBooleanExtra(
                EXTRA_ATIVAR_BOTAO_FLUTUANTE_APOS_ACESSIBILIDADE,
                true
            )
        }
    }

    private fun processarSolicitacoesPendentes() {
        if (aguardandoRetornoAcessibilidadeAtivacao) {
            aguardandoRetornoAcessibilidadeAtivacao = false
            sincronizarEstadoBotaoFlutuante()
            if (ativarBotaoAposAtivarAcessibilidade &&
                AmadeusAccessibilityService.isActive(this) &&
                canDrawOverlays()) {
                ativarBotaoFlutuante()
            }
            ativarBotaoAposAtivarAcessibilidade = false
            return
        }

        if (aguardandoRetornoConfiguracaoAcessibilidade) {
            aguardandoRetornoConfiguracaoAcessibilidade = false
            if (AmadeusAccessibilityService.isActive(this) && canDrawOverlays()) {
                if (ativarBotaoFlutuanteAposAcessibilidade) {
                    ativarBotaoFlutuante()
                }
            } else {
                exibirFeedbackEReverterAtivacaoBotaoFlutuante(R.string.acessibilidade_nao_concedida)
            }
            ativarBotaoFlutuanteAposAcessibilidade = false
            return
        }

        if (solicitarDialogAcessibilidadeAoEntrar && !AmadeusAccessibilityService.isActive(this)) {
            solicitarDialogAcessibilidadeAoEntrar = false
            window.decorView.post {
                solicitarAcessibilidadeComRationale(ativarBotaoFlutuanteAposAcessibilidade)
            }
        }
    }

    private fun iniciarFluxoAtivacaoBotaoFlutuante() {
        when {
            !canDrawOverlays() -> solicitarPermissaoOverlayComRationale()
            !AmadeusAccessibilityService.isActive(this) -> solicitarAcessibilidadeComRationale()
            else -> ativarBotaoFlutuante()
        }
    }

    private fun sincronizarEstadoBotaoFlutuante() {
        val botaoPodeFicarAtivo =
            getPrefBool(PREF_BOTAO_FLUTUANTE, this, false) &&
                canDrawOverlays() &&
                AmadeusAccessibilityService.isActive(this)

        if (!botaoPodeFicarAtivo) {
            setPrefBool(PREF_BOTAO_FLUTUANTE, false, applicationContext)
            setPrefBool(PREF_WAKE_WORD_ATIVO, false, applicationContext)
            stopService(Intent(this, FloatingButtonAmadeusService::class.java))
        }

        atualizarToggles {
            acessibilidadeToggle.isChecked = AmadeusAccessibilityService.isActive(this)
            botaoFlutuanteToggle.isChecked = botaoPodeFicarAtivo
            wakeWordToggle.isChecked =
                botaoPodeFicarAtivo && getPrefBool(PREF_WAKE_WORD_ATIVO, this, false)
            wakeWordToggle.isEnabled = botaoPodeFicarAtivo
        }
    }

    private fun atualizarToggles(bloco: () -> Unit) {
        sincronizandoToggles = true
        try {
            bloco()
        } finally {
            sincronizandoToggles = false
        }
    }

    private fun ativarBotaoFlutuante() {
        if (!canDrawOverlays() || !AmadeusAccessibilityService.isActive(this)) {
            sincronizarEstadoBotaoFlutuante()
            return
        }
        setPrefBool(PREF_BOTAO_FLUTUANTE, true, applicationContext)
        startService(Intent(this, FloatingButtonAmadeusService::class.java))
        atualizarToggles {
            botaoFlutuanteToggle.isChecked = true
            wakeWordToggle.isEnabled = true
        }
    }

    private fun desativarBotaoFlutuante() {
        setPrefBool(PREF_BOTAO_FLUTUANTE, false, applicationContext)
        stopService(Intent(this, FloatingButtonAmadeusService::class.java))
    }

    private fun desativarBotaoFlutuanteCompleto() {
        desativarBotaoFlutuante()
        setPrefBool(PREF_WAKE_WORD_ATIVO, false, applicationContext)
        atualizarToggles {
            wakeWordToggle.isChecked = false
            wakeWordToggle.isEnabled = false
        }
    }

    private fun exibirFeedbackEReverterAtivacaoBotaoFlutuante(messageResId: Int) {
        desativarBotaoFlutuanteCompleto()
        atualizarToggles {
            botaoFlutuanteToggle.isChecked = false
        }
        meuToast(getString(messageResId), applicationContext)
    }

    private fun configurarBotaoAssistente() {
        findViewById<android.view.View>(R.id.assistenteVozLayout).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun configurarBotaoMacros() {
        findViewById<android.view.View>(R.id.macrosLayout).setOnClickListener {
            startActivity(Intent(this, ListaMacrosActivity::class.java))
        }
    }

    private fun configurarTextInput() {
        nomeUsuarioEditText = findViewById(R.id.nomeUsuarioEditText)
        nomeIaEditText = findViewById(R.id.nomeIaEditText)

        nomeUsuarioEditText.setText(getPrefString(PREF_NOME_USU, this))
        nomeIaEditText.setText(getPrefString(PREF_NOME_IA, this))

        nomeIaEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                salvarNomes()
                true
            } else {
                false
            }
        }
    }

    private fun configurarBotaoSalvar() {
        findViewById<android.view.View>(R.id.okButton).setOnClickListener { salvarNomes() }
    }

    private fun salvarNomes() {
        val nomeUsuarioTextInput = findViewById<TextInputLayout>(R.id.nomeUsuarioTextInput)
        val nomeIATextInput = findViewById<TextInputLayout>(R.id.nomeIaTextInput)

        if (nomeUsuarioEditText.text.toString().isEmpty()) {
            nomeUsuarioTextInput.error = getString(R.string.nome_invalido)
            return
        } else {
            nomeUsuarioTextInput.isErrorEnabled = false
        }

        if (nomeIaEditText.text.toString().isEmpty()) {
            nomeIATextInput.error = getString(R.string.nome_invalido)
            return
        } else {
            nomeIATextInput.isErrorEnabled = false
        }

        setPrefString(nomeUsuarioEditText.text.toString().trim(), PREF_NOME_USU, this)
        setPrefString(nomeIaEditText.text.toString().trim(), PREF_NOME_IA, this)
        finish()
    }

    private fun solicitarPermissaoOverlayComRationale() {
        DialogPermissao.exibirRationale(
            activity = this,
            titulo = getString(R.string.permissao_overlay_rationale_titulo),
            mensagem = getString(R.string.permissao_overlay_rationale_mensagem),
            textoConfirmar = getString(R.string.ir_para_configuracoes),
            textoCancelar = getString(R.string.agora_nao),
            onConfirmar = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    overlayPermissionLauncher.launch(
                        Permissao.getIntentSolicitarSobreposicao(packageName)
                    )
                }
            },
            onCancelar = {
                exibirFeedbackEReverterAtivacaoBotaoFlutuante(R.string.permissao_overlay_nao_concedida)
            }
        )
    }

    private fun solicitarAcessibilidadeComRationale(ativarBotaoAoConcluir: Boolean = true) {
        DialogPermissao.exibirRationale(
            activity = this,
            titulo = getString(R.string.acessibilidade_rationale_titulo),
            mensagem = getString(R.string.acessibilidade_rationale_mensagem),
            textoConfirmar = getString(R.string.ir_para_configuracoes),
            textoCancelar = getString(R.string.agora_nao),
            onConfirmar = {
                ativarBotaoFlutuanteAposAcessibilidade = ativarBotaoAoConcluir
                aguardandoRetornoConfiguracaoAcessibilidade = true
                AmadeusAccessibilityService.openSettings(this)
            },
            onCancelar = {
                ativarBotaoFlutuanteAposAcessibilidade = false
                solicitarDialogAcessibilidadeAoEntrar = false
                exibirFeedbackEReverterAtivacaoBotaoFlutuante(R.string.acessibilidade_nao_concedida)
            }
        )
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
