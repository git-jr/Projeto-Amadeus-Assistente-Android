package com.paradoxo.amadeus.activity

import android.app.Activity
import android.app.Dialog
import android.os.AsyncTask
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.adapter.AdapterSimples
import com.paradoxo.amadeus.dao.AcaoDAO
import com.paradoxo.amadeus.enums.AcaoEnum
import com.paradoxo.amadeus.modelo.Sentenca
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca

class ListaAcaoActivity : AppCompatActivity() {

    companion object {
        var adapter: AdapterSimples? = null
        var textoBusca: String? = null

        private fun gerarListaAcoes(busca: String?, context: Activity): List<Sentenca> {
            val acaoDAO = AcaoDAO(context)
            val acoes = acaoDAO.getAcoes() ?: emptyList()
            val itens = mutableListOf<Sentenca>()

            for (acao in acoes) {
                val sentenca = Sentenca()
                sentenca.acao = AcaoEnum.SEM_ACAO
                val nome = acao.acaoEnum.toString()
                sentenca.chave = nome.replace("_", " ")
                sentenca.addResposta("Gatilhos: " + acao.gatilhos.toString().replace("[", "").replace("]", ""))

                if (busca == null || nome.lowercase().contains(busca) || acao.gatilhos.toString().lowercase().contains(busca)) {
                    itens.add(sentenca)
                }
            }
            return itens
        }

        private fun atualizarRecycler(sentencas: List<Sentenca>) {
            adapter?.addAll(sentencas)
        }

        @Suppress("DEPRECATION")
        private fun carregaSentencaBanco(context: Activity) {
            object : AsyncTask<Void?, Void?, List<Sentenca>>() {
                override fun doInBackground(vararg voids: Void?): List<Sentenca> {
                    return if (textoBusca.isNullOrEmpty()) {
                        gerarListaAcoes(null, context)
                    } else {
                        gerarListaAcoes(textoBusca, context)
                    }
                }

                override fun onPostExecute(sentencas: List<Sentenca>) {
                    super.onPostExecute(sentencas)
                    if (textoBusca.isNullOrEmpty()) {
                        atualizarRecycler(sentencas)
                    } else {
                        adapter?.trocarLista(sentencas)
                    }
                }
            }.execute()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_sentencas)
        configurarIterface()
    }

    private fun configurarIterface() {
        configurarToolBarBranca(this)
        configurarRecycler()
        configurarBotaoAdicionar()
        configurarBotaoBusca()
    }

    private fun configurarBotaoBusca() {
        (findViewById<EditText>(R.id.buscaSentencaEditText)).setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                textoBusca = textView.text.toString().lowercase().trim()
                carregaSentencaBanco(this)
                true
            } else {
                false
            }
        }
    }

    private fun configurarBotaoAdicionar() {
        val imageView = findViewById<ImageView>(R.id.adicionarButton)
        imageView.setImageResource(R.drawable.ic_search)

        findViewById<android.view.View>(R.id.adicionarButton).setOnClickListener {
            textoBusca = (findViewById<EditText>(R.id.buscaSentencaEditText)).text.toString().trim()
            carregaSentencaBanco(this)
        }
    }

    private fun configurarRecycler() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = AdapterSimples(mutableListOf())
        recyclerView.adapter = adapter

        carregaSentencaBanco(this)

        adapter?.setOnItemClickListener { _, sentenca, _ -> abrirDialogVerAcao(sentenca) }
        adapter?.setOnLongClickListener { _, _, _ -> }
    }

    fun abrirDialogVerAcao(sentenca: Sentenca) {
        val alertDialogBuilder = Dialog(this)
        alertDialogBuilder.setContentView(
            layoutInflater.inflate(R.layout.dialog_ver_acao, findViewById(R.id.listaSetencasLayout))
        )
        (alertDialogBuilder.findViewById<TextView>(R.id.tituloTextView)).text = sentenca.chave
        (alertDialogBuilder.findViewById<TextView>(R.id.conteudoTexView)).text =
            sentenca.respostas.toString().replace("[", "").replace("]", "")

        alertDialogBuilder.findViewById<android.view.View>(R.id.botaoOk).setOnClickListener {
            alertDialogBuilder.dismiss()
        }
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.show()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
