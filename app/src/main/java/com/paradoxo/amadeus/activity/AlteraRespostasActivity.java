package com.paradoxo.amadeus.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.adapter.AdapterMensagensBusca;
import com.paradoxo.amadeus.dao.AutorDAO;
import com.paradoxo.amadeus.dao.MensagemDAO;
import com.paradoxo.amadeus.modelo.Mensagem;
import com.paradoxo.amadeus.util.Animacoes;

import java.util.List;

public class AlteraRespostasActivity extends AppCompatActivity {

    private MensagemDAO mensagemDAO;
    private List<Mensagem> mensagens;
    private boolean exibindoListaPergunta;
    private AdapterMensagensBusca adapterResposta;
    private AdapterMensagensBusca adapterMensagensBusca;
    private boolean exibindoListResposta = true;
    private boolean perguntaJaPossuiResposta = false;
    private EditText editTextPergunta, editTextResposta;
    private String respostaSelecionada, perguntaSelecionada;
    private LinearLayout linearLayoutPergunta, linearLayoutResposta;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respostas_alterar);

        recuperarPerguntaActivityAnterior();

        mensagemDAO = new MensagemDAO(this);

        configurarRecylers();
        iniciarlizarInterface();
    }

    private void recuperarPerguntaActivityAnterior() {
        try {
            Intent intent = this.getIntent();
            perguntaSelecionada = intent.getStringExtra("pergunta_selecionada");
            respostaSelecionada = intent.getStringExtra("resposta_selecionada");

            if (respostaSelecionada != null) perguntaJaPossuiResposta = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configurarRecylers() {

        mensagens = mensagemDAO.listarParcial(null, true);

        RecyclerView recyclerViewPergunta = findViewById(R.id.recyclerPergunta);
        RecyclerView recyclerViewResposta = findViewById(R.id.recyclerResposta);

        adapterMensagensBusca = new AdapterMensagensBusca(mensagens);
        recyclerViewPergunta.setAdapter(adapterMensagensBusca);

        adapterResposta = new AdapterMensagensBusca(mensagens);
        recyclerViewResposta.setAdapter(adapterResposta);

        adapterMensagensBusca.setOnItemClickListener(new AdapterMensagensBusca.OnItemClickListener() {
            @Override
            public void onItemClick(View view, String viewModel, int pos) {
                editTextPergunta.setText(viewModel);
                editTextPergunta.setSelection(viewModel.length());
                // Move a posição do cursor para o fim do texto para facilitar a edição

                Animacoes.contrairRecycler(linearLayoutPergunta);
                exibindoListaPergunta = true;
                esconderTeclado();
            }
        });


        adapterResposta.setOnItemClickListener(new AdapterMensagensBusca.OnItemClickListener() {
            @Override
            public void onItemClick(View view, String viewModel, int pos) {
                editTextResposta.setText(viewModel);
                editTextResposta.setSelection(viewModel.length());
                Animacoes.contrairRecycler(linearLayoutResposta);
                exibindoListResposta = true;
                esconderTeclado();
            }
        });


    }

    private void iniciarlizarInterface() {
        linearLayoutPergunta = findViewById(R.id.ll_recycler_pergunta);
        linearLayoutResposta = findViewById(R.id.ll_recycler_resposta);

        editTextPergunta = findViewById(R.id.perguntaEditText);
        editTextPergunta.setText(perguntaSelecionada);
        editTextPergunta.setSelection(perguntaSelecionada.length());
        editTextPergunta.addTextChangedListener(detectarMudancaTextoPergunta);

        editTextResposta = findViewById(R.id.respostaEditText);

        if (perguntaJaPossuiResposta) {
            editTextResposta.setText(respostaSelecionada);
            editTextResposta.setSelection(respostaSelecionada.length());
            editTextResposta.addTextChangedListener(detectarMudancaTextoResposta);

        }

        editTextPergunta.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // Veirifica se o botão de "Ok" do teclado foi clicado e se sim esconde o teclado
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    esconderTeclado();
                    return true;
                }
                return false;
            }
        });

        editTextResposta.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    esconderTeclado();
                    return true;
                }
                return false;
            }
        });

        findViewById(R.id.cancelarButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                esconderTeclado();
                finish();
            }
        });

        findViewById(R.id.confirmarButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                atualizarResposta();
            }
        });
    }

    private TextWatcher detectarMudancaTextoPergunta = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence c, int i, int i1, int i2) {
            animarRecyclerPergunta();
        }

        @Override
        public void beforeTextChanged(CharSequence c, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {

            if (perguntaJaPossuiResposta) {
                mensagens = mensagemDAO.listarParcial(editable.toString(), true);
                adapterMensagensBusca.setMensagens(mensagens);
                adapterMensagensBusca.atualiza_recycler();
            }
        }
    };

    private TextWatcher detectarMudancaTextoResposta = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence c, int i, int i1, int i2) {
            animarRecyclerResposta();
        }

        @Override
        public void beforeTextChanged(CharSequence c, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {

            mensagens = mensagemDAO.listarParcial(editable.toString(), false);
            adapterResposta.setMensagens(mensagens);
            adapterResposta.atualiza_recycler();

        }
    };

    public void esconderTeclado() {
        View view = this.getCurrentFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            assert view != null;
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

    }


    private void animarRecyclerPergunta() {
        if (exibindoListaPergunta) {
            Animacoes.expandirRecycler(linearLayoutPergunta);
            exibindoListaPergunta = false;
        }
    }

    private void animarRecyclerResposta() {
        if (exibindoListResposta) {
            Animacoes.expandirRecycler(linearLayoutResposta);
            exibindoListResposta = false;
        }
    }


    public void atualizarResposta() {
        // Fazer isso em asyncTask no futuro

        String novaPergunta = String.valueOf(editTextPergunta.getText()).trim();
        String novaResposta = String.valueOf(editTextResposta.getText()).trim();

        if (!(novaPergunta.length() < 1 || novaResposta.length() < 1)) {

            MensagemDAO msgDAO = new MensagemDAO(this);
            mensagens = msgDAO.listarRespostasCompleto();
            Mensagem objMsgPergunta = new Mensagem();
            objMsgPergunta.setConteudo(perguntaSelecionada);
            objMsgPergunta = (msgDAO.buscaPorConteudo(objMsgPergunta, !perguntaJaPossuiResposta));

            if (!novaPergunta.equals(perguntaSelecionada)) {
                objMsgPergunta.setConteudo(novaPergunta);
                msgDAO.alterar(objMsgPergunta);
            }


            if (!novaResposta.equals(respostaSelecionada)) {
                Mensagem objMsgResposta = new Mensagem();
                objMsgResposta.setConteudo(novaResposta);
                objMsgResposta = msgDAO.buscaPorConteudo(objMsgResposta, true);

                if (objMsgResposta.getAutor() == null) {
                    // Se não existir uma resposta assim já cadastrada, vamos cadastrar uma

                    objMsgResposta.setConteudo(novaResposta);

                    AutorDAO autorDAO = new AutorDAO(this);
                    objMsgResposta.setAutor(autorDAO.listar().get(0));

                    objMsgResposta.setIdResposta((int) mensagemDAO.inserirMensagem(objMsgResposta));
                    // Grava a nova resposta e traz seu ID para que ele possa ser inserido na resposta da pergunta selecionada

                    objMsgResposta.setId(objMsgPergunta.getId());

                    mensagemDAO.inserirResposta(objMsgResposta);

                } else {
                    // Se a resposta já estiver cadastrada vamos apenas alterar o id_resposta

                    objMsgPergunta.setIdResposta(objMsgResposta.getId());
                    msgDAO.alterar(objMsgPergunta);
                }
            }

            if (perguntaJaPossuiResposta) {
                finish();
                meuToast(String.valueOf(this.getText(R.string.resposta_atualizada)));
            } else {
                meuToast(String.valueOf(this.getText(R.string.resposta_gravada)));
            }

            finish();

        } else {
            meuToast(String.valueOf(this.getText(R.string.preencha_tudo)));
        }
    }

    private void meuToast(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show();
    }
}



