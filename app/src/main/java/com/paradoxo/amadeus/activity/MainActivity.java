package com.paradoxo.amadeus.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slidingpanelayout.widget.SlidingPaneLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.adapter.AdapterMaisDeUmItem;
import com.paradoxo.amadeus.cognicao.Acionadora;
import com.paradoxo.amadeus.cognicao.Processadora;
import com.paradoxo.amadeus.cognicao.faisca.Inicia;
import com.paradoxo.amadeus.dao.AcaoDAO;
import com.paradoxo.amadeus.dao.SentencaDAO;
import com.paradoxo.amadeus.enums.AcaoEnum;
import com.paradoxo.amadeus.enums.ItemEnum;
import com.paradoxo.amadeus.fragments.DialogSimples;
import com.paradoxo.amadeus.modelo.Acao;
import com.paradoxo.amadeus.modelo.EventoMensagem;
import com.paradoxo.amadeus.modelo.Sentenca;
import com.paradoxo.amadeus.service.GravaHistoricoService;
import com.paradoxo.amadeus.util.Preferencias;
import com.paradoxo.amadeus.util.voz.EscutadaoraService;
import com.paradoxo.amadeus.util.voz.TextoParaVoz;
import com.paradoxo.amadeus.util.voz.VozParaTexto;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import static com.paradoxo.amadeus.enums.ItemEnum.ITEM_LOAD;
import static com.paradoxo.amadeus.util.Animacoes.animarComFade;
import static com.paradoxo.amadeus.util.Permissao.solicitarPermissaoMicrofone;
import static com.paradoxo.amadeus.util.Toasts.meuToast;
import static com.paradoxo.amadeus.util.Toasts.meuToastLong;
import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;
import static com.paradoxo.amadeus.util.Util.esconderTeclado;
import static com.paradoxo.amadeus.util.Validadora.validarTexto;

public class MainActivity extends AppCompatActivity implements DialogSimples.FragmentDialogInterface {
    Acionadora acionadora;
    ImageView botoaEnviar;
    TextoParaVoz textoParaVoz;
    Processadora processadora;
    EditText mensagemEditText;
    VozParaTexto vozParaTexto;
    RecyclerView recyclerView;
    String tag_mic, tag_enviar;
    TextView textoOuvidoTextView;
    long idUltimoHistoricoGravado;
    Intent escutadaoraServiceIntent;
    static AdapterMaisDeUmItem adapter;
    SlidingPaneLayout menuSlidingPaneLayout;

    public static final String PREF_ID_ULTIMO_UPLOAD = "idUltimoUpload";
    public static final String PREF_UPLOAD_DADOS_AUTORIZADO = "upload_dados_autorizado";

