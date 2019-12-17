package com.paradoxo.amadeus.cognicao;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.activity.AprendizActivity;
import com.paradoxo.amadeus.activity.ListaAcaoActivity;
import com.paradoxo.amadeus.cognicao.faisca.Inicia;
import com.paradoxo.amadeus.dao.AcaoDAO;
import com.paradoxo.amadeus.dao.SentencaDAO;
import com.paradoxo.amadeus.enums.AcaoEnum;
import com.paradoxo.amadeus.enums.ItemEnum;
import com.paradoxo.amadeus.modelo.Acao;
import com.paradoxo.amadeus.modelo.Musica;
import com.paradoxo.amadeus.modelo.Sentenca;
import com.paradoxo.amadeus.util.Cronos;
import com.paradoxo.amadeus.util.GerenciaApp;
import com.paradoxo.amadeus.util.GerenciaMusica;
import com.paradoxo.amadeus.util.Permissao;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.paradoxo.amadeus.util.Preferencias.getPrefString;

public class Acionadora {

    Activity activity;
    GerenciaMusica gerenciaMusica;

    public static final String PREF_NOME_IA = "nomeIA";
    public static final String PREF_NOME_USU = "nomeUsu";

    public Acionadora(Activity activity) {
        this.activity = activity;
        gerenciaMusica = new GerenciaMusica(activity.getApplicationContext());
    }

    public void isAcao(String entrada) {
        Acao acaoAserTratada = new Acao(AcaoEnum.SEM_ACAO);

        AcaoDAO acaoDAO = new AcaoDAO(activity);
        List<Acao> acoesDisponiveis = acaoDAO.getAcoes();
        if (acoesDisponiveis == null) return;

        for (Acao acao : acoesDisponiveis) {
            for (String gatilho : acao.getGatilhos()) {
                if (entrada.contains(gatilho)) {
                    acaoAserTratada = acao;
                    acao.setGatilhos(Collections.singletonList(gatilho));
                    break;
                }
            }
            if (acaoAserTratada.getAcaoEnum() != AcaoEnum.SEM_ACAO) break;
        }

        tratarAcao(acaoAserTratada, entrada);
    }

    public void tratarAcao(Acao acaoAserTratada, String entrada) {
        switch (acaoAserTratada.getAcaoEnum()) {
            case SEM_ACAO: {
                acionarSentenciadora(entrada);
                break;
            }
            case MUSICA: {
                acionarMusica(acaoAserTratada, entrada);
                break;
            }

            case PARAR_MUSICA:
                acionarParadaMusica();
                break;

            case PROXIMA_MUSICA:
                acionarProximaMusica(entrada);
                break;

            case APP: {
                entrada = entrada.replace(acaoAserTratada.getGatilhos().get(0), "").trim();
                GerenciaApp.encontrarApp(entrada, activity);

                Log.e("Tag", "abrir App");
                break;
            }
            case SABER_HORA:
                notificarOutput(Cronos.getHora());
                break;
            case SABER_DATA:
                notificarOutput(Cronos.getData());
                break;

            case CUMPRIMENTAR:
                Inicia inicia = new Inicia(activity);
                inicia.cumprimentar();
                break;

            case DESPEDIR:
                Inicia inicia2 = new Inicia(activity);
                inicia2.despedir();
                break;

            case SAUDAR:
                Inicia inicia3 = new Inicia(activity);
                inicia3.gerarSaudacao(false);
                break;

            case STATUS:
                acionarSentenciadora("status");
                break;

            case TESTE:
                acionarSentenciadora("teste");
                break;

            case AGRADECER:
                acionarSentenciadora("obrigado");
                break;

            case PREVER_TEMPO:
                acionarSentenciadora("preveja o tempo");
                break;

            case APRESENTAR_IA:
                apresentarIA();
                break;

            case DIZER_NOME_IA:
                dizerNome(true);
                break;

            case DIZER_NOME_USU:
                dizerNome(false);
                break;

            case ACAO_CADASTRAR_RESPOSTA:
                break;

            case ACAO_ERRO_APP_NAO_EXISTE:
                GerenciaApp.buscarAppOnline(entrada.substring(0, entrada.indexOf(",")), activity);
                break;

            case ACAO_ACESSAR_MEMORIA:
                Permissao.solicitarAcessoArmazenamento(activity);
                break;

            case ABRIR_APRENDIZADO:
                activity.startActivity(new Intent(activity, AprendizActivity.class));
                notificarOutput(activity.getString(R.string.abrindo));
                break;

            case ABRIR_ACOES:
                activity.startActivity(new Intent(activity, ListaAcaoActivity.class));
                notificarOutput(activity.getString(R.string.abrindo));
                break;

            case AMADEUS_PLAY_STORE:
                abrirAmadeusPlayStore();
                break;
        }
    }

