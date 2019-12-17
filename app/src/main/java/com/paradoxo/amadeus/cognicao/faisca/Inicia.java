package com.paradoxo.amadeus.cognicao.faisca;

import android.content.Context;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.modelo.Sentenca;

import org.greenrobot.eventbus.EventBus;
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

import static com.paradoxo.amadeus.util.Preferencias.getPrefLong;
import static com.paradoxo.amadeus.util.Preferencias.getPrefString;
import static com.paradoxo.amadeus.util.Preferencias.setPrefLong;

public class Inicia {
    private Context context;

    public static final String PREF_NOME_USU = "nomeUsu";
    public static final String PREF_ULTIMA_VEZ_ABERTO = "ultimaVezAberto";

    public Inicia(Context context) {
        this.context = context;
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

    public void gerarSaudacao(boolean saudacaoPadrao) {
        setPrefLong(PREF_ULTIMA_VEZ_ABERTO, System.currentTimeMillis(), context);

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

        String saudacaoGearada = saudacaoSelecionada + " " + getPrefString(PREF_NOME_USU, context);
        notificarOutput(saudacaoGearada);
    }

    private int quantasHorasAtrasFoiAberto() {
        int quantoTempoFaz = -1;
        long ultimaVezAberto = getPrefLong(PREF_ULTIMA_VEZ_ABERTO, context);
        if (ultimaVezAberto != 0) {
            DateTime dataUltimaVezAberto = new DateTime(ultimaVezAberto);
            DateTime dataAgora = new DateTime(System.currentTimeMillis());

            Duration duration = new Duration(dataUltimaVezAberto, dataAgora);
            quantoTempoFaz = (int) duration.getStandardHours();
        }
        setPrefLong(PREF_ULTIMA_VEZ_ABERTO, System.currentTimeMillis(), context);
        return quantoTempoFaz;
    }

    public void cumprimentar() {
        List<String> cumprimentos = new ArrayList<>(Arrays.asList(context.getResources().getStringArray(R.array.cumprimentos)));

        formatarRespostaArray(cumprimentos);
    }

    public void despedir() {
        List<String> cumprimentos = new ArrayList<>(Arrays.asList(context.getResources().getStringArray(R.array.despedidas)));

        formatarRespostaArray(cumprimentos);
    }

    private void formatarRespostaArray(List<String> respostas) {
        Random random = new Random();
        boolean usarNomeNaresposta = random.nextBoolean();

        String respostaSelecionada = respostas.get(random.nextInt(respostas.size()));

        if (usarNomeNaresposta) {
            boolean ehPergunta = respostaSelecionada.contains("?");
            if (ehPergunta) {
                respostaSelecionada = respostaSelecionada.replace("?", "");
                respostaSelecionada = respostaSelecionada.trim();
            }

            respostaSelecionada += " " + getPrefString(PREF_NOME_USU, context);

            if (ehPergunta) {
                respostaSelecionada += " ?";
            }
        }

        notificarOutput(respostaSelecionada);
    }

    private void notificarOutput(String mensagem) {
        Sentenca sentenca = new Sentenca(mensagem);
        EventBus.getDefault().post(sentenca);
    }
}
