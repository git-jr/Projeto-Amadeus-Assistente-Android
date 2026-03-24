package com.paradoxo.amadeus.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.adapter.AdapterSinonimos;
import com.paradoxo.amadeus.dao.EntidadeDAO;
import com.paradoxo.amadeus.fragments.DialogSimples;
import com.paradoxo.amadeus.modelo.Entidade;

import java.util.ArrayList;
import java.util.List;

import static com.paradoxo.amadeus.util.Toasts.meuToast;
import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;
import static com.paradoxo.amadeus.util.Util.esconderTeclado;

public class EditarItemEntidadeNovoActivity extends AppCompatActivity implements DialogSimples.FragmentDialogInterface {

    boolean modificado;
    static Entidade entidadeEmUso;
    static AdapterSinonimos adapter;
    static TextInputEditText entradaEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_item_novo);

        configurarInterface();
    }

    private void configurarInterface() {
        configurarToolBarBranca(this);
        configurarTextInput();
        configuarBotaoLinkStart();
        configuarBotaoAddResposta();
        configurarRecycler();
    }

    private static void atualizarRecycler(List<String> sinonimos) {
        adapter.addAll(sinonimos);
    }

    private void configurarRecycler() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        List<String> sinonimos = new ArrayList<>();
        adapter = new AdapterSinonimos(sinonimos);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL));

        adapter.setOnItemClickListener((view, sinonimo, pos, tipoDeletar) -> {
            if (tipoDeletar) {
                abrirDialogExcluir(pos);
            } else {
                abrirDialogEditar(pos, true);
            }
        });


    }

    private void inserirSinonimo(String novoSinonimo) {
        adapter.add(novoSinonimo);
        modificado = true;
    }

    private void alterarSinonimo(int pos, String novoSinonimo) {
        adapter.altera(pos, novoSinonimo);
        modificado = true;
    }

    private void deletarSinonimo(int pos) {
        adapter.remove(pos);
        modificado = true;
    }

    public void abrirDialogEditar(Integer posi, boolean editando) {
        Dialog alertDialogBuilder = new Dialog(this);
        LayoutInflater inflater = this.getLayoutInflater();
        alertDialogBuilder.setContentView(inflater.inflate(R.layout.dialog_editar_sinonimo, findViewById(R.id.editarItemEntidadeLayout), false));

        TextInputEditText sinonimoEditText = alertDialogBuilder.findViewById(R.id.sinonimoEditText);

        if (editando) {
            sinonimoEditText.setText(adapter.getItens().get(posi));
            sinonimoEditText.setSelection(adapter.getItens().get(posi).length());
        }

        TextInputLayout sinonimoTextInput = alertDialogBuilder.findViewById(R.id.sinonimoTextInput);

        TextView botaoNegar = alertDialogBuilder.findViewById(R.id.botaoCancelar);
        botaoNegar.setOnClickListener(view -> {
            esconderTeclado(view, EditarItemEntidadeNovoActivity.this);
            alertDialogBuilder.dismiss();
        });

        MaterialButton botaoSalvar = alertDialogBuilder.findViewById(R.id.botaoSalvar);
        botaoSalvar.setOnClickListener(view -> {

            if (String.valueOf(sinonimoEditText.getText()).isEmpty()) {
                sinonimoTextInput.setError(getString(R.string.entrada_invalida));
            } else {
                if (editando) {
                    alterarSinonimo(posi, String.valueOf(sinonimoEditText.getText()));
                } else {
                    inserirSinonimo(String.valueOf(sinonimoEditText.getText()));
                }

                esconderTeclado(view, EditarItemEntidadeNovoActivity.this);
                sinonimoTextInput.setErrorEnabled(false);
                alertDialogBuilder.dismiss();
            }
        });

        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.show();
    }

    public void abrirDialogExcluir(int posi) {
        DialogSimples dialog = DialogSimples.newDialog(
                getString(R.string.confirmar_exclusao),
                getString(R.string.tem_certeza_que_deseja_exluir_esse_item),
                posi,
                new int[]{
                        android.R.string.ok,
                        android.R.string.cancel});
        dialog.openDialog(getSupportFragmentManager());
    }

    @Override
    public void onClick(int posi, int which) {
        switch (which) {
            case -1:
                deletarSinonimo(posi);
                break;

            case -2:
                break;
        }
    }

    private void configurarTextInput() {
        ((TextView) findViewById(R.id.tituloTexView)).setText(getString(R.string.entidade));
        ((TextView) findViewById(R.id.titulo2TexView)).setText(getString(R.string.sinonimos));

        entradaEditText = findViewById(R.id.entradaEditText);

        entradaEditText.setOnFocusChangeListener((view, b) -> modificado = true);

        carregarEntidade(this);
    }

    private void configuarBotaoLinkStart() {
        findViewById(R.id.okButton).setOnClickListener(v -> validarInputs());
    }

    private void configuarBotaoAddResposta() {
        findViewById(R.id.adicionarSinonimoButton).setOnClickListener(view -> adicionarMaisUmSinonimo());
    }

    private void adicionarMaisUmSinonimo() {
        abrirDialogEditar(null, false);
    }

    public void validarInputs() {
        TextInputLayout entradaTextInput = findViewById(R.id.entradaTextInput);

        if (String.valueOf(entradaEditText.getText()).isEmpty()) {
            entradaTextInput.setError(getString(R.string.entrada_invalida));
        } else {
            entradaTextInput.setErrorEnabled(false);
            gravarDados(String.valueOf(entradaEditText.getText()).toLowerCase().trim(), this);
        }
    }

    @Override
    public void onBackPressed() {
        if (modificado) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.editarItemEntidadeLayout), getString(R.string.alteracao_ainda_nao_salva), Snackbar.LENGTH_LONG);
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.setAction(getString(R.string.sim_sair), new SnackBarListener()).show();
            modificado = false;
        } else {
            finalizarActivity(this);
        }
    }

    private static void finalizarActivity(Activity context) {
        context.startActivity(new Intent(context, ListaEntidadeActivity.class));
        context.finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public class SnackBarListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            startActivity(new Intent(getApplicationContext(), ListaEntidadeActivity.class));
            finish();
        }
    }

    private static void carregarEntidade(Activity context) {
        new AsyncTask<Void, Void, Entidade>() {
            @Override
            protected Entidade doInBackground(Void... voids) {

                Gson gson = new Gson();
                entidadeEmUso = gson.fromJson(context.getIntent().getStringExtra("entidade"), Entidade.class);
                return entidadeEmUso;

            }

            @Override
            protected void onPostExecute(Entidade entidade) {
                super.onPostExecute(entidade);
                if (entidade == null) return;
               entradaEditText.setText(entidade.getNome());

                atualizarRecycler(entidade.getSinonimos());
            }
        }.execute();
    }

    public static void gravarDados(String entradaValida, Activity context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                meuToast(context.getString(R.string.salvando_dados), context);

            }

            @Override
            protected Void doInBackground(Void... voids) {
                // Gson gson = new GsonBuilder().create();

                Entidade entidadeGravar = new Entidade();
                EntidadeDAO entidadeDAO = new EntidadeDAO(context);

                if (entidadeEmUso == null) {
                    entidadeGravar.setNome(entradaValida);
                    entidadeGravar.setSinonimos(adapter.getItens());
                    entidadeDAO.inserir(entidadeGravar);
                } else {
                    entidadeEmUso.setNome(entradaValida);
                    entidadeEmUso.setSinonimos(adapter.getItens());
                    entidadeDAO.alterarSentenca(entidadeEmUso);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                super.onPostExecute(v);
                meuToast(context.getString(R.string.salvo), context);
                finalizarActivity(context);
            }
        }.execute();
    }
}