    private void abrirAmadeusPlayStore() {
        Intent intentPlayStore = new Intent(Intent.ACTION_VIEW);
        intentPlayStore.setData(Uri.parse(activity.getString(R.string.linkAmadeusGooglePlayStore)));
        activity.startActivity(intentPlayStore);
        notificarOutput(activity.getString(R.string.abrindo));
    }

    private void dizerNome(boolean nomeDaIA) {
        String chave_busca = nomeDaIA ? "diga seu nome" : "diga meu nome";

        SentencaDAO sentencaDAO = new SentencaDAO(activity, false);
        String nome = nomeDaIA ? getPrefString(PREF_NOME_IA, activity) : getPrefString(PREF_NOME_USU, activity);
        List<String> complementos = sentencaDAO.buscaPorChave(chave_busca).getRespostas();

        formatarRespostaAleatoria(complementos, nome);
    }

    private void apresentarIA() {
        List<String> apresentacoes = new ArrayList<>(Arrays.asList(activity.getResources().getStringArray(R.array.apresenta_ia)));
        String nome = getPrefString(PREF_NOME_IA, activity);

        formatarRespostaAleatoria(apresentacoes, nome);
    }

    private void formatarRespostaAleatoria(List<String> apresentcacoes, String nome) {
        Random random = new Random();

        String apresentacaoSelecionada = apresentcacoes.get(random.nextInt(apresentcacoes.size()));
        apresentacaoSelecionada = String.format(apresentacaoSelecionada, nome);
        notificarOutput(apresentacaoSelecionada);
    }

    private void acionarSentenciadora(String entrada) {
        Senteciadora senteciadora = new Senteciadora(activity);
        senteciadora.isSentenca(entrada);
    }

    private void acionarParadaMusica() {
        if (gerenciaMusica.musicaEstaTocando()) {
            gerenciaMusica.pararMusicaSeEstiverTocando();
            notificarOutput(activity.getString(R.string.musica_encerrada));
        } else {
            notificarOutput(activity.getString(R.string.nehuma_musica_em_reproducao));
        }
    }

    private void acionarProximaMusica(String entrada) {
        if (gerenciaMusica.musicaEstaTocando()) {

            Musica musica = gerenciaMusica.encontrarMusica(null);
            gerenciaMusica.configurarMediPlayer(musica);

        } else {
            if (entrada.contains("música") || entrada.contains("musica")) {
                notificarOutput(activity.getString(R.string.nehuma_musica_em_reproducao));
            } else {
                acionarSentenciadora(entrada);
            }

        }
    }

    private void acionarMusica(Acao acaoAserTratada, String entrada) {
        if (!Permissao.armazenamentoAcessivel(activity)) {

            Sentenca sentenca = new Sentenca(activity.getString(R.string.forneca_acesso_ao_armazenamento), AcaoEnum.ACAO_ACESSAR_MEMORIA);
            sentenca.setTipo_item(ItemEnum.ITEM_CARD.ordinal());
            sentenca.addResposta(activity.getString(R.string.acesso_a_memoria));

            EventBus.getDefault().post(sentenca);

            return;
        }

        Musica musica;
        boolean musicaAleatoria = acaoAserTratada.getGatilhos().get(0).equals(entrada);

        if (musicaAleatoria) {
            musica = gerenciaMusica.encontrarMusica(null);
        } else {
            entrada = entrada.replace(acaoAserTratada.getGatilhos().get(0), "").trim();
            musica = gerenciaMusica.encontrarMusica(entrada);
        }

        gerenciaMusica.configurarMediPlayer(musica);
    }

    private void notificarOutput(String mensagem) {
        Sentenca sentenca = new Sentenca(mensagem);
        EventBus.getDefault().post(sentenca);
    }
}
