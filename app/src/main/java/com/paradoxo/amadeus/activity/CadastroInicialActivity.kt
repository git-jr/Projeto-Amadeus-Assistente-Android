package com.paradoxo.amadeus.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.activity.ConfigPrimariaActivity.Companion.PREF_NOME_IA
import com.paradoxo.amadeus.activity.ConfigPrimariaActivity.Companion.PREF_NOME_USU
import com.paradoxo.amadeus.util.Preferencias.setPrefString
import com.paradoxo.amadeus.util.Util.aplicarInsets
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca

class CadastroInicialActivity : AppCompatActivity() {

    private lateinit var nomeUsuarioEditText: TextInputEditText
    private lateinit var nomeIaEditText: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_inicial)
        aplicarInsets(findViewById(R.id.cadastroInicialLayout))
        configurarToolBarBranca(this)

        nomeUsuarioEditText = findViewById(R.id.nomeUsuarioEditText)
        nomeIaEditText = findViewById(R.id.nomeIaEditText)

        nomeIaEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                verificarNomes()
                true
            } else {
                false
            }
        }

        findViewById<View>(R.id.okButton).setOnClickListener { verificarNomes() }
        configurarTextoTermos()

//        nomeUsuarioEditText.setText("Paradoxo")
//        nomeIaEditText.setText("Amadeus")
//        gravarDados()

    }

    private fun configurarTextoTermos() {
        val textView = findViewById<TextView>(R.id.termosTextView)
        val textoCompleto = getString(R.string.ao_continuar_termos)
        val linkLabel = getString(R.string.termos_e_politica)
        val spannable = SpannableString(textoCompleto)
        val inicio = textoCompleto.indexOf(linkLabel)
        if (inicio >= 0) {
            val fim = inicio + linkLabel.length
            spannable.setSpan(UnderlineSpan(), inicio, fim, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.link_politica_privaicade))))
                }
                override fun updateDrawState(ds: android.text.TextPaint) {
                    ds.isUnderlineText = true
                    ds.color = ds.linkColor
                }
            }, inicio, fim, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        textView.text = spannable
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun verificarNomes() {
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

        gravarDados()
    }

    private fun gravarDados() {
        setPrefString(nomeUsuarioEditText.text.toString().trim(), PREF_NOME_USU, this)
        setPrefString(nomeIaEditText.text.toString().trim(), PREF_NOME_IA, this)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
