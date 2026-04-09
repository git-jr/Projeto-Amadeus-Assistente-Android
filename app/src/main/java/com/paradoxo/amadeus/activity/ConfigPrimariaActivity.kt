package com.paradoxo.amadeus.activity

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.service.FloatingButtonAmadeusService
import com.paradoxo.amadeus.util.Permissao
import com.paradoxo.amadeus.util.Preferencias
import com.paradoxo.amadeus.util.Preferencias.getPrefBool
import com.paradoxo.amadeus.util.Preferencias.getPrefString
import com.paradoxo.amadeus.util.Preferencias.setPrefBool
import com.paradoxo.amadeus.util.Preferencias.setPrefString
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca

class ConfigPrimariaActivity : AppCompatActivity() {

    private var primeiroUso = true
    private lateinit var acessoDadosToggleButton: MaterialSwitch
    private lateinit var botaoFlutuanteToggle: MaterialSwitch
    private lateinit var wakeWordToggle: MaterialSwitch
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_primaria)
        configurarInterface()
    }

    private fun configurarInterface() {
        configurarToolBarBranca(this)
        configurarTextInput()
        configuarBotaoLinkStart()
        configurarToggleButton()
    }

    private fun configurarToggleButton() {
        val modoFalaToggleButton = findViewById<MaterialSwitch>(R.id.modoFalaToggleButton)
        acessoDadosToggleButton = findViewById(R.id.acessoDadosToggleButton)
        botaoFlutuanteToggle = findViewById(R.id.botaoFlutuanteToggle)
        wakeWordToggle = findViewById(R.id.wakeWordToggle)

        modoFalaToggleButton.isChecked = getPrefBool(PREF_VOZ_ATIVA, this, false)
        acessoDadosToggleButton.isChecked = getPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO, this, false)
        val botaoFlutuanteAtivo =
            getPrefBool(PREF_BOTAO_FLUTUANTE, this, false) && Settings.canDrawOverlays(this)
        botaoFlutuanteToggle.isChecked = botaoFlutuanteAtivo

        wakeWordToggle.isChecked =
            getPrefBool(PREF_WAKE_WORD_ATIVO, this, false) && botaoFlutuanteAtivo
        wakeWordToggle.isEnabled = botaoFlutuanteAtivo

        modoFalaToggleButton.setOnCheckedChangeListener { _, valor ->
            setPrefBool(
                PREF_VOZ_ATIVA,
                valor,
                applicationContext
            )
        }
        acessoDadosToggleButton.setOnCheckedChangeListener { _, valor ->
            setPrefBool(
                PREF_UPLOAD_DADOS_AUTORIZADO,
                valor,
                applicationContext
            )
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
    }

    private fun ativarBotaoFlutuante() {
        setPrefBool(PREF_BOTAO_FLUTUANTE, true, applicationContext)
        startService(Intent(this, FloatingButtonAmadeusService::class.java))
    }

    private fun desativarBotaoFlutuante() {
        setPrefBool(PREF_BOTAO_FLUTUANTE, false, applicationContext)
        stopService(Intent(this, FloatingButtonAmadeusService::class.java))
    }

    private fun configurarTextInput() {
        nomeUsuarioEditText = findViewById(R.id.nomeUsuarioEditText)
        nomeIaEditText = findViewById(R.id.nomeIaEditText)

        val nomeUsu = getPrefString(PREF_NOME_USU, this)
        if (nomeUsu.isNotEmpty()) {
            val nomeIA = getPrefString(PREF_NOME_IA, this)
            nomeUsuarioEditText.setText(nomeUsu)
            nomeIaEditText.setText(nomeIA)
            primeiroUso = false
        }

        nomeIaEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                verificarNomes()
                true
            } else {
                false
            }
        }
    }

    fun verificarNomes() {
        val nomeUsuarioTextInput = findViewById<TextInputLayout>(R.id.nomeUsuarioTextInput)
        val nomeIATextInput = findViewById<TextInputLayout>(R.id.nomeIaTextInput)

        if (nomeUsuarioEditText.text.toString().isEmpty()) {
            nomeUsuarioTextInput.error = getString(R.string.nome_invalido)
        } else {
            nomeUsuarioTextInput.isErrorEnabled = false
        }

        if (nomeIaEditText.text.toString().isEmpty()) {
            nomeIATextInput.error = getString(R.string.nome_invalido)
            return
        } else {
            nomeIATextInput.isErrorEnabled = false
        }

        dialogUsoDados()
    }

    private fun configuarBotaoLinkStart() {
        findViewById<android.view.View>(R.id.okButton).setOnClickListener { verificarNomes() }
    }

    fun dialogUsoDados() {
        val uploadDadosAutorizado =
            Preferencias.getPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO, this, false)
        if (uploadDadosAutorizado && !primeiroUso) {
            gravarDados()
            return
        }

        val alertDialogBuilder = Dialog(this)
        val inflater = layoutInflater
        alertDialogBuilder.setContentView(
            inflater.inflate(
                R.layout.dialog_sim_nao,
                findViewById(R.id.configPrimariaLayout),
                false
            )
        )
        (alertDialogBuilder.findViewById<TextView>(R.id.tituloTextView)).setText(R.string.acesso_e_uso_dados_titulo)
        (alertDialogBuilder.findViewById<TextView>(R.id.conteudoTexView)).setText(R.string.acesso_e_uso_dados_descri)

        val botaoNegar = alertDialogBuilder.findViewById<TextView>(R.id.botaoNegar)
        botaoNegar.setText(R.string.negar)
        botaoNegar.setOnClickListener {
            setPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO, false, applicationContext)
            acessoDadosToggleButton.isChecked = false
            alertDialogBuilder.dismiss()
            gravarDados()
        }

        val botaoAutorizar = alertDialogBuilder.findViewById<MaterialButton>(R.id.botaoAutorizar)
        botaoAutorizar.setText(R.string.autorizar)
        botaoAutorizar.setOnClickListener {
            setPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO, true, applicationContext)
            acessoDadosToggleButton.isChecked = true
            alertDialogBuilder.dismiss()
            gravarDados()
        }

        alertDialogBuilder.findViewById<android.view.View>(R.id.linkTextView).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(getString(R.string.link_politica_privaicade))
            startActivity(intent)
        }

        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.show()
    }

    private fun gravarDados() {
        setPrefString(nomeUsuarioEditText.text.toString().trim(), PREF_NOME_USU, this)
        setPrefString(nomeIaEditText.text.toString().trim(), PREF_NOME_IA, this)

        if (primeiroUso) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            finish()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
