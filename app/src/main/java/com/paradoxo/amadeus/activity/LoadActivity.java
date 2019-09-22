package com.paradoxo.amadeus.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.dao.AutorDAO;
import com.paradoxo.amadeus.modelo.Autor;

import static com.paradoxo.amadeus.util.Toasts.meuToast;
import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class LoadActivity extends AppCompatActivity {
    public static boolean emEdicao = false;
    TextInputEditText nomeUsuarioEditText, nomeIaEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_redesign);


        configurarToolBarBranca(this);
        configuarBotaoLinkStart();
        configurarTextInput();
        verificarSeEstaEditando();

    }

    private void configurarTextInput() {
        nomeUsuarioEditText = findViewById(R.id.nomeUsuEditText);
        nomeIaEditText = findViewById(R.id.nomeIAEditText);

        ((TextInputEditText) findViewById(R.id.nomeIAEditText)).setOnEditorActionListener(new TextInputEditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    verificarNomes();
                    return true;
                }
                return false;
            }
        });
    }

    private void configuarBotaoLinkStart() {
        findViewById(R.id.okButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verificarNomes();
            }
        });
    }

    private void verificarSeEstaEditando() {
        try {
            Intent intent = this.getIntent();
            if (intent.getStringExtra("nomeUsu").length() > 0) {

                MaterialButton buttonLinkStart = findViewById(R.id.okButton);

                nomeUsuarioEditText.setText(intent.getStringExtra("nomeUsu"));
                nomeUsuarioEditText.setSelection(String.valueOf(nomeUsuarioEditText.getText()).length());

                nomeIaEditText.setText(intent.getStringExtra("nomeIA"));
                nomeIaEditText.setSelection(String.valueOf(nomeIaEditText.getText()).length());

                buttonLinkStart.setText(this.getString(R.string.Atualizar));

                emEdicao = true;
            }
        } catch (Exception e) {
            Log.e("Acitivity Load", "Primeiro uso");
            e.printStackTrace();
        }

    }

    public void gravarDados() {

        String nomeUsu = String.valueOf(nomeUsuarioEditText.getText()).trim();
        String nomeIa = String.valueOf(nomeIaEditText.getText()).trim();

        Autor objAutorUsu = new Autor(2, nomeUsu);
        Autor objAutorIa = new Autor(1, nomeIa);

        AutorDAO autorDAO = new AutorDAO(this);
        autorDAO.alterar(objAutorIa);
        autorDAO.alterar(objAutorUsu);

        Toast.makeText(this, getString(R.string.gravando_dados), Toast.LENGTH_LONG).show();

        setPrefBool();
        Intent mainActivity = new Intent(LoadActivity.this, MainActivity.class);
        startActivity(mainActivity);
        finish();
    }

    public void verificarNomes() {
        TextInputLayout nomeUsuTextInput = findViewById(R.id.nomeUsuTextInput);
        TextInputLayout nomeIATextInput = findViewById(R.id.nomeIATextInput);

        if (String.valueOf(nomeUsuarioEditText.getText()).isEmpty()) {
            nomeUsuTextInput.setError(getString(R.string.nome_invalido));
        } else {
            nomeUsuTextInput.setErrorEnabled(false);
        }

        if (String.valueOf(nomeIaEditText.getText()).isEmpty()) {
            nomeIATextInput.setError(getString(R.string.nome_invalido));
            return;
        } else {
            nomeIATextInput.setErrorEnabled(false);
        }

        meuToast(getString(R.string.gravando_dados), this);
        gravarDados();

    }

    private void setPrefBool() {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putBoolean("bancoInserido", true);
        mEditor.putBoolean("ja_foi_aberto", true);

        mEditor.apply();
    }

}












