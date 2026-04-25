package com.paradoxo.amadeus.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.service.FloatingButtonAmadeusService
import com.paradoxo.amadeus.util.Permissao
import com.paradoxo.amadeus.util.Preferencias.getPrefBool
import com.paradoxo.amadeus.util.Preferencias.getPrefString
import com.paradoxo.amadeus.util.Preferencias.setPrefBool
import com.paradoxo.amadeus.util.Preferencias.setPrefString
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca

class ConfigPrimariaActivity : AppCompatActivity() {

    private lateinit var acessoDadosToggleButton: MaterialSwitch
    private lateinit var botaoFlutuanteToggle: MaterialSwitch
    private lateinit var wakeWordToggle: MaterialSwitch
    private lateinit var reusarMacrosPromptToggle: MaterialSwitch
    private lateinit var nomeUsuarioEditText: TextInputEditText
    private lateinit var nomeIaEditText: TextInputEditText

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val temPermissao = Settings.canDrawOverlays(this)
        botaoFlutuanteToggle.isChecked = temPermissao
        if (temPermissao) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(
                    this,
                    getString(R.string.permissao_microfone_necessaria),
                    Toast.LENGTH_SHORT
                ).show()
                Permissao.solicitarPermissaoMicrofone(this)
            } else {
                ativarBotaoFlutuante()
            }
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_primaria)
        configurarInterface()
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
        wakeWordToggle = findViewById(R.id.wakeWordToggle)
        reusarMacrosPromptToggle = findViewById(R.id.reusarMacrosPromptToggle)

        modoFalaToggleButton.isChecked = getPrefBool(PREF_VOZ_ATIVA, this, false)
        acessoDadosToggleButton.isChecked = getPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO, this, false)
        val botaoFlutuanteAtivo =
            getPrefBool(PREF_BOTAO_FLUTUANTE, this, false) && Settings.canDrawOverlays(this)
        botaoFlutuanteToggle.isChecked = botaoFlutuanteAtivo

        wakeWordToggle.isChecked =
            getPrefBool(PREF_WAKE_WORD_ATIVO, this, false) && botaoFlutuanteAtivo
        wakeWordToggle.isEnabled = botaoFlutuanteAtivo
        reusarMacrosPromptToggle.isChecked =
            getPrefBool(PREF_REUSAR_MACROS_PROMPT_GEMINI, this, false)

        modoFalaToggleButton.setOnCheckedChangeListener { _, valor ->
            setPrefBool(PREF_VOZ_ATIVA, valor, applicationContext)
        }
        acessoDadosToggleButton.setOnCheckedChangeListener { _, valor ->
            setPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO, valor, applicationContext)
        }
        botaoFlutuanteToggle.setOnCheckedChangeListener { _, ativado ->
            if (ativado) {
                if (Settings.canDrawOverlays(this)) {
                    ativarBotaoFlutuante()
                    wakeWordToggle.isEnabled = true
                } else {
                    meuToast(getString(R.string.permissao_overlay_necessaria), applicationContext)
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    overlayPermissionLauncher.launch(intent)
                    botaoFlutuanteToggle.isChecked = false
                }
            } else {
                desativarBotaoFlutuante()
                wakeWordToggle.isChecked = false
                wakeWordToggle.isEnabled = false
                setPrefBool(PREF_WAKE_WORD_ATIVO, false, applicationContext)
            }
        }

        wakeWordToggle.setOnCheckedChangeListener { _, ativado ->
            if (!botaoFlutuanteToggle.isChecked) {
                meuToast(getString(R.string.escuta_requer_botao_flutuante), applicationContext)
                wakeWordToggle.isChecked = false
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

    private fun ativarBotaoFlutuante() {
        setPrefBool(PREF_BOTAO_FLUTUANTE, true, applicationContext)
        startService(Intent(this, FloatingButtonAmadeusService::class.java))
    }

    private fun desativarBotaoFlutuante() {
        setPrefBool(PREF_BOTAO_FLUTUANTE, false, applicationContext)
        stopService(Intent(this, FloatingButtonAmadeusService::class.java))
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

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
