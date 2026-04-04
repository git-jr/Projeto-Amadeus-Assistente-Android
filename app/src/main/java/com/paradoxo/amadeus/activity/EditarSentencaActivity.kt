package com.paradoxo.amadeus.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.toEntity
import com.paradoxo.amadeus.dao.room.toModel
import com.paradoxo.amadeus.enums.AcaoEnum
import com.paradoxo.amadeus.enums.ItemEnum
import com.paradoxo.amadeus.modelo.Sentenca
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditarSentencaActivity : AppCompatActivity() {

    companion object {
        var idItem: String? = null
        var respostas: MutableList<TextInputEditText> = mutableListOf()
        var entradaEditText: TextInputEditText? = null
        var layoutsRepostas: MutableList<LinearLayout> = mutableListOf()

        private fun finilizarActivity(context: Activity) {
            context.startActivity(Intent(context, ListaSentencaActivity::class.java))
            context.finish()
        }

        fun gravarDados(entradaValida: String, respostasValidas: List<String>, context: AppCompatActivity) {
            context.lifecycleScope.launch {
                meuToast(context.getString(R.string.salvando_dados), context)

                withContext(Dispatchers.IO) {
                    val sentenca = Sentenca().apply {
                        chave = entradaValida
                        respostas = respostasValidas.toMutableList()
                    }
                    val dao = AmadeusDatabase.getInstance(context).sentencaDAO()
                    if (idItem == null) {
                        dao.inserir(sentenca.toEntity())
                    } else {
                        sentenca.id = idItem
                        sentenca.acao = AcaoEnum.SEM_ACAO
                        sentenca.tipo_item = ItemEnum.USUARIO.ordinal
                        dao.alterar(sentenca.toEntity())
                    }
                }

                meuToast(context.getString(R.string.salvo), context)
                finilizarActivity(context)
            }
        }

        private fun carregarSentenca(context: AppCompatActivity) {
            context.lifecycleScope.launch {
                idItem = context.intent.getStringExtra("idItem")
                val id = idItem?.toIntOrNull() ?: return@launch

                val sentenca = withContext(Dispatchers.IO) {
                    AmadeusDatabase.getInstance(context).sentencaDAO().buscaPorId(id)?.toModel()
                } ?: return@launch

                entradaEditText?.setText(sentenca.chave)
                for ((idx, resposta) in sentenca.respostas.withIndex()) {
                    respostas[idx].setText(resposta)
                    respostas[idx].visibility = View.VISIBLE
                    layoutsRepostas[idx].visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_item)
        configurarInterface()
    }

    private fun configurarInterface() {
        configurarToolBarBranca(this)
        configurarTextInput()
        configuarBotaoLinkStart()
        configuarBotaoAddResposta()
    }

    private fun configurarTextInput() {
        entradaEditText = findViewById(R.id.entradaEditText)

        respostas = mutableListOf(
            findViewById(R.id.resposta1EditText),
            findViewById(R.id.resposta2EditText),
            findViewById(R.id.resposta3EditText),
            findViewById(R.id.resposta4EditText),
            findViewById(R.id.resposta5EditText)
        )

        layoutsRepostas = mutableListOf(
            findViewById(R.id.resposta1Layout),
            findViewById(R.id.resposta2Layout),
            findViewById(R.id.resposta3LayoutButton),
            findViewById(R.id.resposta4Layout),
            findViewById(R.id.resposta5Layout)
        )

        carregarSentenca(this)
    }

    private fun configuarBotaoLinkStart() {
        findViewById<View>(R.id.okButton).setOnClickListener { validarInputs() }
    }

    private fun configuarBotaoAddResposta() {
        findViewById<View>(R.id.adicionarRespostaButton).setOnClickListener { adicionarMaisUmaResposta() }
    }

    private fun adicionarMaisUmaResposta() {
        var totalVisiveis = 0
        for (layoutResposta in layoutsRepostas) {
            if (layoutResposta.visibility == View.VISIBLE) {
                totalVisiveis++
            } else {
                layoutResposta.visibility = View.VISIBLE
                respostas[layoutsRepostas.indexOf(layoutResposta)].visibility = View.VISIBLE
                return
            }
        }
        if (totalVisiveis == layoutsRepostas.size) {
            meuToast(getString(R.string.limite_de_respostas_atingido), applicationContext)
        }
    }

    fun deletarResposta(view: View) {
        val idResposta = view.tag.toString().toInt() - 1
        layoutsRepostas[idResposta].visibility = View.GONE
        respostas[idResposta].visibility = View.GONE
        respostas[idResposta].text?.clear()
    }

    fun validarInputs() {
        val entradaTextInput = findViewById<TextInputLayout>(R.id.entradaTextInput)

        if (entradaEditText?.text.toString().isEmpty()) {
            entradaTextInput.error = getString(R.string.entrada_invalida)
            return
        } else {
            entradaTextInput.isErrorEnabled = false
        }

        var totalVisiveis = 0
        val respostasValidas = mutableListOf<String>()
        for (resposta in respostas) {
            if (resposta.visibility != View.GONE) {
                totalVisiveis++
                if (resposta.text.toString().isEmpty()) {
                    resposta.error = getString(R.string.resposta_invalida)
                    return
                } else {
                    respostasValidas.add(resposta.text.toString().trim())
                }
            }
        }

        if (totalVisiveis == 0) {
            meuToast(getString(R.string.deve_haver_ao_menos_uma_reposta), applicationContext)
        } else {
            gravarDados(entradaEditText?.text.toString().trim(), respostasValidas, this)
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        finilizarActivity(this)
    }
}
