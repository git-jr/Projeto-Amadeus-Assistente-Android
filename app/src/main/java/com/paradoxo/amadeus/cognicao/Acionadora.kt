package com.paradoxo.amadeus.cognicao

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.activity.AprendizActivity
import com.paradoxo.amadeus.activity.ListaAcaoActivity
import com.paradoxo.amadeus.cognicao.faisca.Inicia
import com.paradoxo.amadeus.dao.AcaoDAO
import com.paradoxo.amadeus.dao.SentencaDAO
import com.paradoxo.amadeus.enums.AcaoEnum
import com.paradoxo.amadeus.enums.ItemEnum
import com.paradoxo.amadeus.modelo.Acao
import com.paradoxo.amadeus.modelo.Sentenca
import com.paradoxo.amadeus.util.Cronos
import com.paradoxo.amadeus.util.GerenciaApp
import com.paradoxo.amadeus.util.GerenciaMusica
import com.paradoxo.amadeus.util.Permissao
import com.paradoxo.amadeus.util.Preferencias.getPrefString
import org.greenrobot.eventbus.EventBus
import java.util.Collections
import java.util.Random

class Acionadora(private val activity: Activity) {

    private val gerenciaMusica = GerenciaMusica(activity.applicationContext)

    companion object {
        const val PREF_NOME_IA = "nomeIA"
        const val PREF_NOME_USU = "nomeUsu"
    }

    fun isAcao(entrada: String) {
        var acaoAserTratada = Acao(AcaoEnum.SEM_ACAO)

        val acaoDAO = AcaoDAO(activity)
        val acoesDisponiveis = acaoDAO.acoes ?: return

        for (acao in acoesDisponiveis) {
            for (gatilho in acao.gatilhos) {
                if (entrada.contains(gatilho)) {
                    acaoAserTratada = acao
                    acao.gatilhos = Collections.singletonList(gatilho)
                    break
                }
            }
            if (acaoAserTratada.acaoEnum != AcaoEnum.SEM_ACAO) break
        }

        tratarAcao(acaoAserTratada, entrada)
    }

    fun tratarAcao(acaoAserTratada: Acao, entrada: String) {
        var entradaMutavel = entrada
        when (acaoAserTratada.acaoEnum) {
            AcaoEnum.SEM_ACAO -> acionarSentenciadora(entradaMutavel)

            AcaoEnum.MUSICA -> acionarMusica(acaoAserTratada, entradaMutavel)

            AcaoEnum.PARAR_MUSICA -> acionarParadaMusica()

            AcaoEnum.PROXIMA_MUSICA -> acionarProximaMusica(entradaMutavel)

            AcaoEnum.APP -> {
                entradaMutavel = entradaMutavel.replace(acaoAserTratada.gatilhos[0], "").trim()
                GerenciaApp.encontrarApp(entradaMutavel, activity)
                Log.e("Tag", "abrir App")
            }

            AcaoEnum.SABER_HORA -> notificarOutput(Cronos.getHora())

            AcaoEnum.SABER_DATA -> notificarOutput(Cronos.getData())

            AcaoEnum.CUMPRIMENTAR -> Inicia(activity).cumprimentar()

            AcaoEnum.DESPEDIR -> Inicia(activity).despedir()

            AcaoEnum.SAUDAR -> Inicia(activity).gerarSaudacao(false)

            AcaoEnum.STATUS -> acionarSentenciadora("status")

            AcaoEnum.TESTE -> acionarSentenciadora("teste")

            AcaoEnum.AGRADECER -> acionarSentenciadora("obrigado")

            AcaoEnum.PREVER_TEMPO -> acionarSentenciadora("preveja o tempo")

            AcaoEnum.APRESENTAR_IA -> apresentarIA()

            AcaoEnum.DIZER_NOME_IA -> dizerNome(true)

            AcaoEnum.DIZER_NOME_USU -> dizerNome(false)

            AcaoEnum.ACAO_CADASTRAR_RESPOSTA -> { }

            AcaoEnum.ACAO_ERRO_APP_NAO_EXISTE ->
                GerenciaApp.buscarAppOnline(entradaMutavel.substring(0, entradaMutavel.indexOf(",")), activity)

            AcaoEnum.ACAO_ACESSAR_MEMORIA ->
                Permissao.solicitarAcessoArmazenamento(activity)

            AcaoEnum.ABRIR_APRENDIZADO -> {
                activity.startActivity(Intent(activity, AprendizActivity::class.java))
                notificarOutput(activity.getString(R.string.abrindo))
            }

            AcaoEnum.ABRIR_ACOES -> {
                activity.startActivity(Intent(activity, ListaAcaoActivity::class.java))
                notificarOutput(activity.getString(R.string.abrindo))
            }

            AcaoEnum.AMADEUS_PLAY_STORE -> abrirAmadeusPlayStore()

            else -> { }
        }
    }

