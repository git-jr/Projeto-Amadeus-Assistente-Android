package com.paradoxo.amadeus.cognicao

import android.app.Activity
import android.util.Log
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.dao.EntidadeDAO
import com.paradoxo.amadeus.dao.SentencaDAO
import com.paradoxo.amadeus.enums.ItemEnum
import com.paradoxo.amadeus.modelo.Entidade
import com.paradoxo.amadeus.modelo.Sentenca
import com.paradoxo.amadeus.util.Preferencias
import com.paradoxo.amadeus.util.busca.ScanPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.util.Random

class Senteciadora(private val context: Activity) {

    companion object {
        const val PREF_USAR_SINONIMOS_BUSCA = "usar_sinonimos_busca"
        private val random = Random()
    }

    fun isSentenca(entrada: String) {
        val sentencaDAO = SentencaDAO(context, false)
        val sentenca = sentencaDAO.buscaPorChave(entrada)

        when {
            sentenca.chave != null -> notificarOutput(sentenca)
            Preferencias.getPrefBool(PREF_USAR_SINONIMOS_BUSCA, context, false) ->
                buscaPorSinonimos(entrada.lowercase())
            else -> notificarOutputSemResposta()
        }
    }

    private fun buscaPorSinonimos(entradaOriginal: String) {
        val chaves = entradaOriginal.split(" ")

        CoroutineScope(Dispatchers.IO).launch {
            val entidadeDAO = EntidadeDAO(context)
            val entidadesSinonimo = mutableListOf<Entidade>()

            for (chave in chaves) {
                var entidade = entidadeDAO.buscaPorChave(chave)

                if (entidade.nome == null) {
                    entidade = Entidade().apply {
                        nome = chave
                        try { sinonimos = ScanPage.obterSinonimo(chave) } catch (e: Exception) { e.printStackTrace() }
                        try { significado = ScanPage.obterSignificado(chave) } catch (e: Exception) { e.printStackTrace() }
                    }
                    if (entidade.sinonimos != null) entidadeDAO.inserir(entidade)
                }

                if (entidade.sinonimos != null) entidadesSinonimo.add(entidade)
            }

            if (entidadesSinonimo.isEmpty()) {
                notificarOutputSemResposta()
            } else {
                reformularEntradaComSinonimos(entidadesSinonimo)
            }
        }
    }

    private fun reformularEntradaComSinonimos(sinonimosDisponiveis: List<Entidade>) {
        if (sinonimosDisponiveis.size >= 2) {
            val analisar = sinonimosDisponiveis.take(2)

            val sinonimos1 = (analisar[0].sinonimos ?: emptyList()).toMutableList()
                .also { if (analisar[0].nome != null) it.add(analisar[0].nome!!) }

            val sinonimos2 = (analisar[1].sinonimos ?: emptyList()).toMutableList()
                .also { if (analisar[1].nome != null) it.add(analisar[1].nome!!) }

            val geradas = sinonimos1.flatMap { s1 -> sinonimos2.map { s2 -> "$s1 $s2" } }

            // Apenas dois níveis de combinação são usados por enquanto; adicionar mais
            // significa ampliar o "if" inicial e mais iterações no flatMap acima.
            val entidadeNova = Entidade().apply { sinonimos = geradas }
            val restante = listOf(entidadeNova) + sinonimosDisponiveis.drop(2)

            reformularEntradaComSinonimos(restante)
        } else {
            val sentencaDAO = SentencaDAO(context, false)
            val listaSentenca = sentencaDAO.listar()

            for (entrada in sinonimosDisponiveis[0].sinonimos ?: emptyList()) {
                Log.e("Testado ", entrada)
                val encontrada = listaSentenca.firstOrNull { it.chave == entrada }
                if (encontrada != null) {
                    notificarOutput(encontrada)
                    return
                }
            }

            notificarOutputSemResposta()
        }
    }

    private fun notificarOutput(sentenca: Sentenca) {
        if (sentenca.tipo_item == ItemEnum.USUARIO.ordinal) {
            sentenca.tipo_item = ItemEnum.IA.ordinal
        }

        val numeroRespostas = sentenca.respostas.size
        if (numeroRespostas > 1) {
            sentenca.addResposta(sentenca.respostas[random.nextInt(numeroRespostas)], 0)
        }

        context.runOnUiThread { EventBus.getDefault().post(sentenca) }
    }

    private fun notificarOutputSemResposta() {
        context.runOnUiThread {
            EventBus.getDefault().post(Sentenca(getRespostaNaoEncontrada()))
        }
    }

    private fun getRespostaNaoEncontrada(): String {
        val opcoes = context.resources.getStringArray(R.array.resposta_nao_localizada).toList()
        return opcoes[Random().nextInt(opcoes.size)]
    }
}
