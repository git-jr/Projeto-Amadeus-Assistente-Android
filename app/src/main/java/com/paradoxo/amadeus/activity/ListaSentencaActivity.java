package com.paradoxo.amadeus.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.adapter.AdapterSimples;
import com.paradoxo.amadeus.adapter.SimpleCallbackSentenca;
import com.paradoxo.amadeus.dao.SentencaDAO;
import com.paradoxo.amadeus.modelo.Sentenca;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class ListaSentencaActivity extends AppCompatActivity {

    static String textoBusca;
    static AdapterSimples adapterSimples;
    static long limiteCarregarItensRecycler;

    private static final long LIMITE_ITENS_PADRAO = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_sentencas);

        configurarIterface();
    }

    private void configurarBotaoBusca() {
        ((EditText) findViewById(R.id.buscaSentencaEditText)).setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                textoBusca = textView.getText().toString().trim();
                carregaSentencaBanco(this);
                return true;
            }
            return false;
        });
    }

    private void configurarBotaoAdicionar() {
        findViewById(R.id.adicionarButton).setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), EditarSentencaActivity.class));
            finish();
        });
    }

    private static void atualizarRecycler(List<Sentenca> sentencas) {
        try {
            adapterSimples.addAll(sentencas.subList(adapterSimples.getItemCount(), ((int) limiteCarregarItensRecycler)));
        } catch (Exception e) {
            adapterSimples.addAll(sentencas.subList(adapterSimples.getItemCount(), sentencas.size()));
        }
    }

    private void configurarIterface() {
        configurarToolBarBranca(this);
        configurarRecycler();
        configurarBotaoAdicionar();
        configurarBotaoBusca();
    }

    private void configurarRecycler() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        List<Sentenca> sentencas = new ArrayList<>();
        adapterSimples = new AdapterSimples(sentencas);
        recyclerView.setAdapter(adapterSimples);

        carregaSentencaBanco(this);

        adapterSimples.setOnItemClickListener((view, sentenca, pos) -> {
            Log.e("nome", String.valueOf(sentenca.getChave()));
            Log.e("tipo", String.valueOf(sentenca.getTipo_item()));

            Intent intent = new Intent(getApplicationContext(), EditarSentencaActivity.class);
            intent.putExtra("idItem", sentenca.getId());
            startActivity(intent);
            finish();
        });

        adapterSimples.setOnLongClickListener((view, position, mensagem) -> {
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int posiUltimoItem = Objects.requireNonNull(layoutManager).findLastCompletelyVisibleItemPosition();

                if (posiUltimoItem == limiteCarregarItensRecycler - 1) {
                    carregaSentencaBanco(ListaSentencaActivity.this);
                }

            }
        });

        ItemTouchHelper itemTouchHelper = new
                ItemTouchHelper(new SimpleCallbackSentenca(adapterSimples, this));
        itemTouchHelper.attachToRecyclerView(recyclerView);

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
                limiteCarregarItensRecycler += LIMITE_ITENS_PADRAO;
            }

            @Override
            protected List<Sentenca> doInBackground(Void... voids) {
                SentencaDAO sentencaDAO = new SentencaDAO(context, false);

                if (textoBusca == null || textoBusca.isEmpty()) {
                    return sentencaDAO.listar(limiteCarregarItensRecycler);
                } else {
                    limiteCarregarItensRecycler = LIMITE_ITENS_PADRAO;
                    return sentencaDAO.buscaPorChaveLista(textoBusca, limiteCarregarItensRecycler);
                }
            }

            @Override
            protected void onPostExecute(List<Sentenca> sentencas) {
                super.onPostExecute(sentencas);

                if (textoBusca == null || textoBusca.isEmpty()) {
                    atualizarRecycler(sentencas);
                } else {
                    adapterSimples.trocarLista(sentencas);
                }
            }
        }.execute();
    }
}
