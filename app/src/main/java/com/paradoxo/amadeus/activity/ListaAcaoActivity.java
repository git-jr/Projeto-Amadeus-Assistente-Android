package com.paradoxo.amadeus.activity;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.adapter.AdapterSimples;
import com.paradoxo.amadeus.dao.AcaoDAO;
import com.paradoxo.amadeus.enums.AcaoEnum;
import com.paradoxo.amadeus.modelo.Acao;
import com.paradoxo.amadeus.modelo.Sentenca;

import java.util.ArrayList;
import java.util.List;

import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class ListaAcaoActivity extends AppCompatActivity {

    static AdapterSimples adapter;
    static String textoBusca;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_sentencas);

        configurarIterface();
    }

    private void configurarIterface() {
        configurarToolBarBranca(this);
        configurarRecycler();
        configurarBotaoAdicionar();
        configurarBotaoBusca();
    }

    private void configurarBotaoBusca() {
        ((EditText) findViewById(R.id.buscaSentencaEditText)).setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                textoBusca = textView.getText().toString().toLowerCase().trim();
                carregaSentencaBanco(this);
                return true;
            }
            return false;
        });
    }

    private void configurarBotaoAdicionar() {
        ImageView imageView = findViewById(R.id.adicionarButton);
        imageView.setImageResource(R.drawable.ic_search);

        findViewById(R.id.adicionarButton).setOnClickListener(view -> {
            textoBusca = ((EditText) findViewById(R.id.buscaSentencaEditText)).getText().toString().trim();
            carregaSentencaBanco(this);
        });
    }

    private static List<Sentenca> gerarListaAcoes(String busca, Activity context) {
        AcaoDAO acaoDAO = new AcaoDAO(context);
        List<Acao> acoes = acaoDAO.getAcoes();

        List<Sentenca> itens = new ArrayList<>();

        for (Acao acao : acoes) {

            Sentenca sentenca = new Sentenca();
            sentenca.setAcao(AcaoEnum.SEM_ACAO);

            String nome = acao.getAcaoEnum().toString();
            sentenca.setChave(nome.replace("_", " "));
            sentenca.addResposta("Gatilhos: " + acao.getGatilhos().toString().replace("[", "").replace("]", ""));


            if (busca == null || (nome.toLowerCase().contains(busca) || acao.getGatilhos().toString().toLowerCase().contains(busca))) {
                itens.add(sentenca);
            }
        }
        return itens;
    }

    private static void atualizarRecycler(List<Sentenca> sentencas) {
        adapter.addAll(sentencas);
    }

    private void configurarRecycler() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        List<Sentenca> sentencas = new ArrayList<>();
        adapter = new AdapterSimples(sentencas);
        recyclerView.setAdapter(adapter);

        carregaSentencaBanco(this);

        adapter.setOnItemClickListener((view, sentenca, pos) -> abrirDialogVerAcao(sentenca));

        adapter.setOnLongClickListener((view, position, mensagem) -> {
        });

    }

    public void abrirDialogVerAcao(Sentenca sentenca) {
        Dialog alertDialogBuilder = new Dialog(this);
        LayoutInflater inflater = this.getLayoutInflater();
        alertDialogBuilder.setContentView(inflater.inflate(R.layout.dialog_ver_acao, findViewById(R.id.listaSetencasLayout)));

        ((TextView) alertDialogBuilder.findViewById(R.id.tituloTextView)).setText(sentenca.getChave());
        ((TextView) alertDialogBuilder.findViewById(R.id.conteudoTexView)).setText(sentenca.getRespostas().toString().replace("[", "").replace("]", ""));

        alertDialogBuilder.findViewById(R.id.botaoOk).setOnClickListener(view -> alertDialogBuilder.dismiss());
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.show();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private static void carregaSentencaBanco(Activity context) {

        new AsyncTask<Void, Void, List<Sentenca>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected List<Sentenca> doInBackground(Void... voids) {
                if (textoBusca == null || textoBusca.isEmpty()) {
                    return gerarListaAcoes(null,context);
                } else {
                    // Buscar
                    return gerarListaAcoes(textoBusca, context);
                }

            }

            @Override
            protected void onPostExecute(List<Sentenca> sentencas) {
                super.onPostExecute(sentencas);

                if (textoBusca == null || textoBusca.isEmpty()) {
                    atualizarRecycler(sentencas);
                } else {
                    adapter.trocarLista(sentencas);
                }

            }
        }.execute();
    }

}
