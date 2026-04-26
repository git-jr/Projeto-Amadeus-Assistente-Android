package com.paradoxo.amadeus.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

const val PERMISSAO_ACESSO_MICROFONE = 1

fun Context.canDrawOverlays(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(this)
    } else {
        true
    }
}

object Permissao {

    fun armazenamentoAcessivel(context: Context): Boolean {
        val permissao = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permissao) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getIntentSolicitarSobreposicao(packageName: String): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri()
        )
    }

    fun solicitarPermissaoMicrofone(activity: Activity) {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSAO_ACESSO_MICROFONE
            )
        }
    }

    fun solicitarAcessoArmazenamento(activity: Activity) {
        val permissao = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        ActivityCompat.requestPermissions(activity, arrayOf(permissao), 2)
    }

    fun Context.hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

}

/*
 * BACKUP — Java original
 *
 * package com.paradoxo.amadeus.util;
 *
 * import android.Manifest;
 * import android.app.Activity;
 * import android.content.Context;
 * import android.content.pm.PackageManager;
 * import android.os.Build;
 * import androidx.core.app.ActivityCompat;
 * import androidx.core.content.ContextCompat;
 *
 * public class Permissao {
 *
 *     public static boolean armazenamentoAcessivel(Context context) {
 *         return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
 *     }
 *
 *     public static void solicitarPermissaoMicrofone(Activity activity) {
 *         if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
 *             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
 *                 int PERMISSAO_ACESSO_MICROFONE = 1;
 *                 activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSAO_ACESSO_MICROFONE);
 *             }
 *         }
 *     }
 *
 *     public static void solicitarAcessoArmazenamento(Activity activity) {
 *         int PERMISSAO_ACESSO_ARMAZENAMENTO = 2; // here
 *         ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSAO_ACESSO_ARMAZENAMENTO);
 *     }
 * }
 */
