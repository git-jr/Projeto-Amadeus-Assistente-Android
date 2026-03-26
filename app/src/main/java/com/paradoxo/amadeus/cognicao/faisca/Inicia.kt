package com.paradoxo.amadeus.cognicao.faisca

import android.content.Context
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.modelo.Sentenca
import com.paradoxo.amadeus.util.Preferencias
import org.greenrobot.eventbus.EventBus
import org.joda.time.DateTime
import org.joda.time.Duration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Random

class Inicia(private val context: Context) {
    companion object {
        const val PREF_NOME_USU = "nomeUsu"
        const val PREF_ULTIMA_VEZ_ABERTO = "ultimaVezAberto"
    }

    fun despertar() {
        val qtdHorasDesdeUltima = quantasHorasAtrasFoiAberto()
        if (qtdHorasDesdeUltima < 7 && qtdHorasDesdeUltima != -1) {
            val random = Random()
            val tempoMinimoNovaSaudacao = 3
            val horaAleatoria = random.nextInt(tempoMinimoNovaSaudacao)
            if (horaAleatoria < tempoMinimoNovaSaudacao && horaAleatoria == qtdHorasDesdeUltima) {
                gerarSaudacao(false)
            } else {
                gerarSaudacao(true)
            }
        } else {
            gerarSaudacao(false)
        }
    }

    fun gerarSaudacao(saudacaoPadrao: Boolean) {
        Preferencias.setPrefLong(PREF_ULTIMA_VEZ_ABERTO, System.currentTimeMillis(), context)

        val saudacaoSelecionada: String
        if (saudacaoPadrao) {
            val saudacao = context.resources.getStringArray(R.array.saudacoes_secundarias).toList()
            saudacaoSelecionada = saudacao[Random().nextInt(saudacao.size)]
        } else {
            val saudacao = context.resources.getStringArray(R.array.saudacoes_primarias).toList()
            val horaAtual = SimpleDateFormat("HH", Locale.getDefault()).format(Calendar.getInstance().time).toInt()
            saudacaoSelecionada = when {
                horaAtual >= 18 -> saudacao[1]
                horaAtual >= 12 -> saudacao[2]
                horaAtual <= 4  -> saudacao[3]
                else            -> saudacao[0]
            }
        }

        notificarOutput("$saudacaoSelecionada ${Preferencias.getPrefString(PREF_NOME_USU, context)}")
    }

    private fun quantasHorasAtrasFoiAberto(): Int {
        var quantoTempoFaz = -1
        val ultimaVezAberto = Preferencias.getPrefLong(PREF_ULTIMA_VEZ_ABERTO, context)
        if (ultimaVezAberto != 0L) {
            quantoTempoFaz = Duration(DateTime(ultimaVezAberto), DateTime(System.currentTimeMillis()))
                .standardHours.toInt()
        }
        Preferencias.setPrefLong(PREF_ULTIMA_VEZ_ABERTO, System.currentTimeMillis(), context)
        return quantoTempoFaz
    }

    fun cumprimentar() {
        formatarRespostaArray(context.resources.getStringArray(R.array.cumprimentos).toMutableList())
    }

    fun despedir() {
        formatarRespostaArray(context.resources.getStringArray(R.array.despedidas).toMutableList())
    }

    private fun formatarRespostaArray(respostas: MutableList<String>) {
        val random = Random()
        val usarNomeNaResposta = random.nextBoolean()
        var respostaSelecionada = respostas[random.nextInt(respostas.size)]

        if (usarNomeNaResposta) {
            val ehPergunta = respostaSelecionada.contains("?")
            if (ehPergunta) respostaSelecionada = respostaSelecionada.replace("?", "").trim()
            respostaSelecionada += " ${Preferencias.getPrefString(PREF_NOME_USU, context)}"
            if (ehPergunta) respostaSelecionada += " ?"
        }

        notificarOutput(respostaSelecionada)
    }

    private fun notificarOutput(mensagem: String) {
        EventBus.getDefault().post(Sentenca(mensagem))
    }
}
