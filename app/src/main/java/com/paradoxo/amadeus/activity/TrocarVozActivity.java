package com.paradoxo.amadeus.activity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.adapter.AdapterVozes;
import com.paradoxo.amadeus.modelo.Voz;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrocarVozActivity extends AppCompatActivity {
    private TextToSpeech textToSpeech;
    private EditText textViewTextoFalar;
    private String estiloDeVozSelecionada;
    private boolean estiloVozAlterado;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trocar_voz);

        configurarFala();
        configurarBotaoFala();

    }

    private void configurarBotaoFala() {
        findViewById(R.id.falarButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                falar();
            }
        });
    }

    private void configurarRecycler(List<Voz> vozes) {
        RecyclerView recyclerViewVozes = findViewById(R.id.recycler);
        AdapterVozes adapterVozes = new AdapterVozes(vozes, this);
        recyclerViewVozes.setAdapter(adapterVozes);

        adapterVozes.setOnItemClickListener(new AdapterVozes.OnItemClickListener() {
            @Override
            public void onItemClickListener(View view, int position, Voz voz) {
                estiloDeVozSelecionada = voz.getCodigo();
                falar();
                alterarTextoToolbar();
            }
        });
    }

    private void alterarTextoToolbar() {
        try {
            getSupportActionBar().setTitle(estiloDeVozSelecionada);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configurarFala() {
        textViewTextoFalar = findViewById(R.id.textoFalarEditText);
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.getDefault());

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Lingua não suportada");
                    } else {
                        Log.e("TTS", "TTS");
                        CarregarVozes carregarVozes = new CarregarVozes();
                        carregarVozes.execute();
                    }
                }
            }
        });
    }

    public void meuToast(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_tipo_voz, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int id = item.getItemId();

        switch (id) {
            case R.id.salvar: {
                gravarVozSelecionada();
                break;
            }

            case R.id.vozPadrao: {
                setPrefString("default");
                meuToast(getString(R.string.voz_padrao_selecionada));
                estiloDeVozSelecionada = "default";
                estiloVozAlterado = true;
                break;
            }

            case R.id.carregarRepetidas: {

                carregarVozesRepetidas();

                break;
            }

        }

        return true;
    }

    private void carregarVozesRepetidas() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(String.valueOf(this.getText(R.string.carregar_todas)));
        builder.setMessage(getString(R.string.aviso_vozes_repetidas));

        builder.setPositiveButton(String.valueOf(this.getText(R.string.agora)), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                CarregarVozes carregarVozes = new CarregarVozes();
                carregarVozes.setCarregarTodas(true);
                carregarVozes.execute();
            }
        });


        builder.setNegativeButton(String.valueOf(this.getText(R.string.reiniciar_depois)), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
            }
        });

        AlertDialog alerta = builder.create();
        alerta.show();

    }

    private void gravarVozSelecionada() {
        setPrefString(estiloDeVozSelecionada);
        meuToast(getString(R.string.estilo_voz_de_voz_alterado));
        meuToast(estiloDeVozSelecionada + " " + getString(R.string.agora_eh_o_estilo_padrao));

        estiloVozAlterado = true;
    }

    @Override
    public void onBackPressed() {


        if (estiloVozAlterado) {
            reiniciarApp();
        } else {
            super.onBackPressed();
        }
    }

    public final void reiniciarApp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(String.valueOf(this.getText(R.string.reiniciar)));
        builder.setMessage(String.valueOf(this.getText(R.string.alteracao_realizada)));

        builder.setPositiveButton(String.valueOf(this.getText(R.string.agora)), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finishAffinity();
                Intent mainActivity = new Intent(TrocarVozActivity.this, MainActivity.class);
                startActivity(mainActivity);
            }
        });


        builder.setNegativeButton(String.valueOf(this.getText(R.string.reiniciar_depois)), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                meuToast(getString(R.string.mudancas_serao_aplicadas));
                finish();
            }
        });

        AlertDialog alerta = builder.create();
        alerta.show();
    }


    private void setPrefString(String estiloDeVozSelecionada) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putString("estiloDeVozPadrao", estiloDeVozSelecionada);
        mEditor.apply();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void falar() {
        Voice voz = new Voice(estiloDeVozSelecionada, Locale.getDefault(), 1, 1, false, null);
        textToSpeech.setVoice(voz);

        String textoASerDito = textViewTextoFalar.getText().toString();
        if (textoASerDito.isEmpty())
            textoASerDito = "Essa é uma frase teste";

        textToSpeech.speak(textoASerDito, TextToSpeech.QUEUE_FLUSH, null);

    }

    @SuppressLint("StaticFieldLeak")
    private class CarregarVozes extends AsyncTask<Void, Void, List<Voz>> {

        boolean carregarTodas;

        public void setCarregarTodas(boolean carregarTodas) {
            this.carregarTodas = carregarTodas;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected List<Voz> doInBackground(Void... voids) {
            List<Voz> vozes = new ArrayList<>();

            try {
                for (Voice voice : textToSpeech.getVoices()) {
                    Voz voz = new Voz(voice.getLocale().getDisplayName(), voice.getLocale().getDisplayLanguage(), voice.getName());

                    if (carregarTodas) {
                        vozes.add(voz);
                    } else if (voice.getName().contains("-language")) {
                        vozes.add(voz);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return vozes;
        }

        @Override
        protected void onPostExecute(List<Voz> vozes) {
            super.onPostExecute(vozes);
            configurarRecycler(vozes);
            meuToast(vozes.size() + " " + getString(R.string.numero_vozes_econtradas));
        }
    }
}
