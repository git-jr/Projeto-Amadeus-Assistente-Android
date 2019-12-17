package com.paradoxo.amadeus.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Permissao {

    public static boolean armazenamentoAcessivel(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static void solicitarPermissaoMicrofone(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int PERMISSAO_ACESSO_MICROFONE = 1;
                activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSAO_ACESSO_MICROFONE);
            }
        }
    }

    public static void solicitarAcessoArmazenamento(Activity activity) {
        int PERMISSAO_ACESSO_ARMAZENAMENTO = 2;
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSAO_ACESSO_ARMAZENAMENTO);
    }

}
