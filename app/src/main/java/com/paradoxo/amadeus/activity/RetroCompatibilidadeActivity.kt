package com.paradoxo.amadeus.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.toEntity
import com.paradoxo.amadeus.enums.AcaoEnum
import com.paradoxo.amadeus.enums.ItemEnum
import com.paradoxo.amadeus.modelo.Sentenca
import com.paradoxo.amadeus.util.Animacoes
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.Toasts.meuToastLong
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RetroCompatibilidadeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_retro_compatibilidade)
        configurarToolBarBranca(this)
        configurarBotaoImportar()
        configurarBotaoNaoImportar()
    }

    private fun configurarBotaoImportar() {
        findViewById<android.view.View>(R.id.botaoImportar).setOnClickListener { importarBancoAntigo(this) }
    }

    private fun configurarBotaoNaoImportar() {
        findViewById<android.view.View>(R.id.botaoNaoImportar).setOnClickListener { voltarParaSplahScreen(this) }
    }

    companion object {
        private fun voltarParaSplahScreen(context: AppCompatActivity) {
            context.lifecycleScope.launch(Dispatchers.IO) {
                val db = AmadeusDatabase.getInstance(context)
                db.mensagemDAO().deletarTodas()
                db.autorDAO().deletarTodos()
                withContext(Dispatchers.Main) {
                    context.startActivity(Intent(context, SplashScreenActivity::class.java))
                    context.finish()
                }
            }
        }

        private fun trocarTextoPreCarregamentoBanco(context: Activity) {
            (context.findViewById<TextView>(R.id.subTituloTextView)).setText(R.string.importando_banco)
            Animacoes.animarComFade(context.findViewById(R.id.avisoLayout), false)
            Animacoes.animarComFade(context.findViewById(R.id.progressoLayout), true)
        }

        private fun atualizarProgresso(porcentagem: Int, context: Activity) {
            (context.findViewById<TextView>(R.id.progressoTextView)).text = "$porcentagem%"
            (context.findViewById<ProgressBar>(R.id.barraProgresso)).progress = porcentagem
        }

        private fun importarBancoAntigo(context: AppCompatActivity) {
            context.lifecycleScope.launch {
                trocarTextoPreCarregamentoBanco(context)
                var sucesso = false

                withContext(Dispatchers.IO) {
                    try {
                        val db = AmadeusDatabase.getInstance(context)
                        val mensagens = db.mensagemDAO().listarRespostasCompleto()
                        val sentencas = mutableListOf<Sentenca>()

                        val progressoTotal = mensagens.size * 2
                        var progressoAtual = 0
                        val idItemIa = ItemEnum.IA.ordinal

                        for (mensagem in mensagens) {
                            val sentenca = Sentenca().apply {
                                acao = AcaoEnum.SEM_ACAO
                                tipo_item = idItemIa
                                chave = mensagem.conteudo
                                addResposta(mensagem.conteudo_resposta ?: "")
                            }
                            sentencas.add(sentenca)

                            progressoAtual++
                            val pct = ((progressoAtual / progressoTotal.toFloat()) * 100).toInt()
                            withContext(Dispatchers.Main) { atualizarProgresso(pct, context) }
                        }

                        for (sentenca in sentencas) {
                            db.sentencaDAO().inserir(sentenca.toEntity())
                            progressoAtual++
                            val pct = ((progressoAtual / progressoTotal.toFloat()) * 100).toInt()
                            withContext(Dispatchers.Main) { atualizarProgresso(pct, context) }
                        }

                        sucesso = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (sucesso) {
                    voltarParaSplahScreen(context)
                } else {
                    meuToast("Erro ao importar o banco de dados anterior", context)
                    meuToastLong("Reinicie o app", context)
                }
            }
        }
    }
}
