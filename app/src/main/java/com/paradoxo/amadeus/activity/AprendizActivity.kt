package com.paradoxo.amadeus.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.dao.AcaoDAO;
import com.paradoxo.amadeus.dao.EntidadeDAO;
import com.paradoxo.amadeus.dao.SentencaDAO;

import java.util.ArrayList;
import java.util.List;

import static com.paradoxo.amadeus.util.Preferencias.getPrefBool;
import static com.paradoxo.amadeus.util.Preferencias.setPrefBool;
import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class AprendizActivity extends AppCompatActivity {
    private static final int LISTA_ACAO = 2;
    private static final int LISTA_ENTIDADE = 1;
    private static final int LISTA_SENTENCA = 0;

    public static final String PREF_USAR_SINONIMOS_BUSCA = "usar_sinonimos_busca";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aprendiz);

        configurarInterface();
        carregarInfosBancoAtual(this);
    }

    private void configurarInterface() {
        configurarToolBarBranca(this);
        configurarChips();
        configurarToggleButton();
    }

    private void configurarChips() {
        ((ChipGroup) findViewById(R.id.rootchipGroup)).setOnCheckedChangeListener((group, checkedId) -> {
            Chip chipSelecionado = findViewById(checkedId);

            if (chipSelecionado != null) {

                int tipoLista = Integer.valueOf(chipSelecionado.getTag().toString());

                Intent intent = null;
                switch (tipoLista) {
                    case LISTA_SENTENCA: {
                        intent = new Intent(getApplicationContext(), ListaSentencaActivity.class);
                        break;
                    }
                    case LISTA_ENTIDADE: {
                        intent = new Intent(getApplicationContext(), ListaEntidadeActivity.class);
                        break;
                    }

                    case LISTA_ACAO: {
                        intent = new Intent(getApplicationContext(), ListaAcaoActivity.class);
                        break;
                    }
                }
                if (intent != null)
                    startActivity(intent);

                new Handler().postDelayed(() -> chipSelecionado.setChecked(false), 1000);

            }

        });
    }

    private void configurarToggleButton() {
        ToggleButton usoSinonimosToggleButton = findViewById(R.id.usoSinonimosToggleButton);

        usoSinonimosToggleButton.setChecked(getPrefBool(PREF_USAR_SINONIMOS_BUSCA, this, false));
        usoSinonimosToggleButton.setOnCheckedChangeListener((compoundButton, valor) -> setPrefBool(PREF_USAR_SINONIMOS_BUSCA, valor, getApplicationContext()));
    }

    private static void carregarInfosBancoAtual(Activity context) {

        new AsyncTask<Void, Void, List<Long>>() {
            @Override
            protected List<Long> doInBackground(Void... voids) {
                SentencaDAO sentencaDAO = new SentencaDAO(context, false);
                EntidadeDAO entidadeDAO = new EntidadeDAO(context);
                AcaoDAO acaoDAO = new AcaoDAO(context);

                List<Long> retornar = new ArrayList<>();
                retornar.add(sentencaDAO.getQuantidadeTotal());
                retornar.add(entidadeDAO.getQuantidadeTotal());
                retornar.add(acaoDAO.getQuantidadeTotal());


                return retornar;
            }

            @Override
            protected void onPostExecute(List<Long> valores) {
                super.onPostExecute(valores);

                ((TextView) context.findViewById(R.id.qtdSentencasTextView)).setText(String.valueOf(valores.get(0)));
                ((TextView) context.findViewById(R.id.qtdEntidadesTextView)).setText(String.valueOf(valores.get(1)));
                ((TextView) context.findViewById(R.id.qtdAcoesTextView)).setText(String.valueOf(valores.get(2)));
            }
        }.execute();
    }
}
