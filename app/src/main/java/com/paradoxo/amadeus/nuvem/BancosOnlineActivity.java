package com.paradoxo.amadeus.nuvem;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.adapter.AdapterBancosOnline;
import com.paradoxo.amadeus.firebase.MeusBancosFirebase;
import com.paradoxo.amadeus.modelo.Banco;
import com.paradoxo.amadeus.util.Arquivo;

import java.util.ArrayList;
import java.util.List;

public class BancosOnlineActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 1001;
    private boolean downloadEsperandoAprovacao;
    private String urlDonwloadEspera;
    private String nomeBancoEspera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bancos_online);

        configurarToolbar();
        carregarBanos();

    }

    public void carregarBanos() {
        final ProgressDialog progressDialogCarregandoBanco = ProgressDialog.show(this, getString(R.string.carregando_banco), getString(R.string.aguarde), true, false);

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = firebaseDatabase.getReference("bancosUsu");

        final List<Banco> listaDeBancos = new ArrayList<>();

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Banco novoBanco = snapshot.getValue(Banco.class);
                    listaDeBancos.add(novoBanco);
                }

                progressDialogCarregandoBanco.dismiss();
                configurarRecycler(listaDeBancos);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialogCarregandoBanco.dismiss();
                meuLogE("Erro ao listar os bancos do firebase" + databaseError.getDetails());
                meuToast(getString(R.string.erro_carregar_tente_novamente));
            }

        });
    }

    private void configurarRecycler(List<Banco> bancosOnline) {
        RecyclerView recyclerViewBancos = findViewById(R.id.recycler);
        AdapterBancosOnline adapterBancosOnline = new AdapterBancosOnline(bancosOnline, this);
        recyclerViewBancos.setAdapter(adapterBancosOnline);

        adapterBancosOnline.setOnItemClickListener(new AdapterBancosOnline.OnItemClickListener() {
            @Override
            public void onItemClickListener(View view, int position, Banco banco) {
                dialogBaixarBanco(banco.getUrlDownlaod(), banco.getNome());
            }
        });

        adapterBancosOnline.setOnItemVerMaisClickListener(new AdapterBancosOnline.OnItemVerMaisClickListener() {
            @Override
            public void onItemMaisClickListener(View view, int position, Banco banco) {
            }
        });

        verificarBancosBaixados(bancosOnline, adapterBancosOnline);
    }

    private void verificarBancosBaixados(List<Banco> bancosOnline, AdapterBancosOnline adapterBancosOnline) {
        try {
            Arquivo arquivo = new Arquivo();
            List<Banco> bancosBaixados = arquivo.listarBancoBaixados();

            for (Banco bancoBaixado : bancosBaixados) {
                for (Banco bancoOnline : bancosOnline) {
                    if (bancoBaixado.getNome().equals(bancoOnline.getNome())) {
                        meuLogE("O Banco " + bancoBaixado.getNome() + " já foi baixado");
                        int indiceBancoBaixado = bancosOnline.indexOf(bancoOnline);
                        bancosOnline.get(indiceBancoBaixado).setBaixado(true);
                        adapterBancosOnline.atualizar(indiceBancoBaixado);

                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void dialogBaixarBanco(final String urlDownlaod, final String nomeBanco) {
        AlertDialog.Builder builder = new AlertDialog.Builder(BancosOnlineActivity.this);
        builder.setMessage(getString(R.string.deseja_baixar_esse_banco))
                .setPositiveButton(getString(R.string.baixar), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadFirebase(urlDownlaod, nomeBanco);
                    }
                })
                .setNegativeButton(getString(R.string.cancelar), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        builder.create().show();
    }

    private void downloadFirebase(String urlDnwload, String nomeBanco) {
        if (acessoAoArmazenamentoFoiPermitido()) {
            downloadEsperandoAprovacao = true;
            solicitarAcessoArmazenamento();
            urlDonwloadEspera = urlDnwload;
            nomeBancoEspera = nomeBanco;
        } else {
            MeusBancosFirebase meusBancosFirebase = new MeusBancosFirebase(getApplicationContext());
            meusBancosFirebase.downlaodBanco(urlDnwload, nomeBanco);

            Snackbar snackbar = Snackbar.make(findViewById(R.id.activityBancosOnline), getString(R.string.gerenciar_seus_bancos), Snackbar.LENGTH_LONG);
            snackbar.setAction(this.getString(R.string.acessar), new MeusBancosListenerSnackBar());
            snackbar.setActionTextColor(Color.CYAN);
            snackbar.show();
        }

    }

    private boolean acessoAoArmazenamentoFoiPermitido() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    private void solicitarAcessoArmazenamento() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (downloadEsperandoAprovacao) {
                    // Ativado se no primeiro download o acesso á memória ainda não foi concedido
                    downloadFirebase(urlDonwloadEspera, nomeBancoEspera);

                }
            } else {
                meuToastX(this.getString(R.string.acesso_memoria_para_download));
            }
        }
    }

    private void configurarToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_voltar);
        toolbar.setTitle(R.string.nuvem);
        toolbar.setTitleTextColor(getResources().getColor(R.color.cinza7));
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_nuvem, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.opcoes) {
            Intent meusBancosONline = new Intent(this, MeusBancosActivity.class);
            startActivity(meusBancosONline);
        } else {
            finish();
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarBanos();
    }

    private class MeusBancosListenerSnackBar implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(BancosOnlineActivity.this, MeusBancosActivity.class);
            startActivity(intent);
        }
    }

    private void meuLogE(String texto) {
        Log.e("LOG", texto);
    }

    public void meuToast(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
    }

    public void meuToastX(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show();
    }

}
