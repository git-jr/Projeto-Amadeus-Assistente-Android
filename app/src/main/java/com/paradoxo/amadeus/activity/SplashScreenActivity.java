package com.paradoxo.amadeus.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.PackageInfoCompat;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.dao.AutorDAO;
import com.paradoxo.amadeus.dao.EntidadeDAO;
import com.paradoxo.amadeus.dao.MensagemDAO;
import com.paradoxo.amadeus.dao.SentencaDAO;
import com.paradoxo.amadeus.modelo.Autor;
import com.paradoxo.amadeus.modelo.Entidade;
import com.paradoxo.amadeus.modelo.Mensagem;
import com.paradoxo.amadeus.modelo.Sentenca;
import com.paradoxo.amadeus.util.Animacoes;
import com.paradoxo.amadeus.util.Preferencias;

import java.util.List;

import static com.paradoxo.amadeus.util.Preferencias.appJaFoiAberto;
import static com.paradoxo.amadeus.util.Preferencias.confirmarAberturaApp;
import static com.paradoxo.amadeus.util.Toasts.meuToast;
import static com.paradoxo.amadeus.util.Toasts.meuToastLong;
import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class SplashScreenActivity extends AppCompatActivity {
    Activity context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

/*
        startActivity(new Intent(this, TrocarVozActivity.class));
        finish();
*/

        context = this;
        checarVersaoAntiga();
        //inseriMensagemTesteRetrocompatibilidade();


    }

    private void iniciarApp() {
        configurarToolBarBranca(this);
        verificarBanco(appJaFoiAberto(this), this);
    }

    private static void verificarBanco(boolean bancoJaInserido, Activity context) {
        try {
            PackageInfo pInfo;
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            long longVersionCode = PackageInfoCompat.getLongVersionCode(pInfo);

            if (longVersionCode < 18 || !bancoJaInserido) { // Versões anteriores a Pretty e novas versões
                copiarBanco(context);
            } else {
                new Handler().postDelayed(() -> decidirParaOndevai(context), 1000);
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            meuToast(context.getString(R.string.erro_iniciar_app), context);
        }
    }

    private static void decidirParaOndevai(Activity context) {
        if (!Preferencias.getPrefString("nomeUsu", context).isEmpty()) {
            context.startActivity(new Intent(context.getApplicationContext(), MainActivity.class));
            context.finish();
        } else {
            context.startActivity(new Intent(context.getApplicationContext(), ConfigPrimariaActivity.class));
            context.finish();
        }
        context.finish();
    }

    private void inseriMensagemTesteRetrocompatibilidade() {
        Autor autor = new Autor("NomeAutor1");
        AutorDAO autorDAO = new AutorDAO(this);
        autor.setId((int) autorDAO.inserirAutor(autor));


        MensagemDAO mensagemDAO = new MensagemDAO(this);

        int i = 1;
        while (i != 2500) {
            mensagemDAO.inserirMensagemImportada("boa " + i, 1);
            mensagemDAO.inserirMensagemImportada("notche " + i, 1);

            mensagemDAO.inserirRespostaImportada(i, i + 1);
            i++;

            Log.e("Gravando...", String.valueOf(i));
        }

    }

    private void checarVersaoAntiga() {
        MensagemDAO mensagemDAO = new MensagemDAO(this);
        boolean bancoAntigoExiste = mensagemDAO.verificarExistencia(new Mensagem(0));

        //bancoAntigoExiste = true;
        if (bancoAntigoExiste) {
            startActivity(new Intent(this, RetroCompatibilidadeActivity.class));
            finish();
        } else {
            iniciarApp();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected void onStop() {
        super.onStop();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private static void trocarTextoPreCarregamentoBanco(Activity context) {
        ((TextView) context.findViewById(R.id.subTituloTextView)).setText(context.getString(R.string.carregando_banco));

        Animacoes.animarComFade(context.findViewById(R.id.logoAmadeusImageView), false);
        Animacoes.animarComFade(context.findViewById(R.id.progressoLayout), true);
    }

    private static void copiarBanco(Activity context) {

        new AsyncTask<Void, Integer, Boolean>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                trocarTextoPreCarregamentoBanco(context);
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    SentencaDAO sentencaDAO = new SentencaDAO(context, false);
                    SentencaDAO sentencaDAOHistorico = new SentencaDAO(context, true);
                    EntidadeDAO entidadeDAO = new EntidadeDAO(context);

                    List<Sentenca> sentencasJson = sentencaDAO.getSentencasPadraoJson();
                    List<Sentenca> sentencasHistoricoJson = sentencaDAOHistorico.getSentencasHistoricoPadraoJson();
                    List<Entidade> entidadesJson = entidadeDAO.getEntidadesPadraoJson();

                    int progressoTotal = (sentencasJson.size() + sentencasHistoricoJson.size() + entidadesJson.size());
                    int progressoAtual = 0;

                    for (Sentenca sentenca : sentencasJson) {
                        sentencaDAO.inserir(sentenca);

                        progressoAtual++;
                        this.publishProgress((int) ((progressoAtual / (float) progressoTotal) * 100));

                    }

                    for (Sentenca sentenca : sentencasHistoricoJson) {
                        sentencaDAOHistorico.inserir(sentenca);

                        progressoAtual++;
                        this.publishProgress((int) ((progressoAtual / (float) progressoTotal) * 100));

                    }

                    for (Entidade entidade : entidadesJson) {
                        entidadeDAO.inserir(entidade);

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
                    confirmarAberturaApp(context);
                    verificarBanco(true, context);
                } else {
                    meuToast("Erro ao carregar o banco de dados", context);
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
