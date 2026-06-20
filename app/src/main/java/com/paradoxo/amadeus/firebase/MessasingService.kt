package com.paradoxo.amadeus.firebase

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.activity.MainActivity
import com.paradoxo.amadeus.util.Preferencias
import java.net.URL
import kotlin.concurrent.thread

class MessasingService : FirebaseMessagingService() {
    override fun onNewToken(tk: String) {
        Log.d("Tk Firebase", "token Firebase: $tk")
        Preferencias.setPrefString(tk, "tk", this)

        FirebaseDatabase.getInstance().reference.child("tk").push().setValue(tk)
            .addOnSuccessListener { Log.d("Tk Firebase", "token salvo no Realtime Database") }
            .addOnFailureListener { e -> Log.e("Tk Firebase", "falha ao salvar token", e) }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val titulo = remoteMessage.notification?.title ?: remoteMessage.data["title"]
        val corpo = remoteMessage.notification?.body ?: remoteMessage.data["body"]
        val imageUrl = remoteMessage.notification?.imageUrl?.toString() ?: remoteMessage.data["imageUrl"]
        val link = remoteMessage.data["link"]
        Log.d("FCM", "Mensagem recebida: titulo=$titulo, corpo=$corpo, imageUrl=$imageUrl, link=$link")
        if (titulo.isNullOrBlank() && corpo.isNullOrBlank()) return

        criarCanalNotificacao()
        val pendingIntent = construirPendingIntent(link)

        if (!imageUrl.isNullOrBlank()) {
            thread {
                val bitmap = baixarBitmap(imageUrl)
                mostrarNotificacao(titulo, corpo, pendingIntent, bitmap)
            }
        } else {
            mostrarNotificacao(titulo, corpo, pendingIntent, null)
        }
    }

    private fun construirPendingIntent(link: String?): PendingIntent {
        val intent = if (!link.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun mostrarNotificacao(
        titulo: String?,
        corpo: String?,
        pendingIntent: PendingIntent,
        imagem: Bitmap?
    ) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo_white)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (imagem != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(imagem)
                    .setBigContentTitle(titulo)
                    .setSummaryText(corpo)
            ).setLargeIcon(imagem)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun baixarBitmap(url: String): Bitmap? = try {
        BitmapFactory.decodeStream(URL(url).openStream())
    } catch (e: Exception) {
        Log.e("FCM", "Erro ao baixar imagem: $url", e)
        null
    }

    private fun criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Notificações",
            NotificationManager.IMPORTANCE_HIGH
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "default_channel"
    }
}
