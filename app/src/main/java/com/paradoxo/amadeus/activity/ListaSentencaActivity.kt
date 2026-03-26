package com.paradoxo.amadeus.activity

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.adapter.AdapterSimples
import com.paradoxo.amadeus.adapter.SimpleCallbackSentenca
import com.paradoxo.amadeus.dao.SentencaDAO
import com.paradoxo.amadeus.modelo.Sentenca
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca

class ListaSentencaActivity : AppCompatActivity() {

    companion object {
        var textoBusca: String? = null
        var adapterSimples: AdapterSimples? = null
        var limiteCarregarItensRecycler: Long = 0

        private const val LIMITE_ITENS_PADRAO = 100L

        private fun atualizarRecycler(sentencas: List<Sentenca>) {
            try {
                adapterSimples?.addAll(sentencas.subList(adapterSimples!!.itemCount, limiteCarregarItensRecycler.toInt()))
            } catch (e: Exception) {
                adapterSimples?.addAll(sentencas.subList(adapterSimples!!.itemCount, sentencas.size))
            }
        }

        @Suppress("DEPRECATION")
        private fun carregaSentencaBanco(context: Activity) {
            object : AsyncTask<Void?, Void?, List<Sentenca>>() {
                override fun onPreExecute() {
                    super.onPreExecute()
                    limiteCarregarItensRecycler += LIMITE_ITENS_PADRAO
                }

                override fun doInBackground(vararg voids: Void?): List<Sentenca> {
                    val sentencaDAO = SentencaDAO(context, false)
                    return if (textoBusca.isNullOrEmpty()) {
                        sentencaDAO.listar(limiteCarregarItensRecycler)
                    } else {
                        limiteCarregarItensRecycler = LIMITE_ITENS_PADRAO
                        sentencaDAO.buscaPorChaveLista(textoBusca ?: "", limiteCarregarItensRecycler)
                    }
                }

                override fun onPostExecute(sentencas: List<Sentenca>) {
                    super.onPostExecute(sentencas)
                    if (textoBusca.isNullOrEmpty()) {
                        atualizarRecycler(sentencas)
                    } else {
                        adapterSimples?.trocarLista(sentencas)
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
                textoBusca = textView.text.toString().trim()
                carregaSentencaBanco(this)
                true
            } else {
                false
            }
        }
    }

    private fun configurarBotaoAdicionar() {
        findViewById<android.view.View>(R.id.adicionarButton).setOnClickListener {
            startActivity(Intent(applicationContext, EditarSentencaActivity::class.java))
            finish()
        }
    }

    private fun configurarRecycler() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapterSimples = AdapterSimples(mutableListOf())
        recyclerView.adapter = adapterSimples

        carregaSentencaBanco(this)

        adapterSimples?.setOnItemClickListener { _, sentenca, _ ->
            Log.e("nome", sentenca.chave.toString())
            Log.e("tipo", sentenca.tipo_item.toString())
            val intent = Intent(applicationContext, EditarSentencaActivity::class.java)
            intent.putExtra("idItem", sentenca.id)
            startActivity(intent)
            finish()
        }

        adapterSimples?.setOnLongClickListener { _, _, _ -> }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val posiUltimoItem = layoutManager.findLastCompletelyVisibleItemPosition()
                if (posiUltimoItem.toLong() == limiteCarregarItensRecycler - 1) {
                    carregaSentencaBanco(this@ListaSentencaActivity)
                }
            }
        })

        val itemTouchHelper = ItemTouchHelper(SimpleCallbackSentenca(adapterSimples!!, this))
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