    private fun abrirAmadeusPlayStore() {
        val intentPlayStore = Intent(Intent.ACTION_VIEW)
        intentPlayStore.data = Uri.parse(activity.getString(R.string.linkAmadeusGooglePlayStore))
        activity.startActivity(intentPlayStore)
        notificarOutput(activity.getString(R.string.abrindo))
    }

    private fun dizerNome(nomeDaIA: Boolean) {
        val chaveBusca = if (nomeDaIA) "diga seu nome" else "diga meu nome"
        val sentencaDAO = SentencaDAO(activity, false)
        val nome = if (nomeDaIA) getPrefString(PREF_NOME_IA, activity) else getPrefString(PREF_NOME_USU, activity)
        val complementos = sentencaDAO.buscaPorChave(chaveBusca).respostas
        formatarRespostaAleatoria(complementos, nome)
    }

    private fun apresentarIA() {
        val apresentacoes = activity.resources.getStringArray(R.array.apresenta_ia).toMutableList()
        val nome = getPrefString(PREF_NOME_IA, activity)
        formatarRespostaAleatoria(apresentacoes, nome)
    }

    private fun formatarRespostaAleatoria(apresentacoes: List<String>, nome: String) {
        val random = Random()
        var apresentacaoSelecionada = apresentacoes[random.nextInt(apresentacoes.size)]
        apresentacaoSelecionada = String.format(apresentacaoSelecionada, nome)
        notificarOutput(apresentacaoSelecionada)
    }

    private fun acionarSentenciadora(entrada: String) {
        val senteciadora = Senteciadora(activity)
        senteciadora.isSentenca(entrada)
    }

    private fun acionarParadaMusica() {
        if (gerenciaMusica.musicaEstaTocando()) {
            gerenciaMusica.pararMusicaSeEstiverTocando()
            notificarOutput(activity.getString(R.string.musica_encerrada))
        } else {
            notificarOutput(activity.getString(R.string.nehuma_musica_em_reproducao))
        }
    }

    private fun acionarProximaMusica(entrada: String) {
        if (gerenciaMusica.musicaEstaTocando()) {
            val musica = gerenciaMusica.encontrarMusica(null)
            gerenciaMusica.configurarMediPlayer(musica)
        } else {
            if (entrada.contains("música") || entrada.contains("musica")) {
                notificarOutput(activity.getString(R.string.nehuma_musica_em_reproducao))
            } else {
                acionarSentenciadora(entrada)
            }
        }
    }

    private fun acionarMusica(acaoAserTratada: Acao, entrada: String) {
        var entradaMutavel = entrada
        if (!Permissao.armazenamentoAcessivel(activity)) {
            val sentenca = Sentenca(activity.getString(R.string.forneca_acesso_ao_armazenamento), AcaoEnum.ACAO_ACESSAR_MEMORIA)
            sentenca.tipo_item = ItemEnum.ITEM_CARD.ordinal
            sentenca.addResposta(activity.getString(R.string.acesso_a_memoria))
            EventBus.getDefault().post(sentenca)
            return
        }

        val musicaAleatoria = acaoAserTratada.gatilhos[0] == entradaMutavel
        val musica = if (musicaAleatoria) {
            gerenciaMusica.encontrarMusica(null)
        } else {
            entradaMutavel = entradaMutavel.replace(acaoAserTratada.gatilhos[0], "").trim()
            gerenciaMusica.encontrarMusica(entradaMutavel)
        }

        gerenciaMusica.configurarMediPlayer(musica)
    }

    private fun notificarOutput(mensagem: String) {
        val sentenca = Sentenca(mensagem)
        EventBus.getDefault().post(sentenca)
    }
}
