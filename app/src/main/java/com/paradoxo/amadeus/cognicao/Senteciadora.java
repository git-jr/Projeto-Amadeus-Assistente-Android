package com.paradoxo.amadeus.cognicao;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.dao.EntidadeDAO;
import com.paradoxo.amadeus.dao.SentencaDAO;
import com.paradoxo.amadeus.enums.ItemEnum;
import com.paradoxo.amadeus.modelo.Entidade;
import com.paradoxo.amadeus.modelo.Sentenca;
import com.paradoxo.amadeus.util.Preferencias;
import com.paradoxo.amadeus.util.busca.ScanPage;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Senteciadora {
    static Random random;
    private Activity context;

    public static final String PREF_USAR_SINONIMOS_BUSCA = "usar_sinonimos_busca";

    public Senteciadora(Activity context) {
        this.context = context;
        random = new Random();
    }

    public void isSentenca(String entrada) {
        SentencaDAO sentencaDAO = new SentencaDAO(context, false);
        Sentenca sentenca = sentencaDAO.buscaPorChave(entrada);

        if (sentenca.getChave() != null) {
            notificarOutput(sentenca, context);
        } else if (Preferencias.getPrefBool(PREF_USAR_SINONIMOS_BUSCA, context, false)) {
            buscaPorSinonimos(entrada.toLowerCase(), context);
        } else {
            notificarOutputSemResposta(context);
        }
    }

    private static void reformularEntradaComSioninomos(List<Entidade> sinonimosDisponiveis, Activity context) {

        if (sinonimosDisponiveis.size() >= 2) {
            List<Entidade> analisar = sinonimosDisponiveis.subList(0, 2);

            List<String> sinonimos1 = new ArrayList<>(analisar.get(0).getSinonimos());
            if (analisar.get(0).getNome() != null)
                sinonimos1.add(analisar.get(0).getNome());

            List<String> sinonimos2 = new ArrayList<>(analisar.get(1).getSinonimos());
            if (analisar.get(1).getNome() != null)
                sinonimos2.add(analisar.get(1).getNome());

            List<String> geradas = new ArrayList<>();
            for (String s1 : sinonimos1) {
                for (String s2 : sinonimos2) {
                    //Log.e("-", s1 + " " + s2);
                    geradas.add(s1 + " " + s2);
                }
            }

            // Apesar do uso de apenas dois clicos "for" aqui poderiam ser 3, 4 ou mais, bastando moificiar apenas o "if" inicial,
            // mas essa abordagem facilita a depuração

            Entidade entidadeNova = new Entidade();
            entidadeNova.setSinonimos(geradas);
            List<Entidade> restante = new ArrayList<>();
            restante.add(entidadeNova);
            restante.addAll(sinonimosDisponiveis.subList(2, sinonimosDisponiveis.size()));

            reformularEntradaComSioninomos(restante, context);
        } else {

            SentencaDAO sentencaDAO = new SentencaDAO(context, false);
            List<Sentenca> listaSentenca = sentencaDAO.listar();


            for (String entrada : sinonimosDisponiveis.get(0).getSinonimos()) {
                Log.e("Testado ", entrada);

                for (Sentenca sentenca : listaSentenca) {

                    if (sentenca.getChave().equals(entrada)) {
                        notificarOutput(sentenca, context);
                        return;
                    }
                }

                /*  A abordagem a seguir pode ser mais lenta no omeç quando poucas sentença ainda estão cadastradaas,
                mas quanto podermos consiederar "poucas", para invalidar a abordagem mais rápida acima?

               Sentenca sentenca = sentencaDAO.buscaPorChave(entrada);


                if (sentenca.getChave() != null) {
                    notificarOutput(sentenca);
                    return;

                }
                */
            }

            notificarOutputSemResposta(context);
        }


    }

    private static void notificarOutput(Sentenca sentenca, Activity context) {
        if (sentenca.getTipo_item() == ItemEnum.USUARIO.ordinal())
            sentenca.setTipo_item(ItemEnum.IA.ordinal());

        int numeroRespostas = sentenca.getRespostas().size();
        if (numeroRespostas > 1) {
            sentenca.addResposta(sentenca.getRespostas().get(random.nextInt(numeroRespostas)), 0);
        }

        context.runOnUiThread(() -> EventBus.getDefault().post(sentenca));
    }

    private static void notificarOutputSemResposta(Activity context) {
        context.runOnUiThread(() -> {
            // Depois daqui vamos fazer uma análise da sentença em si, mas por hora vamos devolver algo para a tela inicial
            Sentenca sentenca = new Sentenca(getRespostaNaoEncontrada(context));
            EventBus.getDefault().post(sentenca);
        });

    }

    private static String getRespostaNaoEncontrada(Activity context) {
        Random random = new Random();
        List<String> naoEncontradas = Arrays.asList(context.getResources().getStringArray(R.array.resposta_nao_localizada));

        return naoEncontradas.get(random.nextInt(naoEncontradas.size()));
    }

    private static void buscaPorSinonimos(String entradaOriginal, Activity context) {
        List<String> chaves = new ArrayList<>(Arrays.asList(entradaOriginal.split(" ")));

        new AsyncTask<Void, Void, Sentenca>() {
            @Override
            protected Sentenca doInBackground(Void... voids) {
                EntidadeDAO entidadeDAO = new EntidadeDAO(context);
                List<Entidade> entidadesSinonimo = new ArrayList<>();
                int maxNumComb = 1; // Realizar testes até descobrir o número que torna inviável realiazr as combinações e aí usar essa variavel para controle

                for (String chave : chaves) {
                    Entidade entidade = entidadeDAO.buscaPorChave(chave);

                    if (entidade.getNome() == null) {
                        entidade = new Entidade();
                        entidade.setNome(chave);
                        try {
                            entidade.setSinonimos(ScanPage.obterSinonimo(chave));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            entidade.setSignificado(ScanPage.obterSignificado(chave));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (entidade.getSinonimos() != null) {
                            entidadeDAO.inserir(entidade);
                        }
                    }

                    if (entidade.getSinonimos() != null) {
                        entidadesSinonimo.add(entidade);
                        maxNumComb *= entidade.getSinonimos().size();
                    }
                }

                if (entidadesSinonimo.isEmpty()) {
                    notificarOutputSemResposta(context);
                } else {
                    reformularEntradaComSioninomos(entidadesSinonimo, context);
                }

                return null;

            }

        }.execute();
    }
}
