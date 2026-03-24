package com.paradoxo.amadeus.activity;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.util.Preferencias;

import static com.paradoxo.amadeus.util.Preferencias.getPrefBool;
import static com.paradoxo.amadeus.util.Preferencias.getPrefString;
import static com.paradoxo.amadeus.util.Preferencias.setPrefBool;
import static com.paradoxo.amadeus.util.Preferencias.setPrefString;
import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class ConfigPrimariaActivity extends AppCompatActivity {
    boolean primeiroUso = true;
    ToggleButton acessoDadosToggleButton;
    TextInputEditText nomeUsuarioEditText, nomeIaEditText;

    public static final String PREF_NOME_IA = "nomeIA";
    public static final String PREF_NOME_USU = "nomeUsu";
    public static final String PREF_VOZ_ATIVA = "voz_ativa";
    public static final String PREF_UPLOAD_DADOS_AUTORIZADO = "upload_dados_autorizado";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_primaria);

        configurarInterface();
    }

    private void configurarInterface() {
        configurarToolBarBranca(this);
        configurarTextInput();
        configuarBotaoLinkStart();
        configurarToggleButton();
    }

    private void configurarToggleButton() {

        ToggleButton modoFalaToggleButton = findViewById(R.id.modoFalaToggleButton);
        acessoDadosToggleButton = findViewById(R.id.acessoDadosToggleButton);

        modoFalaToggleButton.setChecked(getPrefBool(PREF_VOZ_ATIVA, this, false));
        acessoDadosToggleButton.setChecked(getPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO, this, false));

        modoFalaToggleButton.setOnCheckedChangeListener((compoundButton, valor) -> setPrefBool(PREF_VOZ_ATIVA,valor, getApplicationContext()));
        acessoDadosToggleButton.setOnCheckedChangeListener((compoundButton, valor) -> setPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO, valor, getApplicationContext()));
    }

    private void configurarTextInput() {
        nomeUsuarioEditText = findViewById(R.id.nomeUsuarioEditText);
        nomeIaEditText = findViewById(R.id.nomeIaEditText);

        String nomeUsu = getPrefString(PREF_NOME_USU, this);
        if(!nomeUsu.isEmpty()) {
            String nomeIA = getPrefString(PREF_NOME_IA, this);
            nomeUsuarioEditText.setText(nomeUsu);
            nomeIaEditText.setText(nomeIA);
            primeiroUso = false;
        }


        nomeIaEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                verificarNomes();
                return true;
            }
            return false;
        });
    }

    public void verificarNomes() {
        TextInputLayout nomeUsuarioTextInput = findViewById(R.id.nomeUsuarioTextInput);
        TextInputLayout nomeIATextInput = findViewById(R.id.nomeIaTextInput);

        if (String.valueOf(nomeUsuarioEditText.getText()).isEmpty()) {
            nomeUsuarioTextInput.setError(getString(R.string.nome_invalido));
        } else {
            nomeUsuarioTextInput.setErrorEnabled(false);
        }

        if (String.valueOf(nomeIaEditText.getText()).isEmpty()) {
            nomeIATextInput.setError(getString(R.string.nome_invalido));
            return;
        } else {
            nomeIATextInput.setErrorEnabled(false);
        }

        dialogUsoDados();
    }

    private void configuarBotaoLinkStart() {
        findViewById(R.id.okButton).setOnClickListener(v -> verificarNomes());
    }

    public  void dialogUsoDados(){
        boolean uploadDadosAutorizado = Preferencias.getPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO, this, false);
        if(uploadDadosAutorizado && !primeiroUso) {
            gravarDados();
            return;
        }

        Dialog alertDialogBuilder = new Dialog(this);
        LayoutInflater inflater = this.getLayoutInflater();
        alertDialogBuilder.setContentView(inflater.inflate(R.layout.dialog_sim_nao,findViewById(R.id.configPrimariaLayout), false));
        ((TextView) alertDialogBuilder.findViewById(R.id.tituloTextView)).setText(R.string.acesso_e_uso_dados_titulo);
        ((TextView) alertDialogBuilder.findViewById(R.id.conteudoTexView)).setText(R.string.acesso_e_uso_dados_descri);

        TextView botaoNegar = alertDialogBuilder.findViewById(R.id.botaoNegar);
        botaoNegar.setText(R.string.negar);
        botaoNegar.setOnClickListener(view -> {
            setPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO,false, getApplicationContext());
            acessoDadosToggleButton.setChecked(false);
            alertDialogBuilder.dismiss();
            gravarDados();
        });

        MaterialButton botaoAutorizar = alertDialogBuilder.findViewById(R.id.botaoAutorizar);
        botaoAutorizar.setText(R.string.autorizar);
        botaoAutorizar.setOnClickListener(view -> {
            setPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO, true, getApplicationContext());
            acessoDadosToggleButton.setChecked(true);
            alertDialogBuilder.dismiss();
            gravarDados();
        });

        alertDialogBuilder.findViewById(R.id.linkTextView).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getString(R.string.link_politica_privaicade)));
            startActivity(intent);
        });

        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.show();
    }

    private void gravarDados() {
        setPrefString( String.valueOf(nomeUsuarioEditText.getText()).trim(), PREF_NOME_USU,this);
        setPrefString(String.valueOf(nomeIaEditText.getText()).trim(), PREF_NOME_IA, this);

        if (primeiroUso) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
