package com.paradoxo.amadeus.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
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
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.adapter.AdapterVozes;
import com.paradoxo.amadeus.modelo.Voz;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.paradoxo.amadeus.util.Preferencias.getPrefString;
import static com.paradoxo.amadeus.util.Preferencias.setPrefString;
import static com.paradoxo.amadeus.util.Toasts.meuToast;
import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class TrocarVozActivity extends AppCompatActivity {
    Toolbar toolbar;
    boolean estiloVozAlterado;
    static String estiloDeVozPadrao;
    static TextToSpeech textToSpeech;

    public static final String PREF_ESTILO_DE_VOZ_PADRAO = "estiloDeVozPadrao";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trocar_voz);

        configurarFala();
        configurarInterface();
    }

    private void configurarInterface() {
        configurarToolBarBranca(this);
        configurarToolbar();
        configurarBotaoFala();
    }

    private void configurarFala() {

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.getDefault());

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    meuToast(getString(R.string.lingua_nao_suportada), this);
                    Log.e("TTS", getString(R.string.lingua_nao_suportada));
                } else {
                    Log.e("TTS", "TTS");
                    carregarVozes(false, this);
                }
            }
        });
    }

    private void configurarToolbar() {
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        estiloDeVozPadrao = getPrefString(PREF_ESTILO_DE_VOZ_PADRAO, this);
        estiloDeVozPadrao = estiloDeVozPadrao.isEmpty() ? getString(R.string.padrao) : estiloDeVozPadrao;
        alterarTextoToolbar(this);
    }

    private void configurarBotaoFala() {
        findViewById(R.id.falarButton).setOnClickListener(v -> falar(this));
    }

    private static void configurarRecycler(List<Voz> vozes, Activity context) {
        RecyclerView recyclerViewVozes = context.findViewById(R.id.recyclerView);
        AdapterVozes adapterVozes = new AdapterVozes(vozes);
        recyclerViewVozes.setAdapter(adapterVozes);

        adapterVozes.setOnItemClickListener((view, position, voz) -> {
            estiloDeVozPadrao = voz.getCodigo();
            falar(context);
            alterarTextoToolbar(context);
        });
    }

    private static void alterarTextoToolbar(Activity context) {
        ((Toolbar) context.findViewById(R.id.toolbar)).setTitle(estiloDeVozPadrao);
    }

    private void gravarVozSelecionada() {
        setPrefString(estiloDeVozPadrao, PREF_ESTILO_DE_VOZ_PADRAO, this);
        meuToast(getString(R.string.estilo_voz_de_voz_alterado), this);

        estiloVozAlterado = true;
    }

    public final void reiniciarApp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(String.valueOf(this.getText(R.string.reiniciar)));
        builder.setMessage(String.valueOf(this.getText(R.string.alteracao_realizada)));

        builder.setPositiveButton(String.valueOf(this.getText(R.string.agora)), (arg0, arg1) -> {
            finishAffinity();
            Intent mainActivity = new Intent(TrocarVozActivity.this, MainActivity.class);
            startActivity(mainActivity);
        });


        builder.setNegativeButton(String.valueOf(this.getText(R.string.reiniciar_depois)), (arg0, arg1) -> {
            meuToast(getString(R.string.mudancas_serao_aplicadas), getApplicationContext());
            finish();
        });

        AlertDialog alerta = builder.create();
        alerta.show();
    }

    private void carregarVozesRepetidas() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(String.valueOf(this.getText(R.string.carregar_todas)));
        builder.setMessage(getString(R.string.aviso_vozes_repetidas));

        builder.setPositiveButton(String.valueOf(this.getText(R.string.agora)), (arg0, arg1) -> carregarVozes(true, this));


        builder.setNegativeButton(String.valueOf(this.getText(R.string.cancelar)), (arg0, arg1) -> {
        });

        AlertDialog alerta = builder.create();
        alerta.show();
    }

    private void voltarParaVozPadrao() {
        setPrefString("default", PREF_ESTILO_DE_VOZ_PADRAO, this);
        meuToast(getString(R.string.voz_padrao_selecionada), this);
        estiloVozAlterado = true;

        estiloDeVozPadrao = getString(R.string.padrao);
        alterarTextoToolbar(this);
        estiloDeVozPadrao = "default";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_tipo_voz, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        int id = item.getItemId();

        switch (id) {
            case R.id.salvar: {
                gravarVozSelecionada();
                break;
            }

            case R.id.vozPadrao: {
                voltarParaVozPadrao();
                break;
            }

            case R.id.carregarRepetidas: {
                carregarVozesRepetidas();
                break;
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (estiloVozAlterado) {
            reiniciarApp();
        } else {
            super.onBackPressed();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void falar(Activity context) {
        Voice voz = new Voice(estiloDeVozPadrao, Locale.getDefault(), 1, 1, false, null);
        textToSpeech.setVoice(voz);

        EditText textViewTextoFalar = context.findViewById(R.id.textoFalarEditText);
        String textoASerDito = textViewTextoFalar.getText().toString();
        if (textoASerDito.isEmpty())
            textoASerDito = "Essa é uma frase teste";

        textToSpeech.speak(textoASerDito, TextToSpeech.QUEUE_FLUSH, null);

    }

    private static void mostrarLayoutVozes(Activity context) {
        context.findViewById(R.id.layoutLoad).setVisibility(View.GONE);
        context.findViewById(R.id.layoutPrinicipal).setVisibility(View.VISIBLE);
    }

    private static void carregarVozes(boolean carregarTodas, Activity context) {


        new AsyncTask<Void, Void, List<Voz>>() {

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
                configurarRecycler(vozes, context);
                mostrarLayoutVozes(context);
                meuToast(vozes.size() + " " + context.getString(R.string.numero_vozes_econtradas), context);
            }

        }.execute();


    }

}
