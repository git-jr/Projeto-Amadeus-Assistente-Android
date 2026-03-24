package com.paradoxo.amadeus.activity

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.dao.AutorDAO
import com.paradoxo.amadeus.dao.MensagemDAO
import com.paradoxo.amadeus.dao.SentencaDAO
import com.paradoxo.amadeus.enums.AcaoEnum
import com.paradoxo.amadeus.enums.ItemEnum
import com.paradoxo.amadeus.modelo.Sentenca
import com.paradoxo.amadeus.util.Animacoes
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.Toasts.meuToastLong
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca

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
        private fun voltarParaSplahScreen(context: Activity) {
            val mensagemDAO = MensagemDAO(context)
            mensagemDAO.deletarTodasMensagens()

            val autorDAO = AutorDAO(context)
            autorDAO.deletarTodosAutores()

            context.startActivity(Intent(context, SplashScreenActivity::class.java))
            context.finish()
        }

        private fun trocarTextoPreCarregamentoBanco(context: Activity) {
            (context.findViewById<TextView>(R.id.subTituloTextView)).setText(R.string.importando_banco)
            Animacoes.animarComFade(context.findViewById(R.id.avisoLayout), false)
            Animacoes.animarComFade(context.findViewById(R.id.progressoLayout), true)
        }

        @Suppress("DEPRECATION")
        private fun importarBancoAntigo(context: Activity) {
            object : AsyncTask<Void?, Int, Boolean>() {
                override fun onPreExecute() {
                    super.onPreExecute()
                    trocarTextoPreCarregamentoBanco(context)
                }

                override fun doInBackground(vararg voids: Void?): Boolean {
                    return try {
                        val mensagemDAO = MensagemDAO(context)
                        val mensagens = mensagemDAO.listarRespostasCompleto()
                        val sentencas = mutableListOf<Sentenca>()

                        val progressoTotal = mensagens.size * 2
                        var progressoAtual = 0
                        val idItemIa = ItemEnum.IA.ordinal

                        for (mensagem in mensagens) {
                            val sentenca = Sentenca()
                            sentenca.acao = AcaoEnum.SEM_ACAO
                            sentenca.tipo_item = idItemIa
                            sentenca.chave = mensagem.conteudo
                            sentenca.addResposta(mensagem.conteudo_resposta)
                            sentencas.add(sentenca)

                            progressoAtual++
                            publishProgress(((progressoAtual / progressoTotal.toFloat()) * 100).toInt())
                        }

                        val sentencaDAO = SentencaDAO(context, false)
                        for (sentenca in sentencas) {
                            sentencaDAO.inserir(sentenca)
                            progressoAtual++
                            publishProgress(((progressoAtual / progressoTotal.toFloat()) * 100).toInt())
                        }

                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }

                override fun onPostExecute(bancoInserido: Boolean) {
                    super.onPostExecute(bancoInserido)
                    if (bancoInserido) {
                        voltarParaSplahScreen(context)
                    } else {
                        meuToast("Erro ao importar o banco de dados anterior", context)
                        meuToastLong("Reinicie o app", context)
                    }
                }

                override fun onProgressUpdate(vararg values: Int?) {
                    super.onProgressUpdate(*values)
                    val porcentagemProgresso = values[0] ?: 0
                    val progressoAtual = "$porcentagemProgresso%"
                    (context.findViewById<TextView>(R.id.progressoTextView)).text = progressoAtual
                    (context.findViewById<ProgressBar>(R.id.barraProgresso)).progress = porcentagemProgresso
                }
            }.execute()
        }
    }
}
