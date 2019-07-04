package com.paradoxo.amadeus.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.dao.AutorDAO;
import com.paradoxo.amadeus.dao.MensagemDAO;
import com.paradoxo.amadeus.modelo.Mensageiro;
import com.paradoxo.amadeus.util.Arquivo;

import java.util.Objects;

public class QPythonActivity extends AppCompatActivity {

    private int estado = 1; // 0-Não liberado; 1-Agurado; 2-Liberado (Deve ser possoivel fazer isso apenas com booleans depois)
    private String caminhoArquivosQPython;
    private static final int COD_REQUISICAO_TXT = 1000;
    private static final int PERMISSAO_ACESSO_ARMAZENAMENTO = 1001;
    public static final String DOXO_INFOS_TXT = "qpython/IA Doxo/infos.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qpython);

        verificarExistenciaArquivoInfos();
        caminhoArquivosQPython = Environment.getExternalStorageDirectory() + "/" + DOXO_INFOS_TXT;

    }

    private void verificarExistenciaArquivoInfos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            String requiredPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if (this.checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_GRANTED) {
                Arquivo aqr = new Arquivo();
                if (!aqr.arquivoExiste(DOXO_INFOS_TXT)) {

                    ((TextView) findViewById(R.id.caminhoImportacaoTextView)).setText(String.valueOf(this.getText(R.string.arquivo_nao_loc)));

                    estado = 0;
                }

            } else {
                requestPermissions(new String[]{requiredPermission}, PERMISSAO_ACESSO_ARMAZENAMENTO);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSAO_ACESSO_ARMAZENAMENTO && grantResults[0] != PackageManager.PERMISSION_GRANTED) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(this.getText(R.string.realmente_fazer_isso));
            builder.setMessage(this.getText(R.string.acesso_requerido));
            builder.setCancelable(false);

            builder.setPositiveButton(String.valueOf(this.getText(R.string.ok)), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    verificarExistenciaArquivoInfos();
                }
            });


            builder.setNegativeButton(String.valueOf(this.getText(R.string.cancelar)), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    QPythonActivity.this.finish();
                }
            });

            AlertDialog alerta = builder.create();
            alerta.show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == COD_REQUISICAO_TXT && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (Objects.requireNonNull(data.getData()).getLastPathSegment().equals("infos.txt")) {
                    // Se o arquivo se parecer com o que queremos:
                    caminhoArquivosQPython = data.getData().getPath();

                    ((TextView) findViewById(R.id.caminhoImportacaoTextView)).setText(caminhoArquivosQPython);

                    estado = 1;

                } else {
                    Toast.makeText(this, this.getText(R.string.arquivo_nao_compat), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void abrirSeletorArquivosInfo(View view) {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("txt/*");

        Intent intentSeletorArquivos = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.getMediaScannerUri());
        Intent chooserIntent = Intent.createChooser(getIntent, getString(R.string.selecione_infos_txt));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{intentSeletorArquivos});
        startActivityForResult(chooserIntent, COD_REQUISICAO_TXT);
    }

    public void carregarArquivoInfos(View view) {
        switch (estado) {
            case (0): {
                Toast.makeText(this, this.getText(R.string.arquivo_nao_sele), Toast.LENGTH_LONG).show();
                return;
            }
            case (1): {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(this.getText(R.string.realmente_fazer_isso));
                builder.setMessage(this.getText(R.string.tem_certeza));

                builder.setPositiveButton(String.valueOf(this.getText(R.string.ok)), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        importarESobrescreverDadosDobanco();
                    }
                });

                builder.setNegativeButton(String.valueOf(this.getText(R.string.cancelar)), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        finish();
                    }
                });

                AlertDialog alerta = builder.create();
                alerta.show();
            }
        }
    }


    public void importarESobrescreverDadosDobanco() {
        TextView textViewPorcentagem = findViewById(R.id.porcentagemTextView);
        TextView textViewStatus = findViewById(R.id.statusTextView);

        Carregando carregando = new Carregando();
        carregando.setProcentagem(textViewPorcentagem);
        carregando.setTextViewStatus(textViewStatus);

        Mensageiro mensageiro = Arquivo.importarQpython(caminhoArquivosQPython, this);
        if (mensageiro.mensagem != null) {
            carregando.setMensageiro(mensageiro);
            carregando.execute();
        } else {
            meuToast(getString(R.string.erro_importacao));
        }
    }

    public void meuToast(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("StaticFieldLeak")
    private class Carregando extends AsyncTask<Void, String, Void> {

        TextView textViewPorcentagem = findViewById(R.id.porcentagemTextView);
        TextView textViewStatus = findViewById(R.id.statusTextView);
        ScrollView layoutPrincipal = findViewById(R.id.layoutPrincipal);
        LinearLayout layoutSecundario = findViewById(R.id.layoutSecundario);

        Mensageiro mensageiro;

        private void setMensageiro(Mensageiro mensageiro) {
            this.mensageiro = mensageiro;
        }

        private void setProcentagem(TextView textView) {
            this.textViewPorcentagem = textView;
        }

        private void setTextViewStatus(TextView textViewStatus) {
            this.textViewStatus = textViewStatus;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            layoutPrincipal.setVisibility(View.GONE);
            layoutSecundario.setVisibility(View.VISIBLE);
            MensagemDAO.deletarTodasMensagens();
            // Limpa todas as mensagens antigas antes de adicionar as novas
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                AutorDAO autorDAO = new AutorDAO(getApplicationContext());
                autorDAO.alterar(mensageiro.mensagem.get(mensageiro.mensagem.size() - 1).getAutor());
                autorDAO.alterar(mensageiro.mensagem.get(mensageiro.mensagem.size() - 2).getAutor());


                for (int i = 0; i < mensageiro.mensagem.size() - 2; i++) {
                    MensagemDAO.inserirMensagemImportada(mensageiro.mensagem.get(i).getConteudo(), 1);
                    MensagemDAO.inserirMensagemImportada(mensageiro.mensagem.get(i).getConteudo_resposta(), 2);

                    this.publishProgress(i + 3 + " " + getApplicationContext().getText(R.string.de) + " " + mensageiro.mensagem.size());
                    // Atualiza o progresso para o usuário
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textViewStatus.setText(getApplicationContext().getText(R.string.sicronizando));

                    }
                });


                double umPorcento = (mensageiro.mensagem.size() * 2 - 4) / 100;
                int porcentagemAtual = 1;

                for (int i = 0; i < mensageiro.mensagem.size() * 2 - 4; i++) {

                    if (i % 2 != 0) {
                        // Como perguntas e respostas foram inseridas intercaladamente esse if garante a inserção das repsotas nos lugares corretos
                        MensagemDAO.inserirRespostaImportada(i, i + 1);
                    }

                    if (i % umPorcento == 0 && porcentagemAtual <= 100) {
                        this.publishProgress(porcentagemAtual + " %");
                        porcentagemAtual += 1;
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), getApplicationContext().getText(R.string.import_sucesso), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), getApplicationContext().getText(R.string.import_falha), Toast.LENGTH_LONG).show();
                    }
                });
            }


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            layoutPrincipal.setVisibility(View.VISIBLE);
            layoutSecundario.setVisibility(View.GONE);

        }


        @Override
        protected void onProgressUpdate(String... values) {
            if (this.textViewPorcentagem != null)
                this.textViewPorcentagem.setText(String.valueOf(values[0]));
        }
    }
}
