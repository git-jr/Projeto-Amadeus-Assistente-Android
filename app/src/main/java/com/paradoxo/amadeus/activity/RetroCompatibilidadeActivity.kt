package com.paradoxo.amadeus.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.dao.AutorDAO;
import com.paradoxo.amadeus.dao.MensagemDAO;
import com.paradoxo.amadeus.dao.SentencaDAO;
import com.paradoxo.amadeus.enums.AcaoEnum;
import com.paradoxo.amadeus.enums.ItemEnum;
import com.paradoxo.amadeus.modelo.Mensagem;
import com.paradoxo.amadeus.modelo.Sentenca;
import com.paradoxo.amadeus.util.Animacoes;

import java.util.ArrayList;
import java.util.List;

import static com.paradoxo.amadeus.util.Toasts.meuToast;
import static com.paradoxo.amadeus.util.Toasts.meuToastLong;
import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class RetroCompatibilidadeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retro_compatibilidade);

        configurarToolBarBranca(this);

        configurarBotaoImportar();
        configurarBotaoNaoImportar();
    }

    private void configurarBotaoImportar() {
        findViewById(R.id.botaoImportar).setOnClickListener(view -> importarBancoAntigo(this));
    }

    private void configurarBotaoNaoImportar() {
        findViewById(R.id.botaoNaoImportar).setOnClickListener(view -> voltarParaSplahScreen(this));
    }

    private static void voltarParaSplahScreen(Activity context) {

        MensagemDAO mensagemDAO = new MensagemDAO(context);
        mensagemDAO.deletarTodasMensagens();

        AutorDAO autorDAO = new AutorDAO(context);
        autorDAO.deletarTodosAutores();

        context.startActivity(new Intent(context, SplashScreenActivity.class));
        context.finish();
    }

    private static void trocarTextoPreCarregamentoBanco(Activity context) {
        ((TextView) context.findViewById(R.id.subTituloTextView)).setText(R.string.importando_banco);

        Animacoes.animarComFade(context.findViewById(R.id.avisoLayout), false);
        Animacoes.animarComFade(context.findViewById(R.id.progressoLayout), true);
    }

    private static void importarBancoAntigo(Activity context) {

        new AsyncTask<Void, Integer, Boolean>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                trocarTextoPreCarregamentoBanco(context);
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    MensagemDAO mensagemDAO = new MensagemDAO(context);
                    List<Mensagem> mensagens = mensagemDAO.listarRespostasCompleto();
                    List<Sentenca> sentencas = new ArrayList<>();

                    int progressoTotal = mensagens.size() * 2;
                    int progressoAtual = 0;

                    int idItemIa = ItemEnum.IA.ordinal();

                    for (Mensagem mensagem : mensagens) {
                        Sentenca sentenca = new Sentenca();
                        sentenca.setAcao(AcaoEnum.SEM_ACAO);
                        sentenca.setTipo_item(idItemIa);
                        sentenca.setChave(mensagem.getConteudo());
                        sentenca.addResposta(mensagem.getConteudo_resposta());

                        sentencas.add(sentenca);

                        progressoAtual++;
                        this.publishProgress((int) ((progressoAtual / (float) progressoTotal) * 100));
                    }


                    SentencaDAO sentencaDAO = new SentencaDAO(context, false);
                    for (Sentenca sentenca : sentencas) {
                        sentencaDAO.inserir(sentenca);

                        progressoAtual++;
                        this.publishProgress((int) ((progressoAtual / (float) progressoTotal) * 100));
                    }


                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

            }

            @Override
            protected void onPostExecute(Boolean bancoInserido) {
                super.onPostExecute(bancoInserido);
                if (bancoInserido) {
                    voltarParaSplahScreen(context);
                } else {
                    meuToast("Erro ao importar o banco de dados anterior", context);
                    meuToastLong("Reinicie o app", context);
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                Integer porcentagemProgresso = values[0];
                String progressoAtual = porcentagemProgresso + "%";

                ((TextView) context.findViewById(R.id.progressoTextView)).setText(progressoAtual);
                ((ProgressBar) context.findViewById(R.id.barraProgresso)).setProgress(porcentagemProgresso);

            }
        }.execute();
    }

}
