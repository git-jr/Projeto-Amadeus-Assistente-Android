package com.paradoxo.amadeus.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.dao.MensagemDAO;
import com.paradoxo.amadeus.enums.AcaoEnum;
import com.paradoxo.amadeus.modelo.Autor;
import com.paradoxo.amadeus.modelo.Mensagem;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

public class Chatbot {

    private Context context;
    private List<Autor> autores;
    private static final String NENHUM = "NENHUM";
    private List<String> semRespostas = new ArrayList<>();
    private static final int SEM_ACAO = 0, ACAO_ABRIR_APP = 1, ACAO_TOCAR_MUSICA = 2, ACAO_APP_NAO_ENCONTRADO = 3;

    public Chatbot(Context context, List<Autor> autores) {
        this.context = context;
        this.autores = autores;
        semRespostas.add((String) this.context.getText(R.string.resposta_nao_localizada));
    }

    public Mensagem gerarRespoosta(String entrada) {
        Mensagem mensagem = new Mensagem();
        Autor autor = new Autor(1, "IA");
        mensagem.setAutor(autor);

        int acao = indentificarAcao(entrada);

        if (acao != SEM_ACAO) {
            Log.e("Ação", "Identificada");

            switch (acao) {
                case ACAO_ABRIR_APP: {
                    mensagem.setConteudo(context.getString(R.string.abrindo_app));
                    abrirApp(qualAppEstaNestaEntrada(entrada));
                    break;
                }
                case ACAO_APP_NAO_ENCONTRADO: {
                    mensagem.setConteudo(context.getString(R.string.voce_nao_tem_esse_app));
                    mensagem.setAcao(AcaoEnum.ACAO_APP_NAO_ENCONTRADO);
                    break;
                }
                case ACAO_TOCAR_MUSICA: {
                    mensagem.setConteudo(context.getString(R.string.funcao_disponivel_em_breve));
                    break;
                }
            }

        } else {
            Log.e("Ação", "Não Identificada");

            configurarRespostaNaoEncontrada(mensagem, autor);
            avisarSobreEnsinoPorClique(mensagem);

            MensagemDAO msgDAO = new MensagemDAO(context);
            Mensagem mensagemBusca = new Mensagem();
            mensagemBusca.setConteudo(entrada);
            mensagemBusca = msgDAO.buscaPorConteudo(mensagemBusca, false);

            if (mensagemBusca.temResposta()) {
                mensagemBusca = msgDAO.buscaPorID(mensagemBusca);
                mensagemBusca.setAutor(autor);
                // Garante que a resposta será exibida pela IA

                return mensagemBusca;
            }
        }

        return mensagem;
    }

    private int indentificarAcao(String entrada) {
        int tipoDeAcao = SEM_ACAO;

        List<String> comandosAbrirApp = Arrays.asList(context.getResources().getStringArray(R.array.intencoes_abrir_app));
        List<String> comandosTocarMusica = Arrays.asList(context.getResources().getStringArray(R.array.intencoes_tocar_musica));

        List<List<String>> listaDasListasDeComandos = new ArrayList<>();
        listaDasListasDeComandos.add(ACAO_ABRIR_APP - 1, comandosAbrirApp);
        // Aproveitando as constantes para apotar para os indices e poupar switch/case dentro do loop for à seguir
        listaDasListasDeComandos.add(ACAO_TOCAR_MUSICA - 1, comandosTocarMusica);

        for (List<String> listaDeComandos : listaDasListasDeComandos) {
            for (String comandoEmSi : listaDeComandos) {
                if (entrada.contains(comandoEmSi) && !entrada.replace(comandoEmSi, "").isEmpty()) {
                    tipoDeAcao = listaDasListasDeComandos.indexOf(listaDeComandos) + 1;
                    // Retorna qual o número que aponta para o tipo de ação esperada

                    switch (tipoDeAcao) {
                        case ACAO_ABRIR_APP: {
                            if (!qualAppEstaNestaEntrada(entrada).equals(NENHUM)) {
                                break;
                            } else {
                                tipoDeAcao = ACAO_APP_NAO_ENCONTRADO;
                            }
                        }
                        case ACAO_TOCAR_MUSICA: {
                            // Implementar
                        }
                    }
                    if (tipoDeAcao != SEM_ACAO)
                        break;
                }
            }
        }
        return tipoDeAcao;
    }

    private String qualAppEstaNestaEntrada(String entrada) {
        for (String nomeDoApp : listarNomeAppsInstalados()) {
            if (entrada.contains(nomeDoApp)) {
                return nomeDoApp;
            }
        }
        return NENHUM;
    }

    private List<String> listarNomeAppsInstalados() {
        final PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> appsInstalados = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> nomeDosApps = new ArrayList<>();

        for (ApplicationInfo packageInfo : appsInstalados) {
            String nomeApp = packageInfo.loadLabel(packageManager).toString();
            nomeDosApps.add(nomeApp.toLowerCase());
        }
        return nomeDosApps;
    }

