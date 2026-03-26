package com.paradoxo.amadeus.activity

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.dao.AcaoDAO
import com.paradoxo.amadeus.dao.EntidadeDAO
import com.paradoxo.amadeus.dao.SentencaDAO
import com.paradoxo.amadeus.util.Preferencias.getPrefBool
import com.paradoxo.amadeus.util.Preferencias.setPrefBool
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca

class AprendizActivity : AppCompatActivity() {

    companion object {
        private const val LISTA_SENTENCA = 0
        private const val LISTA_ENTIDADE = 1
        private const val LISTA_ACAO = 2

        const val PREF_USAR_SINONIMOS_BUSCA = "usar_sinonimos_busca"

        @Suppress("DEPRECATION")
        private fun carregarInfosBancoAtual(context: Activity) {
            object : AsyncTask<Void?, Void?, List<Long>>() {
                override fun doInBackground(vararg voids: Void?): List<Long> {
                    val sentencaDAO = SentencaDAO(context, false)
                    val entidadeDAO = EntidadeDAO(context)
                    val acaoDAO = AcaoDAO(context)
                    return listOf(
                        sentencaDAO.quantidadeTotal,
                        entidadeDAO.quantidadeTotal,
                        acaoDAO.quantidadeTotal
                    )
                }

                override fun onPostExecute(valores: List<Long>) {
                    super.onPostExecute(valores)
                    (context.findViewById<TextView>(R.id.qtdSentencasTextView)).text = valores[0].toString()
                    (context.findViewById<TextView>(R.id.qtdEntidadesTextView)).text = valores[1].toString()
                    (context.findViewById<TextView>(R.id.qtdAcoesTextView)).text = valores[2].toString()
                }
            }.execute()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aprendiz)
        configurarInterface()
        carregarInfosBancoAtual(this)
    }

    private fun configurarInterface() {
        configurarToolBarBranca(this)
        configurarChips()
        configurarToggleButton()
    }

    private fun configurarChips() {
        (findViewById<ChipGroup>(R.id.rootchipGroup)).setOnCheckedChangeListener { _, checkedId ->
            val chipSelecionado = findViewById<Chip>(checkedId)
            if (chipSelecionado != null) {
                val tipoLista = chipSelecionado.tag.toString().toInt()
                val intent = when (tipoLista) {
                    LISTA_SENTENCA -> Intent(applicationContext, ListaSentencaActivity::class.java)
                    LISTA_ENTIDADE -> Intent(applicationContext, ListaEntidadeActivity::class.java)
                    LISTA_ACAO -> Intent(applicationContext, ListaAcaoActivity::class.java)
                    else -> null
                }
                intent?.let { startActivity(it) }
                Handler().postDelayed({ chipSelecionado.isChecked = false }, 1000)
            }
        }
    }

    private fun configurarToggleButton() {
        val usoSinonimosToggleButton = findViewById<ToggleButton>(R.id.usoSinonimosToggleButton)
        usoSinonimosToggleButton.isChecked = getPrefBool(PREF_USAR_SINONIMOS_BUSCA, this, false)
        usoSinonimosToggleButton.setOnCheckedChangeListener { _, valor ->
            setPrefBool(PREF_USAR_SINONIMOS_BUSCA, valor, applicationContext)
        }
    }
}
