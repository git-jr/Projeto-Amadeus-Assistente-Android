package com.paradoxo.amadeus.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.adapter.AdapterSimplesEntidade;
import com.paradoxo.amadeus.dao.EntidadeDAO;
import com.paradoxo.amadeus.fragments.DialogSimples;
import com.paradoxo.amadeus.modelo.Entidade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class ListaEntidadeActivity extends AppCompatActivity implements DialogSimples.FragmentDialogInterface {

    static String textoBusca;
    static long limiteCarregarItensRecycler;
    static AdapterSimplesEntidade adapterSimples;

    static final long LIMITE_ITENS_PADRAO = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_sentencas);

        configurarIterface();
    }

    private void configurarBotaoAdicionar() {
        findViewById(R.id.adicionarButton).setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), EditarItemEntidadeNovoActivity.class));
            finish();
        });
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

    private static void atualizarRecycler(List<Entidade> entidades) {
        try {
            adapterSimples.addAll(entidades.subList(adapterSimples.getItemCount(), ((int) limiteCarregarItensRecycler)));
        } catch (Exception e) {
            adapterSimples.addAll(entidades.subList(adapterSimples.getItemCount(), entidades.size()));
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

        List<Entidade> entidades = new ArrayList<>();
        adapterSimples = new AdapterSimplesEntidade(entidades);
        recyclerView.setAdapter(adapterSimples);

        carregaSentencaBanco(this);

        adapterSimples.setOnItemClickListener((view, entidade, pos) -> {
            Log.e("nome", String.valueOf(entidade.getNome()));

            Gson gson = new Gson();
            Intent intent = new Intent(getApplicationContext(), EditarItemEntidadeNovoActivity.class);
            intent.putExtra("entidade", gson.toJson(entidade));
            startActivity(intent);
            finish();

        });

        adapterSimples.setOnLongClickListener((view, position, entidade) -> {
            abrirDialogExcluir(position);
            vibrar();
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int posiUltimoItem = Objects.requireNonNull(layoutManager).findLastCompletelyVisibleItemPosition();

                if (posiUltimoItem == limiteCarregarItensRecycler - 1) {
                    //meuToast("Carregando mais", getApplicationContext());
                    //Log.e("POSI", String.valueOf(posiUltimoItem));
                    carregaSentencaBanco(ListaEntidadeActivity.this);
                }

            }
        });
    }

    private void vibrar() {
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        long milliseconds = 100;
        if (vibrator != null) {
            vibrator.vibrate(milliseconds);
        }
    }

    public void abrirDialogExcluir(int posi) {
        DialogSimples dialog = DialogSimples.newDialog(
                "Confirmar exclusão",
                "Tem certeza que deseja excluir este item?",
                posi,
                new int[]{
                        android.R.string.ok,
                        android.R.string.cancel});
        dialog.openDialog(getSupportFragmentManager());
    }

    private void deletarEntidade(int posi) {
        EntidadeDAO entidadeDAO = new EntidadeDAO(getApplicationContext());
        entidadeDAO.excluir(adapterSimples.getItens().get(posi));
        adapterSimples.remove(posi);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void onClick(int posi, int which) {
        switch (which) {
            case -1:
                deletarEntidade(posi);
                break;

            case -2:
                break;
        }
    }

    private static void carregaSentencaBanco(Activity context) {

        new AsyncTask<Void, Void, List<Entidade>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                limiteCarregarItensRecycler += LIMITE_ITENS_PADRAO;
            }

            @Override
            protected List<Entidade> doInBackground(Void... voids) {
                EntidadeDAO entidadeDAO = new EntidadeDAO(context);

                if (textoBusca == null || textoBusca.isEmpty()) {
                    return entidadeDAO.listar(limiteCarregarItensRecycler);
                } else {
                    limiteCarregarItensRecycler = LIMITE_ITENS_PADRAO;
                    return entidadeDAO.buscaPorChaveLista(textoBusca, limiteCarregarItensRecycler);
                }
            }

            @Override
            protected void onPostExecute(List<Entidade> entidades) {
                super.onPostExecute(entidades);

                if (textoBusca == null || textoBusca.isEmpty()) {
                    atualizarRecycler(entidades);
                } else {
                    adapterSimples.trocarLista(entidades);
                }
            }
        }.execute();
    }
}