    private void abrirApp(String app) {
        final PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> appsInstalados = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : appsInstalados) {
            String nomeApp = packageInfo.loadLabel(packageManager).toString().toLowerCase();
            try {
                if (nomeApp.equals(app)) {
                    Intent intentAppParaAbrir = packageManager.getLaunchIntentForPackage(packageInfo.packageName);
                    context.startActivity(intentAppParaAbrir);
                }

            } catch (Exception e) {
                Log.e(TAG, "ERRO" + "Ao listar o app");
            }
        }
    }

    private void configurarRespostaNaoEncontrada(Mensagem mensagem, Autor autor) {
        Random random = new Random();
        random.nextInt(semRespostas.size());

        mensagem.setConteudo(semRespostas.get(random.nextInt(semRespostas.size())));
        mensagem.setAutor(autor);
    }

    private void avisarSobreEnsinoPorClique(Mensagem mensagem) {
        String nomeSharedeEnsinar = "avisadoClickEnsinas";

        if (!getPrefBool(nomeSharedeEnsinar)) {
            mensagem.setConteudo(mensagem.getConteudo() + context.getString(R.string.clique_para_ensinar));
            setPrefBool(nomeSharedeEnsinar);
        }
    }

    public void despertar() {
        int qtdHorasDesdeUltima = quantasHorasAtrasFoiAberto();
        if (qtdHorasDesdeUltima < 7 && qtdHorasDesdeUltima != -1) {
            // Se ainda faz pouco tempo (menos de 7 horas nesse caso) desde a última abetura do app
            // O algoritimo abaixo irá de forma aleatória escolher se irá ou não fazer uma saudação para usuário e como ela se parecerá
            // Exemploes de saudação:  "olá de novo", "bem vindo de volta", "bom dia"

            Random random = new Random();
            int tempoMinimoNoavaSaudacao = 3;
            int horaAleatoria = random.nextInt(tempoMinimoNoavaSaudacao);

            if (horaAleatoria < tempoMinimoNoavaSaudacao && horaAleatoria == qtdHorasDesdeUltima) {
                // Se o tempo minímo já passou e se a hora de agora foi escolhida no aletaorio, então saúde!
                gerarSaudacao(false);
            } else {
                gerarSaudacao(true);
            }

        } else {
            gerarSaudacao(false);
            // Nessa situação já se passou tempo suficinte para que a IA faça uma nova saudação sem ser incoviniente
        }
    }

    private void gerarSaudacao(boolean saudacaoPadrao) {
        setPrefLong(System.currentTimeMillis());

        String saudacaoSelecionada;
        List<String> saudacao = new ArrayList<>();

        if (saudacaoPadrao) {
            saudacao.addAll(Arrays.asList(context.getResources().getStringArray(R.array.saudacoes_secundarias)));

            Random random = new Random();
            saudacaoSelecionada = saudacao.get(random.nextInt(saudacao.size()));

        } else {
            saudacao.addAll(Arrays.asList(context.getResources().getStringArray(R.array.saudacoes_primarias)));

            SimpleDateFormat dateFormat_hora = new SimpleDateFormat("HH", Locale.getDefault());
            Date data = new Date();

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(data);
            Date data_atual = calendar.getTime();

            Integer horaAtual = Integer.valueOf(dateFormat_hora.format(data_atual));

            if (horaAtual >= 18) {
                saudacaoSelecionada = saudacao.get(1);
            } else {
                if (horaAtual >= 12) {
                    saudacaoSelecionada = saudacao.get(2);
                } else {
                    if (horaAtual <= 4) {
                        saudacaoSelecionada = saudacao.get(3);
                    } else {
                        saudacaoSelecionada = saudacao.get(0);
                    }
                }
            }
        }

        String saudaGearada = saudacaoSelecionada + " " + autores.get(1).getNome();
        MensagemDAO objMsgDAO = new MensagemDAO(context);
        objMsgDAO.inserirMensagem(new Mensagem(saudaGearada, autores.get(0)));
    }

    private int quantasHorasAtrasFoiAberto() {
        int quantoTempoFaz = -1;
        long ultimaVezAberto = getPrefLong();
        if (ultimaVezAberto != 0) {
            // Não é a primeira vez aberto que o app está sendo aberto

            JodaTimeAndroid.init(context);
            DateTime dataUltimaVezAberto = new DateTime(ultimaVezAberto);
            DateTime dataAgora = new DateTime(System.currentTimeMillis());

            Duration duration = new Duration(dataUltimaVezAberto, dataAgora);
            quantoTempoFaz = (int) duration.getStandardHours();
        }
        setPrefLong(System.currentTimeMillis());
        return quantoTempoFaz;
    }

    private void setPrefLong(long texto) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putLong("ultimaVezAberto", texto);
        mEditor.apply();
    }

    private long getPrefLong() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        return sharedPreferences.getLong("ultimaVezAberto", 0);
    }

    private void setPrefBool(String nomeSh) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putBoolean(nomeSh, true);
        mEditor.apply();
    }

    private boolean getPrefBool(String nomeSh) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        return sharedPreferences.getBoolean(nomeSh, false);
    }

}