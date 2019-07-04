package com.paradoxo.amadeus.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.dao.AutorDAO;
import com.paradoxo.amadeus.modelo.Autor;

public class LoadActivity extends AppCompatActivity {
    public static boolean emEdicao = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        verificarSeEstaEditando();

        findViewById(R.id.linkStartButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configurarBotaoLinkStart();
            }
        });


    }

    private void verificarSeEstaEditando() {
        try {
            Intent intent = this.getIntent();
            if (intent.getStringExtra("nomeUsu").length() > 0) {

                EditText editTextNomeUsu = findViewById(R.id.nomeUsuarioTextView);
                EditText editTextNomeIa = findViewById(R.id.nomeIATextView);
                Button buttonLinkStart = findViewById(R.id.linkStartButton);

                editTextNomeUsu.setText(intent.getStringExtra("nomeUsu"));
                editTextNomeUsu.setSelection(editTextNomeUsu.getText().length());

                editTextNomeIa.setText(intent.getStringExtra("nomeIA"));
                editTextNomeIa.setSelection(editTextNomeIa.getText().length());

                buttonLinkStart.setText(this.getString(R.string.Atualizar));

                emEdicao = true;
            }
        } catch (Exception e) {
            Log.e("Acitivity Load", "Primeiro uso");
        }

        chamarDialogPrimeiroUso();
    }

    private void chamarDialogPrimeiroUso() {
        if (!emEdicao) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(String.valueOf(this.getText(R.string.bem_vindo)));
            builder.setMessage(String.valueOf(this.getText(R.string.moldar_as_coisas)));

            builder.setPositiveButton(String.valueOf(this.getText(R.string.ok)), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                }
            });


            builder.setNegativeButton(String.valueOf(this.getText(R.string.cancelar)), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    LoadActivity.this.finishAffinity();

                }
            });

            AlertDialog alerta = builder.create();
            alerta.show();
            emEdicao = false;
        }
    }

    public void configurarBotaoLinkStart() {
        AutoCompleteTextView NomeUsu = findViewById(R.id.nomeUsuarioTextView);
        AutoCompleteTextView NomeIA = findViewById(R.id.nomeIATextView);
        String nomeUsu = NomeUsu.getText().toString().trim();
        String nomeIa = NomeIA.getText().toString().trim();

        if ((nomeIa.length() > 0) && (nomeUsu.length() > 0)) {

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

        } else {
            Snackbar.make(findViewById(R.id.activityLoad), String.valueOf(this.getText(R.string.preencha_tudo)), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    private void setPrefBool() {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putBoolean("bancoInserido", true);
        mEditor.apply();
    }

}
