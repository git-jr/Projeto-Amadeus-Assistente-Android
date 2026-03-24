package com.paradoxo.amadeus.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.adapter.AdapterSimplesEntidade
import com.paradoxo.amadeus.dao.EntidadeDAO
import com.paradoxo.amadeus.fragments.DialogSimples
import com.paradoxo.amadeus.modelo.Entidade
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca

class ListaEntidadeActivity : AppCompatActivity(), DialogSimples.FragmentDialogInterface {

    companion object {
        var textoBusca: String? = null
        var limiteCarregarItensRecycler: Long = 0
        var adapterSimples: AdapterSimplesEntidade? = null

        const val LIMITE_ITENS_PADRAO = 100L

        private fun atualizarRecycler(entidades: List<Entidade>) {
            try {
                adapterSimples?.addAll(entidades.subList(adapterSimples!!.itemCount, limiteCarregarItensRecycler.toInt()))
            } catch (e: Exception) {
                adapterSimples?.addAll(entidades.subList(adapterSimples!!.itemCount, entidades.size))
            }
        }

        @Suppress("DEPRECATION")
        private fun carregaSentencaBanco(context: Activity) {
            object : AsyncTask<Void?, Void?, List<Entidade>>() {
                override fun onPreExecute() {
                    super.onPreExecute()
                    limiteCarregarItensRecycler += LIMITE_ITENS_PADRAO
                }

                override fun doInBackground(vararg voids: Void?): List<Entidade> {
                    val entidadeDAO = EntidadeDAO(context)
                    return if (textoBusca.isNullOrEmpty()) {
                        entidadeDAO.listar(limiteCarregarItensRecycler)
                    } else {
                        limiteCarregarItensRecycler = LIMITE_ITENS_PADRAO
                        entidadeDAO.buscaPorChaveLista(textoBusca, limiteCarregarItensRecycler)
                    }
                }

                override fun onPostExecute(entidades: List<Entidade>) {
                    super.onPostExecute(entidades)
                    if (textoBusca.isNullOrEmpty()) {
                        atualizarRecycler(entidades)
                    } else {
                        adapterSimples?.trocarLista(entidades)
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

    private fun configurarBotaoAdicionar() {
        findViewById<android.view.View>(R.id.adicionarButton).setOnClickListener {
            startActivity(Intent(applicationContext, EditarItemEntidadeNovoActivity::class.java))
            finish()
        }
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

    private fun configurarRecycler() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapterSimples = AdapterSimplesEntidade(mutableListOf())
        recyclerView.adapter = adapterSimples

        carregaSentencaBanco(this)

        adapterSimples?.setOnItemClickListener { _, entidade, _ ->
            Log.e("nome", entidade.nome.toString())
            val gson = Gson()
            val intent = Intent(applicationContext, EditarItemEntidadeNovoActivity::class.java)
            intent.putExtra("entidade", gson.toJson(entidade))
            startActivity(intent)
            finish()
        }

        adapterSimples?.setOnLongClickListener { _, position, _ ->
            abrirDialogExcluir(position)
            vibrar()
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val posiUltimoItem = layoutManager.findLastCompletelyVisibleItemPosition()
                if (posiUltimoItem.toLong() == limiteCarregarItensRecycler - 1) {
                    carregaSentencaBanco(this@ListaEntidadeActivity)
                }
            }
        })
    }

    @Suppress("DEPRECATION")
    private fun vibrar() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrate(100)
    }

    fun abrirDialogExcluir(posi: Int) {
        val dialog = DialogSimples.newDialog(
            "Confirmar exclusão",
            "Tem certeza que deseja excluir este item?",
            posi,
            intArrayOf(android.R.string.ok, android.R.string.cancel)
        )
        dialog.openDialog(supportFragmentManager)
    }

    private fun deletarEntidade(posi: Int) {
        val entidadeDAO = EntidadeDAO(applicationContext)
        entidadeDAO.excluir(adapterSimples!!.itens[posi])
        adapterSimples?.remove(posi)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    override fun onClick(posi: Int, which: Int) {
        when (which) {
            -1 -> deletarEntidade(posi)
            -2 -> {}
        }
    }
}
