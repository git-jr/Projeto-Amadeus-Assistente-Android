package com.paradoxo.amadeus.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.paradoxo.amadeus.dao.SentencaDAO;
import com.paradoxo.amadeus.modelo.Sentenca;
import com.paradoxo.amadeus.util.Preferencias;

import java.util.List;

public class GravaHistoricoService extends Service {
    public static final String TK = "tk";
    public static final String ID_ULTIMO_UPLOAD = "idUltimoUpload";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long idUltimoUpload = intent.getLongExtra(ID_ULTIMO_UPLOAD, -1);
        Log.e(ID_ULTIMO_UPLOAD, String.valueOf(idUltimoUpload));

        SentencaDAO sentencaDAO = new SentencaDAO(this, true);
        List<Sentenca> historicoParaUpload = sentencaDAO.listarAPartirDe(idUltimoUpload);
        inseriHistoricoFibrebase(historicoParaUpload);

        return super.onStartCommand(intent, flags, startId);
    }

    private void inseriHistoricoFibrebase(List<Sentenca> historicoParaUpload){
        String tk = Preferencias.getPrefString(TK, getApplicationContext());

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child("historico-beta").child(tk).push().setValue(historicoParaUpload)
                .addOnSuccessListener(aVoid -> Preferencias.setPrefLong(ID_ULTIMO_UPLOAD,
                        Long.parseLong(historicoParaUpload.get(historicoParaUpload.size()-1).getId()),
                        getApplicationContext())).addOnFailureListener(e -> {
                    Log.e("Falha ao upar", "Histórico de conversas");
                    e.printStackTrace();
                });

        this.stopSelf();
        // Depois que termina de fazer o upload para a service
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("Service Historico", "Destruida");
    }

}
