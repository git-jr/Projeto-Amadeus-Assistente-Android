package com.paradoxo.amadeus.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.activity.redesign.SimpleCallback;
import com.paradoxo.amadeus.adapter.AdapterEditaMensagem;
import com.paradoxo.amadeus.dao.MensagemDAO;
import com.paradoxo.amadeus.modelo.Mensagem;

import java.util.ArrayList;
import java.util.List;

import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class ListarRespostasActivity extends AppCompatActivity {

    TextView textViewNenhumaMsgAinda;
    private int posicaoDaMensagemEmEdicao = -1;
    private AdapterEditaMensagem adapterEditaMensagem;
    private ProgressDialog progressDialogCarregandoBanco;
    private List<Mensagem> mensagens = new ArrayList<>();
    RecyclerView recyclerView;

    @NonNull
    @Override
    public LayoutInflater getLayoutInflater() {
        return super.getLayoutInflater();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respostas_listar);

        CarregaMensagens carregaMensagens = new CarregaMensagens();
        carregaMensagens.execute();

        configurarToolBarBranca(this);
        configurarToolbar();
        configurarRecycler();

        configurarEditTextBusca();

        findViewById(R.id.enviarButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent alterarRespostasActivity = new Intent(ListarRespostasActivity.this, AlteraRespostasActivity.class);
                alterarRespostasActivity.putExtra("pergunta_selecionada", ((EditText) findViewById(R.id.mensagemUsuarioTextView)).getText().toString());
                alterarRespostasActivity.putExtra("inserindo",true);

                startActivity(alterarRespostasActivity);
            }
        });
    }

    private void configurarEditTextBusca() {

        ((EditText) findViewById(R.id.mensagemUsuarioTextView)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String textoDaBusca = s.toString();

                if (!textoDaBusca.trim().isEmpty()) {
                    CarregaMensagensParcial carregaMensagensParcial = new CarregaMensagensParcial();
                    carregaMensagensParcial.setTextoBusca(textoDaBusca);
                    carregaMensagensParcial.execute();
                } else {
                    CarregaMensagens carregaMensagens = new CarregaMensagens();
                    carregaMensagens.execute();
                }
            }
        });
    }

    private void configurarToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Algo 2");
        toolbar.setTitleTextColor(getResources().getColor(R.color.gray_700));

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Algo");
    }

    private void configurarRecycler() {
        textViewNenhumaMsgAinda = findViewById(R.id.nenhumaMensagemAindaTextView);
        textViewNenhumaMsgAinda.setVisibility(View.INVISIBLE);
        recyclerView = findViewById(R.id.recycler);

        adapterEditaMensagem = new AdapterEditaMensagem(mensagens);
        recyclerView.setAdapter(adapterEditaMensagem);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        posicaoDaMensagemEmEdicao = -1;
        // O valor "-1" indica que nenhuma mensagem está em edição no momento e por isso o método "onResume" não precisa atualizar a recycler quando chamado

        adapterEditaMensagem.setOnItemClickListenerEditar(new AdapterEditaMensagem.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int pos) {
                editarMensagem(pos);
                posicaoDaMensagemEmEdicao = pos;
            }
        });

        ItemTouchHelper itemTouchHelper = new
                ItemTouchHelper(new SimpleCallback(adapterEditaMensagem, ListarRespostasActivity.this));
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarRecyclerSeOuveAlteracao();
    }

    @SuppressLint("StaticFieldLeak")
    private void atualizarRecyclerSeOuveAlteracao() {
        if (posicaoDaMensagemEmEdicao > -1) {

            new AsyncTask<Object, Object, Object>() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    buscarMensagensBanco();
                    return null;
                }

                @Override
                protected void onPostExecute(Object o) {
                    super.onPostExecute(o);
                    adapterEditaMensagem.atualizar(mensagens.get(posicaoDaMensagemEmEdicao), posicaoDaMensagemEmEdicao);
                    posicaoDaMensagemEmEdicao = -1;

                }
            }.execute();
        }
    }

    public void editarMensagem(int position) {
        Intent alterarRespostasActivity = new Intent(this, AlteraRespostasActivity.class);
        alterarRespostasActivity.putExtra("pergunta_selecionada", mensagens.get(position).getConteudo());
        alterarRespostasActivity.putExtra("resposta_selecionada", mensagens.get(position).getConteudo_resposta());
        alterarRespostasActivity.putExtra("id_selecionado", String.valueOf(position));
        startActivity(alterarRespostasActivity);

    }

    public void meuToast(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show();
    }

    @SuppressLint("StaticFieldLeak")
    public class CarregaMensagens extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialogCarregandoBanco = ProgressDialog.show(ListarRespostasActivity.this, getString(R.string.carregando_banco), getString(R.string.aguarde), true, false);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            buscarMensagensBanco();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialogCarregandoBanco.dismiss();
            if (mensagens.size() > 0) {
                mostrarLayoutRecycler();
            } else {
                mostrarLayoutNadaEncontrado();
            }
        }
    }

    private void mostrarLayoutRecycler() {
        adapterEditaMensagem.trocaTudo(mensagens);
        adapterEditaMensagem.notifyDataSetChanged();

        recyclerView.setVisibility(View.VISIBLE);
        textViewNenhumaMsgAinda.setVisibility(View.GONE);


    }

    private void mostrarLayoutNadaEncontrado() {
        recyclerView.setVisibility(View.GONE);
        textViewNenhumaMsgAinda.setVisibility(View.VISIBLE);
    }

    private void buscarMensagensBanco() {
        MensagemDAO msgDAO = new MensagemDAO(getBaseContext());
        mensagens = msgDAO.listarRespostasCompleto();
    }

    private void buscarMensagensBancoParcial(String textoBusca) {
        MensagemDAO msgDAO = new MensagemDAO(getBaseContext());
        mensagens = msgDAO.listarParcial(textoBusca, true);
    }


    @SuppressLint("StaticFieldLeak")
    public class CarregaMensagensParcial extends AsyncTask<Void, Void, Void> {
        String textoBusca;

        public void setTextoBusca(String textoBusca) {
            this.textoBusca = textoBusca;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (progressDialogCarregandoBanco.isShowing()) {
                progressDialogCarregandoBanco.dismiss();
            }
            progressDialogCarregandoBanco = ProgressDialog.show(ListarRespostasActivity.this, getString(R.string.buscando), getString(R.string.aguarde), true, false);

        }

        @Override
        protected Void doInBackground(Void... voids) {
            buscarMensagensBancoParcial(textoBusca);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialogCarregandoBanco.dismiss();
            if (mensagens.size() > 0) {
                mostrarLayoutRecycler();

            } else {
                mostrarLayoutNadaEncontrado();
            }
        }
    }

}
