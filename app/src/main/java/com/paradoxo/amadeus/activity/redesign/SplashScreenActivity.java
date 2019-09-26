package com.paradoxo.amadeus.activity.redesign;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.activity.LoadActivity;
import com.paradoxo.amadeus.activity.MainActivity;
import com.paradoxo.amadeus.dao.BDGateway;
import com.paradoxo.amadeus.util.Arquivo;

import java.io.IOException;

import static com.paradoxo.amadeus.util.Preferencias.appJaFoiAberto;
import static com.paradoxo.amadeus.util.Toasts.meuToast;
import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        configurarToolBarBranca(this);
        decidirParaOndeVai();


    }

    private void decidirParaOndeVai() {

        if (appJaFoiAberto(this)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                }
            }, 5000);
        } else {
           PrimeiroLoad primeiroLoad = new PrimeiroLoad();
           primeiroLoad.execute();

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void cadastrarNomes() {
        Log.e("tag", "Cópia do banco finalizada");

        Intent loadActivity = new Intent(getApplicationContext(), LoadActivity.class);
        startActivity(loadActivity);
        finish();
    }

    @SuppressLint("StaticFieldLeak")
    public class PrimeiroLoad extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            meuToast(getString(R.string.carregando_banco_inicial),getApplicationContext());
            Log.e("tag", "Iniciando cópia do banco");
            BDGateway.getInstance(getBaseContext());
            // Isso inicializa o banco para que ele possa ser sobrescrito a seguir
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Arquivo.importarBancoPrimeiroUso("Amadeus.db", getApplicationContext());
                Log.e("tag", "Sucesso na cópia do banco");
            } catch (IOException e) {
                Log.e("tag", "Erro ao copiar o banco");
                e.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean bancoCopiado) {
            super.onPostExecute(bancoCopiado);

            if (bancoCopiado) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        cadastrarNomes();
                    }
                }, 1000);
            } else {
                meuToast("Falha ao copiar o banco de dados",getApplicationContext());
            }
        }
    }
}
