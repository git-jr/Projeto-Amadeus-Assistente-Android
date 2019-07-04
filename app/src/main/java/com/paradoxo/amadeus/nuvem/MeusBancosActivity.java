package com.paradoxo.amadeus.nuvem;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.GoogleAuthProvider;
import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.adapter.AdapterBancosOnline;
import com.paradoxo.amadeus.dao.AutorDAO;
import com.paradoxo.amadeus.dao.MensagemDAO;
import com.paradoxo.amadeus.dao.daoAuxCopia.MensagemDAOAuxCopia;
import com.paradoxo.amadeus.firebase.MeusBancosFirebase;
import com.paradoxo.amadeus.modelo.Autor;
import com.paradoxo.amadeus.modelo.Banco;
import com.paradoxo.amadeus.modelo.Mensagem;
import com.paradoxo.amadeus.modelo.Usu;
import com.paradoxo.amadeus.retrofit.RetrofitInicializador;
import com.paradoxo.amadeus.util.Arquivo;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeusBancosActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private Arquivo arquivo;
    private boolean uploadEsperandoAprovacao;
    private Button buttonAcaoDialogEscolhaNome;
    private View viewBotaoSheetBancoPadrao;
    private ProgressDialog progressDialogEspera;
    private BottomSheetDialog bottomSheetDialog;
    private AdapterBancosOnline adapterMeusBancos;
    private BottomSheetBehavior bottomSheetBehavior;
    private TextView textViewStatusNomeUsuarioDialog;

    private static final int RC_SIGN_IN = 1000;
    private static final int PERMISSAO_ACESSO_ARMAZENAMENTO = 1001;

    private FirebaseAuth firebaseAuth = null;
    private GoogleApiClient mGoogleApiClient;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meus_bancos);

        arquivo = new Arquivo();

        if (acessoAMemoriaFoiNegado()) {
            solicitarAcessoArmazenamento();
        } else {
            Arquivo.criarPasta(".Amadeus", this);
        }

        iniciarComponentesVisuais();
    }

    private void iniciarComponentesVisuais() {
        View btnSheet = findViewById(R.id.bottonSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(btnSheet);

        configurarToolbar();
        carregarBancos();
        configurarOpcoesLogin();
    }

    private void configurarRecycler(List<Banco> bancos) {
        RecyclerView recyclerViewMeusBancos = findViewById(R.id.recycler);
        adapterMeusBancos = new AdapterBancosOnline(bancos, this);
        recyclerViewMeusBancos.setAdapter(adapterMeusBancos);

        adapterMeusBancos.setOnItemVerMaisClickListener(new AdapterBancosOnline.OnItemVerMaisClickListener() {
            @Override
            public void onItemMaisClickListener(View view, int position, Banco banco) {
            }
        });

        adapterMeusBancos.setOnItemClickListener(new AdapterBancosOnline.OnItemClickListener() {
            @Override
            public void onItemClickListener(View view, int position, Banco banco) {
                if (banco.getIdAutor() != null) {
                    chamarBottomSheetBancoPadrao();
                } else {
                    chamarBottomSheetBancoBaixado(banco, position);
                }
            }
        });
    }

    private void chamarBottomSheetBancoPadrao() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        viewBotaoSheetBancoPadrao = getLayoutInflater().inflate(R.layout.botao_sheet_meus_bancos_padrao, null);
        viewBotaoSheetBancoPadrao.findViewById(R.id.uploadMaterialRippleLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!alguemEstaLogado() || getPrefString("idu").equals(" ")) {
                    // Se ninguém estiver logado, ou se mesmo logado ele o usuário ainda não escolheu um nome único
                    meuToast(getApplicationContext().getString(R.string.login_requirido));
                    chamarActivityContasLogin();
                    uploadEsperandoAprovacao = true;
                } else {
                    if (acessoAMemoriaFoiNegado()) {
                        uploadEsperandoAprovacao = true;
                        solicitarAcessoArmazenamento();
                    } else {
                        liberarUpload();
                    }
                }
            }
        });

        viewBotaoSheetBancoPadrao.findViewById(R.id.enviarButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextNomeBanco = viewBotaoSheetBancoPadrao.findViewById(R.id.nomeBancoEditText);
                final String nomeBanco = String.valueOf(editTextNomeBanco.getText());

                if (!nomeBanco.replace(" ", "").isEmpty()) {
                    firebaseAuth.getCurrentUser().getIdToken(true)
                            // Antes de cada conexão com o Firebase, recuperamos um token que auntenticará a conexão dessa sessão
                            .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                public void onComplete(@NonNull Task<GetTokenResult> task) {
                                    if (task.isSuccessful()) {
                                        String tokenDaSessao = task.getResult().getToken();
                                        MeusBancosFirebase meusBancosFirebase = new MeusBancosFirebase(getApplicationContext());
                                        meusBancosFirebase.fazerUploadBanco(nomeBanco, getPrefString("idu"), tokenDaSessao);
                                    }
                                }
                            });

                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    bottomSheetDialog.dismiss();
                } else {
                    meuToast(getString(R.string.nome_banco_nao_nulo));
                }
            }
        });

        viewBotaoSheetBancoPadrao.findViewById(R.id.informacoesMaterialRippleLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewBotaoSheetBancoPadrao.findViewById(R.id.posUploadButton).getVisibility() != View.VISIBLE) {
                    meuToastX(getString(R.string.banco_interno_em_uso));
                }
            }
        });


        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(viewBotaoSheetBancoPadrao);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            Objects.requireNonNull(bottomSheetDialog.getWindow()).addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);


        bottomSheetDialog.show();
        bottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                bottomSheetDialog = null;
            }
        });
    }

    private void chamarBottomSheetBancoBaixado(final Banco banco, final int position) {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        final View viewBotaoSheetBancoBaixado = getLayoutInflater().inflate(R.layout.botao_sheet_meus_bancos_baixado, null);
        viewBotaoSheetBancoBaixado.findViewById(R.id.adicionarMaterialRippleLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MeusBancosActivity.this);
                builder.setTitle(R.string.adicionar);
                builder.setIcon(R.drawable.ic_adicionar);
                builder.setMessage(getString(R.string.adicionar_esse_banco))
                        .setPositiveButton(getString(R.string.adicionar), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                adicionarBanco(banco.getNome());
                            }
                        }).setNegativeButton(getString(R.string.cancelar), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.create().show();
            }
        });

        viewBotaoSheetBancoBaixado.findViewById(R.id.substituirMaterialRippleLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MeusBancosActivity.this);
                builder.setTitle(R.string.substituir);
                builder.setIcon(R.drawable.ic_substituir);
                builder.setMessage(getString(R.string.dialog_substituir_banco_1) +
                        getString(R.string.dialog_substituir_banco_2) +
                        getString(R.string.dialog_substituir_banco_3))
                        .setPositiveButton(getString(R.string.substituir), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                substituirBanco(banco.getNome());
                            }
                        }).setNegativeButton(getString(R.string.cancelar), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.create().show();
            }
        });

        viewBotaoSheetBancoBaixado.findViewById(R.id.excluirMaterialRippleLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MeusBancosActivity.this);
                builder.setTitle(R.string.excluir);
                builder.setIcon(R.drawable.ic_deletar);
                builder.setMessage(getString(R.string.dialog_excluir_banco__baixado_1) + banco.getNome() + getString(R.string.dialog_excluir_banco__baixado_2))
                        .setPositiveButton(getString(R.string.excluir), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                meuToast(getString(R.string.excluindo_banco));
                                if (arquivo.excluirBanco(banco.getNome())) {
                                    adapterMeusBancos.deletar(banco, position);

                                    meuToast(getString(R.string.banco_excluido));
                                } else {
                                    meuToast(getString(R.string.banco_nao_excluido));
                                }

                            }
                        }).setNegativeButton(getString(R.string.cancelar), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                builder.create().show();

                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                bottomSheetDialog.dismiss();
            }
        });

        viewBotaoSheetBancoBaixado.findViewById(R.id.informacoesMaterialRippleLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewBotaoSheetBancoBaixado.findViewById(R.id.posUploadButton).getVisibility() != View.VISIBLE) {
                    meuToastX(getString(R.string.banco_disponivel_offline));
                }
            }
        });


        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(viewBotaoSheetBancoBaixado);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            Objects.requireNonNull(bottomSheetDialog.getWindow()).addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        bottomSheetDialog.show();
        bottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                bottomSheetDialog = null;
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    private void carregarBancos() {
        final List<Banco> bancos = new ArrayList<>();
        new AsyncTask<Object, Object, Object>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Object doInBackground(Object[] objects) {
                bancos.add(carregarBancoPadrao());
                if (acessoAMemoriaFoiNegado()) return null;
                bancos.addAll(arquivo.listarBancoBaixados());
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                configurarRecycler(bancos);
            }
        }.execute();

    }

    @SuppressLint("StaticFieldLeak")
    private void adicionarBanco(final String nomeBanco) {
        final ProgressDialog[] progressDialogAdicionandoBanco = new ProgressDialog[1];

        new AsyncTask<Object, Object, Boolean>() {
            @Override
            protected void onPreExecute() {
                progressDialogAdicionandoBanco[0] = ProgressDialog.show(MeusBancosActivity.this, getString(R.string.adicionando), getString(R.string.aguarde_um_momento), true, false);
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Object[] objects) {
                try {
                    meuLogE("Banco copiado com sucesso");
                    Arquivo.copiarBDExternoParaInterno(nomeBanco, MeusBancosActivity.this, false);

                    MeusBancosActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialogAdicionandoBanco[0].setMessage(getString(R.string.copiando));
                        }
                    });

                    MensagemDAOAuxCopia mensagemDAOAuxCopia = new MensagemDAOAuxCopia(MeusBancosActivity.this, nomeBanco + ".db");
                    final List<Mensagem> mensagensExterno = mensagemDAOAuxCopia.listarTodas();
                    Log.e("Banco Externo ", mensagensExterno.get(mensagensExterno.size() - 1).getConteudo());

                    MensagemDAO mensagemDAO = new MensagemDAO(MeusBancosActivity.this);
                    List<Mensagem> mensagensInterno = mensagemDAO.listarRespostasCompleto();
                    Log.e("Banco Interno ", mensagensInterno.get(mensagensInterno.size() - 1).getConteudo());
                    int idUltimaMsgInterna = mensagemDAO.listarUltimaMensagem().getId();

                    // Uma vez que tenhamos as mensagens do banco externo em memória vamos gravá-las no banco interno

                    final int tamanhoTotalMsg = mensagensExterno.size();
                    // Inserindo as mensagens sem as respostas primeiro
                    for (final Mensagem msg : mensagensExterno) {
                        mensagemDAO.inserirMensagem(msg);
                        MeusBancosActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialogAdicionandoBanco[0].setMessage(getString(R.string.copiando) + " " + mensagensExterno.indexOf(msg) + " " + getString(R.string.de) + " " + tamanhoTotalMsg);
                            }
                        });
                    }


                    for (final Mensagem msg : mensagensExterno) {
                        // Inserindo as respostas
                        if (msg.getIdResposta() > 0) {
                            int idPergunta = msg.getId() + idUltimaMsgInterna;
                            int idResposta = msg.getIdResposta() + idUltimaMsgInterna;
                            MensagemDAO.inserirRespostaImportada(idPergunta, idResposta);

                            MeusBancosActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialogAdicionandoBanco[0].setMessage(getString(R.string.sincronizando) + " " + mensagensExterno.indexOf(msg) + " " + getString(R.string.de) + " " + tamanhoTotalMsg);
                                }
                            });
                        }
                    }

                    deleteDatabase(nomeBanco + ".db");
                    // Deleta o banco baixado e matém apenas a cópia que está em uso

                    return true;

                } catch (IOException e) {
                    meuLogE("Não foi possível mover o banco");
                    e.printStackTrace();
                    return false;
                }
            }


            @Override
            protected void onPostExecute(Boolean funcionou) {
                super.onPostExecute(funcionou);
                progressDialogAdicionandoBanco[0].dismiss();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                bottomSheetDialog.dismiss();

                if (funcionou) {
                    meuToast(getString(R.string.adicionado_com_sucesso));
                } else {
                    meuToast(getString(R.string.falha_adicao));
                }
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void substituirBanco(final String nomeNovoBanco) {
        final String nomeBDAtual = getNomeBancoAtual();

        new AsyncTask<Boolean, Void, Boolean>() {
            List<Autor> autoreAntigos;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                autoreAntigos = new AutorDAO(MeusBancosActivity.this).listar();
            }

            @Override
            protected Boolean doInBackground(Boolean... funcionou) {
                try {
                    Arquivo.copiarBDExternoParaInterno(nomeNovoBanco, getApplicationContext(), true);
                    Log.e("tag", "Sucesso na cópia do banco");
                    return true;
                } catch (IOException e) {
                    meuToast(getString(R.string.erro_substituir_banco));
                    Log.e("tag", "Erro ao cpopiar o banco");
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean funcionou) {
                super.onPostExecute(funcionou);
                if (funcionou) {
                    meuToast(getString(R.string.substituido_com_sucesso));
                    meuLogE("Gravar no no banco e fazer a classe BDHelper recuprar o banco com o novo nome");
                    setPrefString(nomeNovoBanco, "bdAtual");

                    try {
                        Arquivo.moverBancoAntigoParaBancosBaixados(nomeBDAtual, MeusBancosActivity.this);
                        sicronizarNomeDosAutoresNoNovoBanco(autoreAntigos);

                    } catch (IOException e) {
                        meuLogE("Falha ao mover os arquivos");
                        e.printStackTrace();
                    }
                } else {
                    meuLogE("Não vai rolar");
                }

                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                bottomSheetDialog.dismiss();
            }
        }.execute();
    }

    private void sicronizarNomeDosAutoresNoNovoBanco(List<Autor> autoresAntigos) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putBoolean("bdMudou", true);
        mEditor.putString("nomeIA", autoresAntigos.get(0).getNome());
        mEditor.putString("nomeUsu", autoresAntigos.get(1).getNome());
        mEditor.apply();

        adapterMeusBancos.notifyDataSetChanged();
    }

    private String getNomeBancoAtual() {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        return sharedPreferences.getString("bdAtual", "Amadeus");
    }

    private void liberarUpload() {
        if (viewBotaoSheetBancoPadrao.findViewById(R.id.posUploadButton).getVisibility() != View.VISIBLE) {
            viewBotaoSheetBancoPadrao.findViewById(R.id.informacoesMaterialRippleLayout).setVisibility(View.GONE);
            viewBotaoSheetBancoPadrao.findViewById(R.id.posUploadButton).setVisibility(View.VISIBLE);

            TextView txtDispoOnline = viewBotaoSheetBancoPadrao.findViewById(R.id.disponibilizarBancoOnlineTextView);
            txtDispoOnline.setText(getString(R.string.banco_vair_ficar_online));
            txtDispoOnline.setTextColor(getResources().getColor(R.color.cinza3));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                txtDispoOnline.setTextAppearance(R.style.TextAppearance_AppCompat_Caption);

        } else {
            meuLogE("Definidindo nome do arquivo para upload");
        }
    }

    private Banco carregarBancoPadrao() {
        File file = new File(Environment.getDataDirectory() +
                File.separator + "data" +
                File.separator + getPackageName() +
                File.separator + "databases" +
                File.separator + getNomeBancoAtual() + ".db");

        String nome = file.getName().replace(".db", "");
        String tamanho = arquivo.calcularTamnhoArquivo(file);
        String idAutor = "Padrão";

        JodaTimeAndroid.init(this);
        DateTime ultimaVezAtualizado = new DateTime(file.lastModified());
        String dtAtualizadoExibicao = ultimaVezAtualizado.toString("dd-MM-YYYY HH:mm");

        return new Banco(nome, tamanho, idAutor, dtAtualizadoExibicao, true);
    }

    private void configurarToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_voltar);
        toolbar.setTitle(R.string.meus_bancos);
        toolbar.setTitleTextColor(getResources().getColor(R.color.cinza7));
        setSupportActionBar(toolbar);
    }

    private boolean acessoAMemoriaFoiNegado() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    private void solicitarAcessoArmazenamento() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSAO_ACESSO_ARMAZENAMENTO);
    }

    private void configurarOpcoesLogin() {
        firebaseAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(MeusBancosActivity.this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    private void chamarActivityContasLogin() {
        Intent logarIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(logarIntent, RC_SIGN_IN);
    }

    private void deslogar() {
        meuToast(getString(R.string.deslogando));
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            // Desloga do Google primeiro
            @Override
            public void onResult(@NonNull Status status) {
                FirebaseAuth.getInstance().signOut();
                // Desloga do Firebase depois
            }
        });

    }

    private boolean alguemEstaLogado() {
        FirebaseUser usuarioAtual = firebaseAuth.getCurrentUser();
        return usuarioAtual != null;
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            progressDialogEspera = ProgressDialog.show(MeusBancosActivity.this, "Verificando login", "Aguarde um momento...", true, false);

                            meuLogE("Login realizado com sucesso");

                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                // Assim que o login for conlcuído recuperamos o token do usuário que será usado nas requisções retrofit
                                user.getIdToken(true)
                                        .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                            public void onComplete(@NonNull Task<GetTokenResult> task) {
                                                if (task.isSuccessful()) {
                                                    String tokenDaSessao = task.getResult().getToken();
                                                    meuLogE("O token dessa sessão é: " + tokenDaSessao);

                                                    verificarSeJaLogouAntes(firebaseAuth.getUid(), tokenDaSessao);
                                                    // Após obtermos o token vamos verficiar se esse usuáro já logou antes ou não, e em seguida se não logou prosseguir para a escolha do nome úncio de usuário
                                                }
                                            }
                                        });
                            }
                        } else {
                            meuLogE("Login falhou");
                        }
                    }
                });
    }

    private void verificarSeJaLogouAntes(String firebaseUid, final String tokenDaSessao) {
        Call call = new RetrofitInicializador().getUsuarioService().jaLogouAntes(firebaseUid, tokenDaSessao);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {

                if (response.code() == 200) {
                    String nomeRetornado = String.valueOf(response.body());
                    Log.e("TGA", "Esse usuário já logou antes");
                    meuToast(getString(R.string.ola_novamente) + " " + nomeRetornado);
                    setPrefString(nomeRetornado, "idu");
                }

                if (response.code() == 401) {
                    Log.e("TGA", "Esse usuário nunca fez login antes");

                    firebaseAuth.getCurrentUser().getIdToken(true)
                            .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                public void onComplete(@NonNull Task<GetTokenResult> task) {
                                    if (task.isSuccessful()) {
                                        String tokenDaSessao = task.getResult().getToken();
                                        meuLogE("O token da sessão para dialog é: " + tokenDaSessao);

                                        progressDialogEspera.dismiss();
                                        dialogEscolherNomeUsu(tokenDaSessao);
                                    }
                                }
                            });
                } else {
                    progressDialogEspera.dismiss();
                    if (uploadEsperandoAprovacao) {
                        liberarUpload();
                        uploadEsperandoAprovacao = false;
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                Log.e("Falha", "Ao verificar existencia do usuário");
                meuToast(getString(R.string.erro_login_tente_de_novo));
            }
        });
    }

    private void dialogEscolherNomeUsu(final String tokenDaSessao) {
        final Dialog builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.setContentView(R.layout.dialog_escolha_nome);
        builder.setCancelable(false);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            layoutParams.copyFrom(Objects.requireNonNull(builder.getWindow()).getAttributes());

        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        textViewStatusNomeUsuarioDialog = builder.findViewById(R.id.statusTextView);
        buttonAcaoDialogEscolhaNome = builder.findViewById(R.id.confirmarButton);

        final TextView txtNovoNomeDigitado = builder.findViewById(R.id.escolhaNomeEditText);
        txtNovoNomeDigitado.setText("");

        buttonAcaoDialogEscolhaNome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = textViewStatusNomeUsuarioDialog.getText().toString();
                if (s.equals(getString(R.string.esse_nome_esta_disponivel))) {
                    gravarNome(txtNovoNomeDigitado.getText().toString(), tokenDaSessao);

                    if (uploadEsperandoAprovacao) {
                        liberarUpload();
                        uploadEsperandoAprovacao = false;
                    }

                    builder.dismiss();
                } else {
                    String novoNomeUsuario = txtNovoNomeDigitado.getText().toString().replace(" ", "");
                    verificarDisponibilidadeNome(novoNomeUsuario, builder, tokenDaSessao);
                }
            }
        });

        txtNovoNomeDigitado.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Para evitar tentativas de mudar um nome já aprovado
                buttonAcaoDialogEscolhaNome.setText(getString(R.string.verificar));
                buttonAcaoDialogEscolhaNome.setBackgroundColor(getResources().getColor(R.color.cinza4));
                textViewStatusNomeUsuarioDialog.setText("");
            }
        });

        builder.show();
        builder.getWindow().setAttributes(layoutParams);
    }

    private void verificarDisponibilidadeNome(final String nome, final Dialog builder, String tokenDaSessao) {
        final ProgressBar progresBarVeificandoNome = builder.findViewById(R.id.barraProgressoProgressBar);
        progresBarVeificandoNome.setVisibility(View.VISIBLE);

        Call call = new RetrofitInicializador().getUsuarioService().verificarExistencia(nome, tokenDaSessao);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                progresBarVeificandoNome.setVisibility(View.GONE);
                Log.e("TGA", String.valueOf(response.body()));
                Log.e("TGA", String.valueOf(response.code()));
                Log.e("TGA", call.request().url().toString());

                if (response.code() == 200) {
                    String nomeRetornado = String.valueOf(response.body());
                    if (nomeRetornado.equals("null")) {
                        Log.e("TGA", "Esse usuário ainda NÃO existe");

                        buttonAcaoDialogEscolhaNome.setText(getString(R.string.confirmar));
                        buttonAcaoDialogEscolhaNome.setBackgroundColor(getResources().getColor(R.color.azul_link));

                        textViewStatusNomeUsuarioDialog.setText(getString(R.string.esse_nome_esta_disponivel));
                        textViewStatusNomeUsuarioDialog.setTextColor(getResources().getColor(R.color.azul_link));

                    } else {
                        Log.e("TGA", "Esse usuário já existe");

                        textViewStatusNomeUsuarioDialog.setText(getString(R.string.esse_nome_nao_disponivel));
                        textViewStatusNomeUsuarioDialog.setTextColor(getResources().getColor(R.color.vermelho));

                        buttonAcaoDialogEscolhaNome.setText(getString(R.string.verificar));
                        buttonAcaoDialogEscolhaNome.setBackgroundColor(getResources().getColor(R.color.cinza4));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                progresBarVeificandoNome.setVisibility(View.GONE);
                Log.e("Falha", "Ao verificar existencia do usuário");
            }
        });


    }

    private void gravarNome(final String nomeUsuario, String tokenDaSessao) {
        Call call = new RetrofitInicializador().getUsuarioService().inserir(firebaseAuth.getUid(), new Usu(nomeUsuario), tokenDaSessao);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                meuLogE("Usuário inserido com sucesso");
                meuToast(getString(R.string.gravado_com_sucesso));

                setPrefString(nomeUsuario, "idu");
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                meuLogE("Falha ao inserir Usuário");
                meuToast(getString(R.string.erro_nome_usuario_tente_novamente));
            }
        });

        Call call2 = new RetrofitInicializador().getUsuarioService().registrarIdUnico(nomeUsuario, new Usu(nomeUsuario), tokenDaSessao);
        call2.enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                meuLogE("Nome único inserido com sucesso");

                setPrefString(nomeUsuario, "idu");
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                meuLogE("Falha ao inserir IdUsuUnio");
            }
        });
    }

    private void setPrefString(String textoShared, String nomeShared) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putString(nomeShared, textoShared);
        mEditor.apply();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSAO_ACESSO_ARMAZENAMENTO) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
                meuToastX(getString(R.string.acesso_memoria_negado));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN && resultCode == RESULT_OK) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                assert account != null;
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                meuLogE("Falha ao realizar o login");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        meuLogE("Conexão para o login falhou");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.deslogar) {
            AutorDAO autorDAO = new AutorDAO(this);

            if (autorDAO.listar().get(1).getNome().equals("kirito") || autorDAO.listar().get(1).getNome().equals("Kirito")) {
                meuToastX(getString(R.string.easter_egg_logout_kirito));
                return true;
            } else {
                deslogar();
            }

        } else if (item.getItemId() == R.id.logar) {
            chamarActivityContasLogin();
        } else {
            finish();
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_meus_bancos, menu);
        return true;
    }

    private String getPrefString(String nomeShared) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrefsUsu", MODE_PRIVATE);
        return sharedPreferences.getString(nomeShared, " ");
    }

    public void meuToast(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
    }

    public void meuToastX(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show();
    }

    private void meuLogE(String texto) {
        Log.e("TAG", texto);
    }
}
