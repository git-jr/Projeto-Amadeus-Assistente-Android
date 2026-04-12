package com.paradoxo.amadeus.cognicao

import android.app.Activity
import android.util.Log
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.toEntity
import com.paradoxo.amadeus.dao.room.toModel
import com.paradoxo.amadeus.enums.AcaoEnum
import com.paradoxo.amadeus.enums.ItemEnum
import com.paradoxo.amadeus.ia.GroqAssistant
import com.paradoxo.amadeus.modelo.Entidade
import com.paradoxo.amadeus.modelo.Sentenca
import com.paradoxo.amadeus.util.Preferencias
import com.paradoxo.amadeus.util.busca.ScanPage
import java.util.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class Senteciadora(private val context: Activity) {

    companion object {
        const val PREF_USAR_SINONIMOS_BUSCA = "usar_sinonimos_busca"
        private val random = Random()
        private const val MENSAGEM_FALHA_GROQ =
            "Nao consegui falar com a Groq agora. Verifique sua chave, conexao ou tente de novo."
    }

    private val groqAssistant = GroqAssistant(context.applicationContext)

    fun isSentenca(entrada: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val entradaNormalizada = entrada.trim()
            val sentencaLocal = buscarSentencaLocal(entradaNormalizada)

            when {
                sentencaLocal != null -> notificarOutput(sentencaLocal)

                Preferencias.getPrefBool(PREF_USAR_SINONIMOS_BUSCA, context, false) -> {
                    val sentencaPorSinonimo = buscaPorSinonimos(entradaNormalizada.lowercase())
                    if (sentencaPorSinonimo != null) {
                        notificarOutput(sentencaPorSinonimo)
                    } else {
                        responderComGroqOuConfiguracao(entradaNormalizada)
                    }
                }

                else -> responderComGroqOuConfiguracao(entradaNormalizada)
            }
        }
    }

    private suspend fun buscarSentencaLocal(entrada: String): Sentenca? {
        val db = AmadeusDatabase.getInstance(context)
        val chaves = listOf(
            entrada.trim(),
            entrada.lowercase().trim(),
            entrada.lowercase().replace("?", "").replace("!", "").trim()
        )
            .filter { it.isNotBlank() }
            .distinct()

        for (chave in chaves) {
            val encontrada = db.sentencaDAO().buscaPorChave(chave)?.toModel()
            if (encontrada != null) return encontrada
        }

        return null
    }

    private suspend fun responderComGroqOuConfiguracao(entrada: String) {
        if (!GroqAssistant.isConfigured(context)) {
            notificarConfiguracaoGroqPendente()
            return
        }

        val resposta = runCatching { groqAssistant.responder(entrada) }
            .getOrElse {
                Log.e("Groq", "Falha ao consultar a Groq", it)
                MENSAGEM_FALHA_GROQ
            }

        notificarTexto(resposta)
    }

    private suspend fun buscaPorSinonimos(entradaOriginal: String): Sentenca? {
        val chaves = entradaOriginal.split(" ")
        val db = AmadeusDatabase.getInstance(context)
        val entidadesSinonimo = mutableListOf<Entidade>()

        for (chave in chaves) {
            var entidade = db.entidadeDAO().buscaPorChave(chave)?.toModel()

            if (entidade == null) {
                entidade = Entidade().apply {
                    nome = chave
                    try {
                        sinonimos = ScanPage.obterSinonimo(chave)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    try {
                        significado = ScanPage.obterSignificado(chave)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (entidade.sinonimos != null) db.entidadeDAO().inserir(entidade.toEntity())
            }

            if (entidade.sinonimos != null) entidadesSinonimo.add(entidade)
        }

        return if (entidadesSinonimo.isEmpty()) {
            null
        } else {
            reformularEntradaComSinonimos(entidadesSinonimo)
        }
    }

    private suspend fun reformularEntradaComSinonimos(sinonimosDisponiveis: List<Entidade>): Sentenca? {
        if (sinonimosDisponiveis.size >= 2) {
            val analisar = sinonimosDisponiveis.take(2)

            val sinonimos1 = (analisar[0].sinonimos ?: emptyList()).toMutableList()
                .also { if (analisar[0].nome != null) it.add(analisar[0].nome!!) }

            val sinonimos2 = (analisar[1].sinonimos ?: emptyList()).toMutableList()
                .also { if (analisar[1].nome != null) it.add(analisar[1].nome!!) }

            val geradas = sinonimos1.flatMap { s1 -> sinonimos2.map { s2 -> "$s1 $s2" } }

            val entidadeNova = Entidade().apply { sinonimos = geradas }
            val restante = listOf(entidadeNova) + sinonimosDisponiveis.drop(2)

            return reformularEntradaComSinonimos(restante)
        }

        val listaSentenca = AmadeusDatabase.getInstance(context)
            .sentencaDAO()
            .listar()
            .map { it.toModel() }

        for (entrada in sinonimosDisponiveis[0].sinonimos ?: emptyList()) {
            Log.e("Testado", entrada)
            val encontrada = listaSentenca.firstOrNull { it.chave == entrada }
            if (encontrada != null) {
                return encontrada
            }
        }

        return null
    }

    private fun notificarConfiguracaoGroqPendente() {
        val sentenca = Sentenca(
            GroqAssistant.MENSAGEM_SEM_CONFIGURACAO,
            AcaoEnum.ABRIR_CONFIG_IA
        )
        sentenca.tipo_item = ItemEnum.ITEM_CARD.ordinal
        sentenca.addResposta(context.getString(R.string.abrir_configuracoes_ia))

        context.runOnUiThread {
            EventBus.getDefault().post(sentenca)
        }
    }

    private fun notificarTexto(texto: String) {
        context.runOnUiThread {
            EventBus.getDefault().post(Sentenca(texto))
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

        context.runOnUiThread {
            EventBus.getDefault().post(sentenca)
        }
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
