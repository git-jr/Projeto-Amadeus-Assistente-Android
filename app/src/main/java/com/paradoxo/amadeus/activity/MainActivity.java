package com.paradoxo.amadeus.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.navigation.NavigationView;
import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.adapter.AdapterMensagensHome;
import com.paradoxo.amadeus.dao.AutorDAO;
import com.paradoxo.amadeus.dao.BDGateway;
import com.paradoxo.amadeus.dao.MensagemDAO;
import com.paradoxo.amadeus.modelo.Autor;
import com.paradoxo.amadeus.modelo.Mensagem;
import com.paradoxo.amadeus.nuvem.BancosOnlineActivity;
import com.paradoxo.amadeus.util.Arquivo;
import com.paradoxo.amadeus.util.Chatbot;
import com.paradoxo.amadeus.util.Classificador;
import com.paradoxo.amadeus.util.SpeechToText;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private TextToSpeech textToSpeech;
    private boolean envioViaVoz = false;

    private SpeechToText speechToText;
    private boolean escutando = false;
    private boolean sttJaUsado = true;
    @SuppressLint("StaticFieldLeak")
    private static EditText editTextMsgUsu;

    Chatbot chatbot;
    private RecyclerView recyclerViewMensagens;
    private static Autor autorIA, autorUsuario;
    private AdapterMensagensHome adapterMensagensHome;
    private ProgressDialog dialogCarregandoBancoInicial;
    private boolean vozIaAtiva, vozIaAtivaMesmoSemResposta;

    private final int RESULT_CODE_LOAD_ACTIVITY = 1000;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inicializarConfiguracoes();

    }

    private void inicializarConfiguracoes() {

        if (!getPrefBool("bancoInserido")) {
            realizarPrimeiroLoad();
        } else {

            dialog_changelog();
            List<Autor> autores = carregarInformacoesIniciais();

            carregarNomeUsuarioIA(autores);

            chatbot = new Chatbot(this, autores);
            if (!getPrefBool("bdMudou")) {
                // Evita que uma saudação seja feita com o nome de usuário errado durante a transição de um banco baixado
                chatbot.despertar();
            }

            inicializarComponentes();

        }
    }

    private void inicializarComponentes() {
        configurarRecycler();
        solicitarPermissaoMicrofone();

        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        configurarCaixaDeDigitacaoMensagem();
        configurarBotaoEnvioMensagem();

        verificarModoVozAtivado();
        AtivarComandosPorVoz();
        configurarFalaIA();
        iniciarAnuncio();
    }


    private void configurarRecycler() {
        MensagemDAO mensagemDAO = new MensagemDAO(this);
        final List<Mensagem> mensagems = mensagemDAO.listarUltimasInseridas();

        recyclerViewMensagens = findViewById(R.id.recycler);

        adapterMensagensHome = new AdapterMensagensHome(mensagems);
        recyclerViewMensagens.setAdapter(adapterMensagensHome);
        adapterMensagensHome.setOnLongClickListener(new AdapterMensagensHome.OnLongClickListener() {
            @Override
            public void onLongClickListener(View view, int position, Mensagem mensagem) {
                vibrar();

                Intent alterarRespostasActivity = new Intent(MainActivity.this, AlteraRespostasActivity.class);
                alterarRespostasActivity.putExtra("pergunta_selecionada", mensagem.getConteudo());
                alterarRespostasActivity.putExtra("resposta_selecionada", mensagem.getConteudo_resposta());
                alterarRespostasActivity.putExtra("id_selecionado", String.valueOf(position));
                startActivity(alterarRespostasActivity);
            }
        });

        adapterMensagensHome.setOnItemClickListener(new AdapterMensagensHome.OnItemClickListener() {
            @Override
            public void onItemClickListener(View view, int position, Mensagem mensagem) {

                String nomeAppBusca = identificarApp(mensagems.get(position - 1).getConteudo());
                if (!nomeAppBusca.isEmpty()) {
                    Intent intenteBuscarApp = new Intent(Intent.ACTION_VIEW);
                    intenteBuscarApp.setData(Uri.parse("https://play.google.com/store/search?q=" + nomeAppBusca + "&c=apps"));
                    startActivity(intenteBuscarApp);
                }
            }
        });
    }

    public String identificarApp(String entrada) {
        List<String> comandosAbrirApp = Arrays.asList("abrir", "abrir aplicativo", "abrir app", "iniciar o aplicativo");
        String nomeApp = "";

        for (String comandoEmSi : comandosAbrirApp) {
            if (entrada.contains(comandoEmSi)) {
                nomeApp = entrada.replace(comandoEmSi, "");
                break;
            }
        }
        return nomeApp;
    }

    private void realizarPrimeiroLoad() {
        dialogCarregandoBancoInicial = ProgressDialog.show(this, "Carregando banco inicial", "Esse processo só precisa ser feito uma vez\nAguarde um momento...", true, false);
        dialogCarregandoBancoInicial.show();

        PrimeiroLoad primeiroLoad = new PrimeiroLoad();
        primeiroLoad.execute();
        // Vai carregar o banco de dados pré pronto e depois chama a activity de login
    }

    private void configurarCaixaDeDigitacaoMensagem() {
        editTextMsgUsu = findViewById(R.id.mensagemUsuarioTextView);
        editTextMsgUsu.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Button btn = findViewById(R.id.enviarButton);
                if (editTextMsgUsu.getText().length() > 0 && !escutando) {
                    btn.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_enviar));
                    envioViaVoz = true;

                } else {
                    btn.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_microfone));
                    envioViaVoz = false;
                }
            }

            public void afterTextChanged(Editable editable) {
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configurarBotaoEnvioMensagem() {
        final Button btnSend = findViewById(R.id.enviarButton);
        btnSend.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN && !envioViaVoz) {
                    vibrar(); // Só pra dar a impressão de clique mesmo
                    speechToText.backgroundVoiceListener.run(); // Se o botão for pressionado o reconhecimento de voz começa
                    escutando = true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP && !envioViaVoz) {
                    vibrar();
                    speechToText.backgroundVoiceListener.interrupt(); // Se o botão for solto o reconhecimento para
                    escutando = false;
                }
                obterResposta();
                return true;
            }
        });
    }

    private void vibrar() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long milliseconds = 50;
        if (vibrator != null) {
            vibrator.vibrate(milliseconds);
        }
    }

    private void configurarFalaIA() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.getDefault());

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Lingua não suportada");
                    } else {
                        String estiloDeVozPadrao = getPrefString("estiloDeVozPadrao");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            textToSpeech.setVoice(new Voice(estiloDeVozPadrao, Locale.getDefault(), 1, 1, false, null));
                        }
                    }

                }
            }
        });
    }

    private void AtivarComandosPorVoz() {
        if (getSttJaUsado()) {
            speechToText = new SpeechToText(this);
            setPrimeiraVezStt(false);
        }
    }

    private void solicitarPermissaoMicrofone() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1001);
            }
        }
    }

    private List<Autor> carregarInformacoesIniciais() {
        AutorDAO autorDAO = new AutorDAO(this);
        return autorDAO.listar();

    }

    private void verificarModoVozAtivado() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        vozIaAtivaMesmoSemResposta = sharedPreferences.getBoolean("switch_voz_ia_sem_resp", true);
        vozIaAtiva = sharedPreferences.getBoolean("switch_voz_ia", true);
    }

    private void iniciarAnuncio() {

        AdView adView = findViewById(R.id.adView);
        boolean emTeste = true;

        if (true) {
            // Isso é para evitar que anúncioos sejam mostrados durante a fase de desenvolvimento
            adView.setVisibility(View.GONE);
        } else {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }


    public void dialog_changelog() {
        String versao = "";
        try {
            PackageInfo pInfo;
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versao = getString(R.string.versao) + " " + pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        if (versao.equals(getPrefString("versaoAtt")))
            return;

        final Dialog builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.setContentView(R.layout.dialog_changelog);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            layoutParams.copyFrom(Objects.requireNonNull(builder.getWindow()).getAttributes());

        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            Objects.requireNonNull(builder.getWindow()).setAttributes(layoutParams);

        ((TextView) builder.findViewById(R.id.tituloTextView)).setText(versao);
        ((TextView) builder.findViewById(R.id.conteudoTextView)).setText(getText(R.string.lista_mundacas_versao));

        final String finalVersaoTemp = versao;
        (builder.findViewById(R.id.fecharButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.dismiss();
                setPrefBool("dailogNewVersion", true);
                setPrefString(finalVersaoTemp);
            }
        });


        (builder.findViewById(R.id.irParaYouTubeButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentPlaylistAmadeusYT = new Intent(Intent.ACTION_VIEW);
                intentPlaylistAmadeusYT.setData(Uri.parse(getString(R.string.link_playlist_amadeus_yt)));
                startActivity(intentPlaylistAmadeusYT);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            Objects.requireNonNull(builder.getWindow()).setAttributes(layoutParams);

        builder.show();
    }

    private void setPrefString(String texto) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putString("versaoAtt", texto);
        mEditor.apply();
    }

    private String getPrefString(String nomeShared) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        return sharedPreferences.getString(nomeShared, "");
    }

    private void setPrefBool(String nomeShared, boolean valor) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putBoolean(nomeShared, valor);
        mEditor.apply();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();


        switch (id) {
            case R.id.nav_perfil: {
                carregarNomeUsuarioIA(carregarInformacoesIniciais());
                // Atualizando os nomes para o caso de uma importação ter sido feita via Qpython

                Intent loadActivity = new Intent(this, LoadActivity.class);
                loadActivity.putExtra("nomeUsu", autorUsuario.getNome());
                loadActivity.putExtra("nomeIA", autorIA.getNome());
                startActivityForResult(loadActivity, RESULT_CODE_LOAD_ACTIVITY);
                break;
            }

            case R.id.nav_editarRespostas: {
                Intent editRespActivity = new Intent(MainActivity.this, ListarRespostasActivity.class);
                startActivity(editRespActivity);
                break;
            }

            case R.id.nav_configuracoes: {
                Intent settingsActivity = new Intent(MainActivity.this, ConfiguracoesActivity.class);
                startActivity(settingsActivity);
                break;
            }

            case R.id.nav_nuvem: {
                Intent nuvemActivity = new Intent(this, BancosOnlineActivity.class);
                startActivity(nuvemActivity);
                break;
            }


            case R.id.nav_troar_voz: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Intent trocarVozActivity = new Intent(this, TrocarVozActivity.class);
                    startActivity(trocarVozActivity);
                } else {
                    meuToast(getString(R.string.recurso_nao_disponivel));
                }
                break;

            }

            case R.id.nav_compartilhar: {
                Intent intentCompartilharAmadeus = new Intent(Intent.ACTION_SEND);
                intentCompartilharAmadeus.setType("text/plain");
                String text = getString(R.string.texto_para_compartilhamento);

                intentCompartilharAmadeus.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(intentCompartilharAmadeus, getString(R.string.compartilhar_com)));
                break;
            }

            case R.id.nav_relatarBug: {
                String mailto;

                try {
                    String versaoAndroidUsuario = "Versão do Android: " + Build.VERSION.RELEASE;
                    String modeloCelularUsuario = "Modelo do celular:" + " " + Build.MODEL;

                    mailto = "mailto:" + getString(R.string.email_suporte) +
                            "?cc=" + "" +
                            "&subject=" + Uri.encode(String.valueOf(this.getText(R.string.report_bug))) +
                            "&body=" + Uri.encode(modeloCelularUsuario + "\n" + versaoAndroidUsuario + "\n" +
                            this.getText(R.string.report_bug_digite) + "\n\n");

                } catch (Exception e) {
                    mailto = "mailto:" + getString(R.string.email_suporte) +
                            "?cc=" + "" +
                            "&subject=" + Uri.encode(String.valueOf(this.getText(R.string.report_bug))) +
                            "&body=" + Uri.encode(this.getText(R.string.report_bug_digite) + "\n\n");
                }

                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse(mailto));

                try {
                    startActivity(emailIntent);
                } catch (ActivityNotFoundException e) {
                    meuToast(getString(R.string.falha_envio_email));
                }
                break;
            }

            case R.id.nav_sugerirIdeia: {
                String mailto = "mailto:" + getString(R.string.email_suporte) +
                        "?cc=" + "" +
                        "&subject=" + Uri.encode(String.valueOf(this.getText(R.string.report_ideia))) +
                        "&body=" + Uri.encode(this.getText(R.string.report_ideia_digite) + "\n\n");

                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse(mailto));

                try {
                    startActivity(emailIntent);
                } catch (ActivityNotFoundException e) {
                    meuToast(getString(R.string.falha_envio_email));
                }

            }

            case R.id.nav_sobre: {
                Intent sobreActivity = new Intent(this, SobreActivity.class);
                startActivity(sobreActivity);
                break;
            }


        }

        Log.e("Saiu", "Mas apassou aqui");
        item.setCheckable(false);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_CODE_LOAD_ACTIVITY) {
            if (resultCode == RESULT_OK && null != data) {
                carregarNomeUsuarioIA(carregarInformacoesIniciais()); // Para a possibilidade dos nomes terem mudado
            }
        }
    }

    @Override
    protected void onDestroy() {
        interromperFalaIA();
        super.onDestroy();
    }

    private void interromperFalaIA() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean bdMudou = getPrefBool("bdMudou");
        if (bdMudou) {
            // Esse bloco é ativado no caso de algum banco ter sido baixado
            try {
                Autor objAutorUsu = new Autor(2, getPrefString("nomeUsu"));
                Autor objAutorIa = new Autor(1, getPrefString("nomeIA"));

                AutorDAO autorDAO = new AutorDAO(this);
                autorDAO.alterar(objAutorIa);
                autorDAO.alterar(objAutorUsu);

                setPrefBool("bdMudou", false);
                setPrefBool("reiniciado", false);

                meuToastX(getString(R.string.banco_instalado));

            } catch (Exception e) {
                meuToastX(getString(R.string.reiniciando_app));
                Log.e("TAG", getString(R.string.falha_ao_copiar_banco));
                e.printStackTrace();
                reiniciarApp();
            }
        }
    }

    private void reiniciarApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        (this).finish();
        Runtime.getRuntime().exit(0);
    }

    private boolean getPrefBool(String nomePref) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        return sharedPreferences.getBoolean(nomePref, false);
    }

    private void carregarNomeUsuarioIA(List<Autor> autores) {
        autorIA = new Autor();
        autorUsuario = new Autor();

        autorIA = autores.get(0);
        autorUsuario = autores.get(1);
    }

    public boolean getSttJaUsado() {
        return sttJaUsado;
    }

    public EditText getEditText_msgUsu() {
        return editTextMsgUsu;
    }

    public void setPrimeiraVezStt(boolean primeiraVezStt) {
        this.sttJaUsado = primeiraVezStt;
    }

    public static void setEditTextMsgUsu(String texto) {
        editTextMsgUsu.setText(texto);
    }

    public void obterResposta() {
        Classificador classificador = new Classificador();
        String entradaUsuario = String.valueOf(getEditText_msgUsu().getText());
        entradaUsuario = classificador.normalizar(entradaUsuario);

        if (entradaUsuario.length() > 0) {
            prepararObtencaoDeResposta(entradaUsuario);
            BuscaInteracao buscaInteracao = new BuscaInteracao();
            buscaInteracao.setEntradaUsuario(entradaUsuario);
            buscaInteracao.execute();

        }
    }

    public final void addInterecao(Mensagem mensagem) {
        if (mensagem.ehUmaResposta()) {
            if (vozIaAtiva) {
                falar(mensagem.getConteudo());
            }
        } else {
            if (vozIaAtivaMesmoSemResposta) {
                falar(mensagem.getConteudo());
            }
        }

        addMensagemListaEBanco(mensagem);
    }

    public final void addInterecaoProgresso() {
        adapterMensagensHome.add(new Mensagem(true, autorIA));
        recyclerViewMensagens.smoothScrollToPosition(adapterMensagensHome.getItemCount());
    }

    public final void removeInterecaoProgresso() {
        adapterMensagensHome.remove();
    }

    private void prepararObtencaoDeResposta(String entrada) {
        Mensagem objMensagem = new Mensagem(entrada, autorUsuario);
        addMensagemListaEBanco(objMensagem);
        esconderTeclado();
        editTextMsgUsu.setText("");
    }

    private void addMensagemListaEBanco(Mensagem objMensagem) {
        MensagemDAO objMsgDAO = new MensagemDAO(this);
        objMsgDAO.inserirMensagem(objMensagem);
        adapterMensagensHome.add(objMensagem);
        recyclerViewMensagens.smoothScrollToPosition(adapterMensagensHome.getItemCount());
    }

    public void chamarActivitySobre(View view) {
        Intent sobreActivity = new Intent(this, SobreActivity.class);
        startActivity(sobreActivity);
    }

    public void falar(String texto) {
        textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void meuToast(String texto) {
        Toast.makeText(MainActivity.this, texto, Toast.LENGTH_SHORT).show();
    }

    public void meuToastX(String texto) {
        Toast.makeText(MainActivity.this, texto, Toast.LENGTH_LONG).show();
    }

    public void esconderTeclado() {
        View view = this.getCurrentFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            assert view != null;
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class PrimeiroLoad extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
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
                meuToast("Falaha ao copiar o banco de dados");
            }
        }
    }

    private void cadastrarNomes() {
        Log.e("tag", "Cópia do banco finalizada");
        dialogCarregandoBancoInicial.dismiss();

        Intent loadActivity = new Intent(getApplicationContext(), LoadActivity.class);
        startActivity(loadActivity);
        finish();
    }

    @SuppressLint("StaticFieldLeak")
    public class BuscaInteracao extends AsyncTask<Void, Void, Void> {
        private String entradaUsuario;
        private Mensagem resposta;

        private void setEntradaUsuario(String entradaUsuario) {
            this.entradaUsuario = entradaUsuario;
        }

        @Override
        protected void onPreExecute() {
            addInterecaoProgresso();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            resposta = chatbot.gerarRespoosta(entradaUsuario);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            removeInterecaoProgresso();
            addInterecao(resposta);

        }
    }
}

/*
    @SuppressLint("StaticFieldLeak")
    private void acionarRespostaAssincrona() {
        // Método para adição de mensagens na tela de tempos em tempos através de um proesso em Background;
        // Deverá ser mais útil quando o funcionamento da IA em segundo plano for implementado

        AsyncTask TesteAsK = new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object[] objects) {

                for (int i = 1000; i != 5000; i += 100) {

                    try {
                        Thread.sleep(i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e("Faaha ", "Na Thread de espera");

                    }

                    final int finalI = i;
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Mensagem objMensagem = new Mensagem(finalI + "º Nova mensagem", autorIA);
                            addInterecao(objMensagem);
                        }
                    });
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
            }
        };

        TesteAsK.execute();
    }
*/