    boolean emTeste = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configurarServicosSegundoPlano();
        configurarIterface();
    }

    private void configurarServicosSegundoPlano() {
        escutadaoraServiceIntent = new Intent(this, EscutadaoraService.class);
        vozParaTexto = new VozParaTexto(getApplicationContext());
        textoParaVoz = new TextoParaVoz(getApplicationContext());

        processadora = new Processadora(this);
        acionadora = new Acionadora(this);
    }

    private void configurarIterface() {
        menuSlidingPaneLayout = findViewById(R.id.menuSlidingPaneLayout);
        textoOuvidoTextView = findViewById(R.id.textoOuvidoTextView);
        mensagemEditText = findViewById(R.id.mensagemEditText);

        tag_mic = getResources().getString(R.string.tag_mic);
        tag_enviar = getResources().getString(R.string.tag_enviar);
        botoaEnviar = findViewById(R.id.enviarButton);

        configurarNomesIaUsu();
        configurarToolBarBranca(this);
        configurarToolbar();
        configurarRecycler();
        configurarEditeText();
        configurarItensMenu();
        configurarBotaoEnviar();
        configurarBotoaEnviarTeclado();

        LottieAnimationView lottieAnimationView = findViewById(R.id.lottieAnimationView);
        lottieAnimationView.setSpeed(0.5F);

        //trocarLayoutBottom(false);
    }

    private void configurarNomesIaUsu() {
        ((TextView) findViewById(R.id.nomeIaTextView)).setText(Preferencias.getPrefString("nomeIA", this));
        ((TextView) findViewById(R.id.nomeUsuarioTextView)).setText(Preferencias.getPrefString("nomeUsu", this));
    }

    private void configurarBotoaEnviarTeclado() {
        mensagemEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                String entrada = ((EditText) findViewById(R.id.mensagemEditText)).getText().toString();
                validarEntrada(entrada);
                return true;
            }
            return false;
        });
    }


    // Input
    private void validarEntrada(String entrada) {
        if (validarTexto(entrada)) {
            adicionarResposta(new Sentenca(entrada, ItemEnum.USUARIO.ordinal()));
            esconderTeclado(this);
            processadora.processarEntrada(entrada);
            mensagemEditText.getText().clear();
            textoOuvidoTextView.setText(getString(R.string.ouvindo));
        }
    }

    // Output texto
    @Subscribe
    public void adicionarResposta(Sentenca sentenca) {
        if (adapter.getItens().size() != 0 && adapter.getItens().get(adapter.getItemCount() - 1).getTipo_item() == ITEM_LOAD.ordinal()) {
            adapter.remove(adapter.getItemCount() - 1);
            recyclerView.smoothScrollToPosition(adapter.getItemCount());
        }

        SentencaDAO sentencaDAOHistorico = new SentencaDAO(this, true);
        idUltimoHistoricoGravado = sentencaDAOHistorico.inserir(sentenca);
        sentenca.setId(String.valueOf(idUltimoHistoricoGravado));

        adapter.add(sentenca);

        if (sentenca.getTipo_item() == ItemEnum.USUARIO.ordinal()) {
            adapter.add(new Sentenca(ITEM_LOAD.ordinal()));
        } else {
            textoParaVoz.configurarFalaIA(sentenca.getRespostas().get(0));
        }

        new Handler().postDelayed(() -> recyclerView.smoothScrollToPosition(adapter.getItemCount()), 250);
    }

    // Output voz
    @Subscribe
    public void lidarTextoOuvido(EventoMensagem eventoMensagem) {
        textoOuvidoTextView.setText(eventoMensagem.getMensagem());

        if (eventoMensagem.getJaTerminou()) {
            trocarLayoutBottom(true);
            validarEntrada(eventoMensagem.getMensagem());
        }

        int codErro = eventoMensagem.getCodErro();
        if (codErro != 0) {
            trocarLayoutBottom(true);

            if (codErro != SpeechRecognizer.ERROR_SPEECH_TIMEOUT && codErro != SpeechRecognizer.ERROR_NO_MATCH) {
                meuToast(eventoMensagem.getNomeErro(), getApplicationContext());
            }
        }
    }

    public void iniciarEscutadoraService() {
        startService(escutadaoraServiceIntent);
    }

    public void pararEscutadoraService() {
        stopService(escutadaoraServiceIntent);
    }

    private void configurarEditeText() {
        mensagemEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String textoDaBusca = editable.toString();
                trocarIconeBotaoEnviar(textoDaBusca);
            }
        });
    }

    private void configurarBotaoEnviar() {
        botoaEnviar.setOnClickListener(view -> {
            String tag = botoaEnviar.getTag().toString();

            if (tag.equals(tag_enviar)) {
                String entrada = ((EditText) findViewById(R.id.mensagemEditText)).getText().toString();
                validarEntrada(entrada);
            } else {
                trocarLayoutBottom(false);
            }
        });
    }

    private void configurarItensMenu() {
/*        findViewById(R.id.configPrimariaLayout).setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), ConfigPrimariaActivity.class)));

        findViewById(R.id.aprendizadoLayout).setOnClickListener(view -> startActivity(new Intent(this, AprendizActivity.class)));

        findViewById(R.id.vozLayout).setOnClickListener(view -> startActivity(new Intent(this, VozConfigActivity.class)));

        findViewById(R.id.sobreLayout).setOnClickListener(view -> startActivity(new Intent(this, SobreActivity.class)));*/

        configurarClickItem(R.id.configPrimariaLayout, new ConfigPrimariaActivity());
        configurarClickItem(R.id.aprendizadoLayout, new AprendizActivity());
        configurarClickItem(R.id.vozLayout, new VozConfigActivity());
        configurarClickItem(R.id.sobreLayout, new SobreActivity());
    }

    private void configurarClickItem(int view, Activity activity) {
        findViewById(view).setOnClickListener(view1 -> startActivity(new Intent(MainActivity.this, activity.getClass())));
    }


    private void trocarLayoutBottom(boolean tipoTexto) {
        solicitarPermissaoMicrofone(this);
        LinearLayout layoutEscrevendo = findViewById(R.id.layoutEscrevendo);
        LinearLayout layoutOuvindo = findViewById(R.id.layoutOuvindo);

        if (tipoTexto) {
            pararEscutadoraService();

            animarComFade(layoutOuvindo, false);
            animarComFade(layoutEscrevendo, true);
        } else {
            iniciarEscutadoraService();
            esconderTeclado(MainActivity.this);

            animarComFade(layoutEscrevendo, false);
            animarComFade(layoutOuvindo, true);

            findViewById(R.id.layoutOuvindo).setOnClickListener(view -> trocarLayoutBottom(true));
        }
    }

    private void trocarIconeBotaoEnviar(String textoDaBusca) {
        String tag = botoaEnviar.getTag().toString();

        if (textoDaBusca.trim().isEmpty()) {
            if (tag.equals(tag_enviar)) {
                botoaEnviar.setImageResource(R.drawable.ic_mic);
                botoaEnviar.setTag(tag_mic);
            }
        } else {
            if (tag.equals(tag_mic)) {
                botoaEnviar.setImageResource(R.drawable.ic_seta_mais);
                botoaEnviar.setTag(tag_enviar);
            }
        }
    }

    private void configurarToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(view -> {
            if (menuSlidingPaneLayout.isOpen()) {
                menuSlidingPaneLayout.closePane();
            } else {
                menuSlidingPaneLayout.openPane();
            }
        });

        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case (R.id.action_add): {
                    startActivity(new Intent(getApplicationContext(), EditarSentencaActivity.class));
                    break;
                }

                case (R.id.action_aprendizado): {
                    startActivity(new Intent(getApplicationContext(), AprendizActivity.class));
                    break;
                }

                case (R.id.action_sobre): {
                    startActivity(new Intent(getApplicationContext(), SobreActivity.class));
                    break;
                }
            }
            return false;
        });
    }

    private void configurarRecycler() {
        recyclerView = findViewById(R.id.recyclerView);
        List<Sentenca> sentencas = new ArrayList<>();
        adapter = new AdapterMaisDeUmItem(sentencas);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((view, item, pos) -> {
            boolean itemTipoIa = item.getTipo_item() != ItemEnum.IA.ordinal();
            if (itemTipoIa && item.getAcao() != AcaoEnum.SEM_ACAO && item.getAcao() != null) {
                acionadora.tratarAcao(new Acao(item.getAcao()), item.getRespostas().get(0));
            } else {
                boolean itemTipoUsuario = item.getTipo_item() == ItemEnum.USUARIO.ordinal();
                if (!itemTipoIa || itemTipoUsuario) {
                    mensagemEditText.setText(item.getRespostas().get(0));
                    mensagemEditText.setSelection(mensagemEditText.length());
                }
            }
        });
        adapter.setOnLongClickListener((view, position, sentenca) -> {
            vibrar();
            abrirDialogSimples(position);
        });

        carregaSentencaBanco(this);
    }

    private static void atualizarRecycler(List<Sentenca> sentencas, Activity context) {
        // adapter.addAll(gerarListaTeste(context));
        adapter.addAll(sentencas);

        Inicia inicia = new Inicia(context);
        inicia.despertar();

    }

    private static List<Sentenca> gerarListaTeste(Activity context) {
        AcaoDAO acaoDAO = new AcaoDAO(context);
        List<Acao> acoes = acaoDAO.getAcoes();

        List<Sentenca> itens = new ArrayList<>();

        for (Acao acao : acoes) {
            Sentenca sentenca = new Sentenca("Gatilhos: " + acao.getGatilhos().toString(), AcaoEnum.SEM_ACAO);
            sentenca.setTipo_item(ItemEnum.ITEM_CARD.ordinal());

            String s = acao.getAcaoEnum().toString();
            sentenca.addResposta("Comando: " + s);

            itens.add(sentenca);
        }

        itens.add(new Sentenca(5));
        itens.add(new Sentenca("Olá " + Preferencias.getPrefString("nomeUsu", context)));
        itens.add(new Sentenca("Essa é uma versão beta experimental"));
        itens.add(new Sentenca("Aperte o botão de voltar, para abrir o menu"));
        itens.add(new Sentenca("Acima estão alguns comandos e seu gatilhos já dsiponíveis no app"));


        return itens;
    }

    private void vibrar() {
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        long milliseconds = 100;
        if (vibrator != null) {
            vibrator.vibrate(milliseconds);
        }
    }

    public void abrirDialogSimples(int posi) {
        DialogSimples dialog = DialogSimples.newDialog(
                getString(R.string.confirmar_exclusao),
                getString(R.string.tem_certeza_que_deseja_exluir_esse_item),
                posi,
                new int[]{
                        android.R.string.ok,
                        android.R.string.cancel});
        dialog.openDialog(getSupportFragmentManager());
    }

    @Override
    public void onClick(int posi, int which) {
        switch (which) {
            case -1:
                meuToast("Ok posi= " + posi, getApplicationContext());
                SentencaDAO sentencaDAO = new SentencaDAO(getApplicationContext(), true);
                sentencaDAO.excluir(adapter.getItens().get(posi));
                adapter.remove(posi);
                break;

            case -2:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onBackPressed() {

        if (menuSlidingPaneLayout.isOpen()) {
            menuSlidingPaneLayout.closePane();
        } else {
            super.onBackPressed();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        textoParaVoz.interromperFalaIA();

        if (emTeste) {
            meuToastLong("Versão de teste, Firebase desativado", getApplicationContext());
            return;
        }
        try {

            boolean uploadDadosAutorizado = Preferencias.getPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO, this, false);
            if (!uploadDadosAutorizado) return;

            long idUltimoUpload = Preferencias.getPrefLong(PREF_ID_ULTIMO_UPLOAD, this);
            if (idUltimoUpload < idUltimoHistoricoGravado) {
                Intent intentService = new Intent(this, GravaHistoricoService.class);
                intentService.putExtra(PREF_ID_ULTIMO_UPLOAD, idUltimoUpload);
                startService(intentService);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Manter o onDestroy sobrescito aqui, para que as funções do onStop funcionem normalmente
    }

    private static void carregaSentencaBanco(Activity context) {

        new AsyncTask<Void, Void, List<Sentenca>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected List<Sentenca> doInBackground(Void... voids) {
                SentencaDAO sentencaDAO = new SentencaDAO(context, true);
                return sentencaDAO.listar();
            }

            @Override
            protected void onPostExecute(List<Sentenca> sentencas) {
                super.onPostExecute(sentencas);
                atualizarRecycler(sentencas, context);

            }
        }.execute();
    }
}
