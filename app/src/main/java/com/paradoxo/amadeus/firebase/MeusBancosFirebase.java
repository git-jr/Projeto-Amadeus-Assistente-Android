package com.paradoxo.amadeus.firebase;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.modelo.Banco;
import com.paradoxo.amadeus.retrofit.RetrofitInicializador;
import com.paradoxo.amadeus.util.Arquivo;
import com.paradoxo.amadeus.util.Notificador;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeusBancosFirebase {
    private Context context;

    public MeusBancosFirebase(Context context) {
        this.context = context;
    }

    public void fazerUploadBanco(final String nomeBanco, final String idUsuario, final String tokenDaSessao) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        Uri file = Uri.fromFile(new File(Arquivo.getCaminhoPadraoAmadeus(context)));
        final StorageReference amadeusRefNome = storageRef.child(nomeBanco + ".db");

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setCustomMetadata("nome", nomeBanco)
                .setCustomMetadata("autor", idUsuario)
                .build();

        final UploadTask uploadTask = amadeusRefNome.putFile(file, metadata);

        final Notificador notificador = new Notificador(context);
        notificador.notificacaoProgressoInciar(context.getString(R.string.fazendo_upload), "0%", 100, 0);

        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progresso = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                String progressoCompleto = progresso + "%";

                notificador.notificacaoProgressoAtualizar(progressoCompleto, (int) taskSnapshot.getTotalByteCount(), (int) taskSnapshot.getBytesTransferred());

            }
        });

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                notificador.notificacaoProgressoFinzaliar(context.getString(R.string.falha_upload));

            }
        });

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                notificador.notificacaoProgressoFinzaliar(context.getString(R.string.sucesso_upload));
                // Após realizar o upload vamos gravar a url desse arquvio no database

                final String[] uriDownload = new String[1];
                uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return amadeusRefNome.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            uriDownload[0] = String.valueOf(task.getResult());

                            Arquivo arquivo = new Arquivo();
                            String tamanho = arquivo.calcularTamnhoArquivo(new File(Arquivo.getCaminhoPadraoAmadeus(context)));
                            gravarBancoNoDatabase(nomeBanco, tamanho, idUsuario, uriDownload[0], tokenDaSessao);
                        }
                    }
                });
            }
        });
    }

    private void gravarBancoNoDatabase(String nomeBanco, String tamanho, String idUsuario, String urlDownload, String tokenDaSessao) {
        JodaTimeAndroid.init(context);
        String dtAtualizadoExibicao = DateTime.now().toString("dd/MM/YYYY HH:mm");

        Banco banco = new Banco(nomeBanco, tamanho, idUsuario, dtAtualizadoExibicao, false, urlDownload);

        Call call = new RetrofitInicializador().getBancoService().inserir(banco, tokenDaSessao);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                Log.e("TGA", "Banco inserido com SUCCESS");
                Log.e("TGA", String.valueOf(response.code()));
                Log.e("TGA", call.request().url().toString());
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                Log.e("TGA", "Banco não inserido com SUCCESS");

            }
        });
    }

    public void downlaodBanco(String urlDnwload, String nomeBanco) {

        final Notificador notificador = new Notificador(context);
        notificador.notificacaoProgressoInciar(context.getString(R.string.fazendo_download), "0%", 100, 0);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference isLandRef = storage.getReferenceFromUrl(urlDnwload);

        Arquivo arquivo = new Arquivo();
        File amadeusCaminhoRoot = arquivo.getAmadeusCaminhoRoot();
        File localFile = new File(amadeusCaminhoRoot, nomeBanco + ".db");
        isLandRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                notificador.notificacaoProgressoFinzaliar(context.getString(R.string.sucesso_download));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                notificador.notificacaoProgressoFinzaliar(context.getString(R.string.falha_download));

            }
        }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {

                double progresso = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                if (progresso > 0.0) {
                    String progressoCompleto = progresso + "%";
                    notificador.notificacaoProgressoAtualizar(progressoCompleto, (int) taskSnapshot.getTotalByteCount(), (int) taskSnapshot.getBytesTransferred());
                }

            }
        });
    }

}
