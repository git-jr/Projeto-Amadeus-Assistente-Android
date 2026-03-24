package com.paradoxo.amadeus.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.paradoxo.amadeus.R;

import static com.paradoxo.amadeus.util.Preferencias.getPrefBool;
import static com.paradoxo.amadeus.util.Preferencias.setPrefBool;
import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class VozConfigActivity extends AppCompatActivity {
    public static final String PREF_VOZ_ATIVA = "voz_ativa";
    public static final String PREF_FALAR_RESPOSTA_NAO_ENCONTRADA = "falar_resposta_nao_encontrada";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voz_config);

        configurarInterface();
    }

    private void configurarInterface() {
        configurarToolBarBranca(this);
        configurarToggleButton();
        configurarItensMenu();
    }

    private void configurarToggleButton() {

        ToggleButton modoFalaToggleButton = findViewById(R.id.modoFalaToggleButton);
        ToggleButton falarRespostasNaoEncontradasToggleButton = findViewById(R.id.falarRespostasNaoEncontradasToggleButton);

        modoFalaToggleButton.setChecked(getPrefBool(PREF_VOZ_ATIVA, this, false));
        falarRespostasNaoEncontradasToggleButton.setChecked(getPrefBool(PREF_FALAR_RESPOSTA_NAO_ENCONTRADA, this, false));

        modoFalaToggleButton.setOnCheckedChangeListener((compoundButton, valor) -> setPrefBool(PREF_VOZ_ATIVA, valor, getApplicationContext()));
        falarRespostasNaoEncontradasToggleButton.setOnCheckedChangeListener((compoundButton, valor) -> setPrefBool(PREF_FALAR_RESPOSTA_NAO_ENCONTRADA, valor, getApplicationContext()));
    }

    private void configurarItensMenu() {
        LinearLayout tipoVozLayout = findViewById(R.id.tipoVozLayout);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tipoVozLayout.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), TrocarVozActivity.class)));
        } else {
            tipoVozLayout.setVisibility(View.GONE);
        }
    }
}
