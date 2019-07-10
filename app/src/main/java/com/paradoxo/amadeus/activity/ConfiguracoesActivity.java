package com.paradoxo.amadeus.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.paradoxo.amadeus.R;

import java.util.List;

public class ConfiguracoesActivity extends AppCompatPreferenceActivity {
    boolean vozIaAtiva, vozIaAtivaMesmoSemResposta;
    private static final int PICK_DOC = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configurarActionBar();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        vozIaAtiva = sharedPreferences.getBoolean("switch_voz_ia", true);
        vozIaAtivaMesmoSemResposta = sharedPreferences.getBoolean("switch_voz_ia_sem_resp", true);

    }

    @Override
    public void onHeaderClick(Header header, int position) {
        super.onHeaderClick(header, position);

        switch ((int) header.id) {
            case R.id.header_importar_qpython: {
                Intent qpythonActivity = new Intent(this, QPythonActivity.class);
                startActivity(qpythonActivity);
                break;
            }

            case R.id.header_segundo_plano: {
                Intent segundoPlanoConfiguracaoActivity = new Intent(this, VozSegundoPlanoConfiguracaoActivity.class);
                startActivity(segundoPlanoConfiguracaoActivity);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_DOC && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                meuToast(this.getString(R.string.selecao_nao_texto));
            }
        }
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private void configurarActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_geral);
            setHasOptionsMenu(true);

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {

            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), ConfiguracoesActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }


    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class NotificationPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_exportar_importar);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), ConfiguracoesActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public final void reiniciarApp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ConfiguracoesActivity.this);
        builder.setTitle(String.valueOf(this.getText(R.string.reiniciar)));
        builder.setMessage(String.valueOf(this.getText(R.string.alteracao_realizada)));

        builder.setPositiveButton(String.valueOf(this.getText(R.string.agora)), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                ConfiguracoesActivity.this.finishAffinity();
                Intent mainActivity = new Intent(ConfiguracoesActivity.this, MainActivity.class);
                startActivity(mainActivity);
            }
        });


        builder.setNegativeButton(String.valueOf(this.getText(R.string.reiniciar_depois)), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
            }
        });

        AlertDialog alerta = builder.create();
        alerta.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        verificarAlteracaoes();
    }

    public void verificarAlteracaoes() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean switch_voz_ia = sharedPreferences.getBoolean("switch_voz_ia", true);
        boolean switch_voz_ia_sem_resp = sharedPreferences.getBoolean("switch_voz_ia_sem_resp", true);

        if ((switch_voz_ia != vozIaAtiva) || (switch_voz_ia_sem_resp != vozIaAtivaMesmoSemResposta)) {
            reiniciarApp();
        }

    }

    public void meuToast(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
    }

}
