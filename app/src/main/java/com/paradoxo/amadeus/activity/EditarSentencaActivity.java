package com.paradoxo.amadeus.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.dao.SentencaDAO;
import com.paradoxo.amadeus.enums.AcaoEnum;
import com.paradoxo.amadeus.enums.ItemEnum;
import com.paradoxo.amadeus.modelo.Sentenca;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.paradoxo.amadeus.util.Toasts.meuToast;
import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class EditarSentencaActivity extends AppCompatActivity {

    static String idItem;
    static List<TextInputEditText> respostas;
    static TextInputEditText entradaEditText;
    static List<LinearLayout> layoutsRepostas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_item);

        configurarInterface();
    }

    private void configurarInterface() {
        configurarToolBarBranca(this);
        configurarTextInput();
        configuarBotaoLinkStart();
        configuarBotaoAddResposta();
    }

    private void configurarTextInput() {
        entradaEditText = findViewById(R.id.entradaEditText);

        respostas = new ArrayList<>();
        respostas.add(findViewById(R.id.resposta1EditText));
        respostas.add(findViewById(R.id.resposta2EditText));
        respostas.add(findViewById(R.id.resposta3EditText));
        respostas.add(findViewById(R.id.resposta4EditText));
        respostas.add(findViewById(R.id.resposta5EditText));

        layoutsRepostas = new ArrayList<>();
        layoutsRepostas.add(findViewById(R.id.resposta1Layout));
        layoutsRepostas.add(findViewById(R.id.resposta2Layout));
        layoutsRepostas.add(findViewById(R.id.resposta3LayoutButton));
        layoutsRepostas.add(findViewById(R.id.resposta4Layout));
        layoutsRepostas.add(findViewById(R.id.resposta5Layout));

        carregarSentenca(this);
    }

    private void configuarBotaoLinkStart() {
        findViewById(R.id.okButton).setOnClickListener(v -> validarInputs());
    }

    private void configuarBotaoAddResposta() {

        findViewById(R.id.adicionarRespostaButton).setOnClickListener(view -> adicionarMaisUmaResposta());
    }

    private void adicionarMaisUmaResposta() {
        int totalVisiveis = 0;
        for (LinearLayout layoutsReposta : layoutsRepostas) {
            if (layoutsReposta.getVisibility() == View.VISIBLE) {
                totalVisiveis++;
            } else {
                layoutsReposta.setVisibility(View.VISIBLE);
                respostas.get(layoutsRepostas.indexOf(layoutsReposta)).setVisibility(View.VISIBLE);
                return;
            }
        }
        if (totalVisiveis == layoutsRepostas.size()) {
            meuToast(getString(R.string.limite_de_respostas_atingido), getApplicationContext());
        }
    }

    public void deletarResposta(View view) {
        int idReposta = Integer.parseInt(view.getTag().toString()) - 1;
        layoutsRepostas.get(idReposta).setVisibility(View.GONE);
        respostas.get(idReposta).setVisibility(View.GONE);
        Objects.requireNonNull(respostas.get(idReposta).getText()).clear();
    }

    public void validarInputs() {
        TextInputLayout entradaTextInput = findViewById(R.id.entradaTextInput);

        if (String.valueOf(entradaEditText.getText()).isEmpty()) {
            entradaTextInput.setError(getString(R.string.entrada_invalida));
            return;
        } else {
            entradaTextInput.setErrorEnabled(false);
        }

        int totalVisiveis = 0;
        List<String> respostasValidas = new ArrayList<>();
        for (TextInputEditText resposta : respostas) {
            if (resposta.getVisibility() != View.GONE) {
                totalVisiveis++;
                if (String.valueOf(resposta.getText()).isEmpty()) {
                    resposta.setError(getString(R.string.resposta_invalida));
                    return;
                } else {
                    respostasValidas.add(String.valueOf(resposta.getText()).trim());
                }
            }
        }

        if (totalVisiveis == 0) {
            meuToast(getString(R.string.deve_haver_ao_menos_uma_reposta), getApplicationContext());
        } else {
            gravarDados(String.valueOf(entradaEditText.getText()).trim(), respostasValidas, this);
        }
    }

    @Override
    public void onBackPressed() {
        finilizarActivity(this);
    }

    private static void finilizarActivity(Activity context) {
        context.startActivity(new Intent(context, ListaSentencaActivity.class));
        context.finish();
    }

    public static void gravarDados(String entradaValida, List<String> respostasValidas, Activity context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                meuToast(context.getString(R.string.salvando_dados), context);

            }

            @Override
            protected Void doInBackground(Void... voids) {
                // Gson gson = new GsonBuilder().create();

                Sentenca sentenca = new Sentenca();
                sentenca.setChave(entradaValida);
                sentenca.setRespostas(respostasValidas);

                SentencaDAO sentencaDAO = new SentencaDAO(context, false);

                if (idItem == null) {
                    sentencaDAO.inserir(sentenca);
                } else {
                    sentenca.setId(idItem);
                    sentenca.setAcao(AcaoEnum.SEM_ACAO);
                    sentenca.setTipo_item(ItemEnum.USUARIO.ordinal());
                    sentencaDAO.alterarSentenca(sentenca);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                super.onPostExecute(v);
                meuToast(context.getString(R.string.salvo), context);
                finilizarActivity(context);
            }
        }.execute();
    }

    private static void carregarSentenca(Activity context) {

        new AsyncTask<Void, Void, Sentenca>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                idItem = context.getIntent().getStringExtra("idItem");
            }

            @Override
            protected Sentenca doInBackground(Void... voids) {
                if (idItem == null) {
                    return null;
                } else {
                    SentencaDAO sentencaDAO = new SentencaDAO(context, false);
                    return sentencaDAO.buscaPorId(idItem);
                }
            }

            @Override
            protected void onPostExecute(Sentenca sentenca) {
                super.onPostExecute(sentenca);
                if (sentenca == null) return;
                entradaEditText.setText(sentenca.getChave());
                for (String resposta : sentenca.getRespostas()) {
                    int id = sentenca.getRespostas().indexOf(resposta);
                    respostas.get(id).setText(resposta);
                    respostas.get(id).setVisibility(View.VISIBLE);
                    layoutsRepostas.get(id).setVisibility(View.VISIBLE);

                }
            }
        }.execute();
    }
}
