package com.paradoxo.amadeus.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.service.EscutadaoraService;

public class VozSegundoPlanoConfiguracaoActivity extends AppCompatActivity {
    private static final int PERMISSAO_ACESSO_ARMAZENAMENTO = 1001;
    private Switch statusSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voz_segundo_plano_configuracao);

        configurarBoatoSwitch();
    }

    private void configurarBoatoSwitch() {
        statusSwitch = findViewById(R.id.StatusSwitch);
        boolean statusInicialBotao = getPrefBool("segundoPlanoAtivo");
        checarSwitch(statusInicialBotao);

        statusSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        iniciarEscutadoraService();
                    } else {
                        solicitarPermissaoMicrofone();
                    }
                } else {
                    pararEscutadoraService();
                }
            }
        });
    }

    private void solicitarPermissaoMicrofone() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSAO_ACESSO_ARMAZENAMENTO);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSAO_ACESSO_ARMAZENAMENTO) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                meuToastX(getString(R.string.permissao_nescessaria));
            } else {
                iniciarEscutadoraService();
            }
        }
    }

    private void checarSwitch(boolean statusInicialBotao) {
        if (!statusInicialBotao) {
            statusSwitch.setChecked(false);
            statusSwitch.setText(R.string.desativado);
            statusSwitch.setTextColor(getResources().getColor(R.color.vermelho));
        } else {
            statusSwitch.setChecked(true);
            statusSwitch.setText(R.string.ativo);
            statusSwitch.setTextColor(getResources().getColor(R.color.azul_link));

        }
    }

    public void iniciarEscutadoraService() {
        checarSwitch(true);
        setPrefBool("segundoPlanoAtivo", true);
        Intent intent = new Intent(this, EscutadaoraService.class);
        startService(intent);
    }

    public void pararEscutadoraService() {
        checarSwitch(false);
        setPrefBool("segundoPlanoAtivo", false);
        Intent intent = new Intent(this, EscutadaoraService.class);
        stopService(intent);
    }

    private boolean getPrefBool(String nomePref) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        return sharedPreferences.getBoolean(nomePref, false);
    }

    private void setPrefBool(String nomeShared, boolean valor) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putBoolean(nomeShared, valor);
        mEditor.apply();
    }

    public void meuToastX(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show();
    }
}
