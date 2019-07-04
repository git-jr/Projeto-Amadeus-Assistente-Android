package com.paradoxo.amadeus.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.activity.MainActivity;

import static android.content.Context.NOTIFICATION_SERVICE;

public class Notificador {
    private Context context;

    private int notificationIdProgress;
    private NotificationCompat.Builder builderProgress;
    private NotificationManagerCompat notificationManagerCompatProgress;
    private static final String CHANNEL_ID_PROGRESS = "CHANNEL_ID_PROGRESS";

    public Notificador(Context context) {
        this.context = context;
    }

    public void notificacaoProgressoInciar(String titulo, String textoAndamento, int tamanhoMax, int tamnahoAtual) {
        notificationManagerCompatProgress = NotificationManagerCompat.from(context);
        builderProgress = new NotificationCompat.Builder(context, CHANNEL_ID_PROGRESS);
        builderProgress.setContentTitle(titulo)
                .setContentText(textoAndamento)
                .setSmallIcon(R.drawable.icone_notificacao_1)
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setColorized(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(false);

        builderProgress.setProgress(tamanhoMax, tamnahoAtual, true);
        notificationIdProgress = 5;
        notificationManagerCompatProgress.notify(notificationIdProgress, builderProgress.build());
    }

    public void notificacaoProgressoAtualizar(String textoAndamento, int tamanhoMax, int tamnahoAtual) {
        builderProgress.setProgress(tamanhoMax, tamnahoAtual, false);
        builderProgress.setContentText(textoAndamento);
        notificationManagerCompatProgress.notify(notificationIdProgress, builderProgress.build());

    }

    public void notificacaoProgressoFinzaliar(String textoFinalizado) {
        builderProgress.setContentText(textoFinalizado)
                .setContentTitle(context.getString(R.string.concluido))
                .setProgress(0, 0, false);
        notificationManagerCompatProgress.notify(notificationIdProgress, builderProgress.build());

    }

    public void notificarConteudoSimples(String titulo, String conteudo, String url_video) {
        int idNot = 1;
        String idCanal = "1";
        String nomeCanal = "canal1";

        NotificationManager notManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, idCanal)
                .setSmallIcon(R.drawable.icone_notificacao_2)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setColor(Color.parseColor("#FFFF0073"))
                .setContentTitle(titulo)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentText(conteudo);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notCanal = new NotificationChannel(idCanal, nomeCanal, NotificationManager.IMPORTANCE_DEFAULT);

            notCanal.enableLights(true);
            notCanal.setLightColor(Color.WHITE);
            notCanal.setShowBadge(true);
            notCanal.enableVibration(true);
            notCanal.setImportance(NotificationManager.IMPORTANCE_HIGH);
            builder.setChannelId(idCanal);

            if (notManager != null) {
                notManager.createNotificationChannel(notCanal);
            }

        } else {

            builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);
        }

        Intent youtubeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url_video));

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        stackBuilder.addNextIntent(youtubeIntent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(idNot, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pendingIntent);

        if (notManager != null) {
            notManager.notify(idNot, builder.build());
        }

    }

    public void notificarComLink(String titulo, String subtitulo, String url_video, Bitmap img_lateral) {
        int idNot = 2;
        String idCanal = "2";
        String nomeCanal = "canal2";

        NotificationCompat.BigPictureStyle notBgStyle = new NotificationCompat.BigPictureStyle();
        notBgStyle.bigPicture(img_lateral).build();

        Intent youtubeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url_video));
        // Abre o video ao toque da notificação
        PendingIntent pendingIntent = PendingIntent.getActivity(context, idNot, youtubeIntent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, idCanal)
                .setSmallIcon(R.drawable.icone_notificacao_2)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setColor(Color.parseColor("#FFFF0073"))
                .setContentTitle(titulo)
                .setContentText(subtitulo)
                .setColorized(true)
                .setColor(Color.RED)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(android.R.drawable.ic_media_play, context.getText(R.string.cancelar), pendingIntent)
                .setStyle(notBgStyle);

        mBuilder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel notCanal = new NotificationChannel(idCanal, nomeCanal, NotificationManager.IMPORTANCE_DEFAULT);

            notCanal.enableLights(true);
            notCanal.setLightColor(Color.WHITE);
            notCanal.setShowBadge(true);
            notCanal.enableVibration(true);
            notCanal.setImportance(NotificationManager.IMPORTANCE_HIGH);
            mBuilder.setChannelId(idCanal);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notCanal);
            }
        } else {
            mBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);
        }

        if (notificationManager != null) {
            notificationManager.notify(idNot, mBuilder.build());
        }
    }

    public void notificarConteudoLongo(String titulo, String subtitulo, String conteudo) {
        int idNot = 2;
        String idCanal = "2";
        String nomeCanal = "canal2";

        NotificationManager notManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, idCanal)
                .setSmallIcon(R.drawable.icone_notificacao_2)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setColor(Color.parseColor("#FFFF0073")) // Cor do icone
                .setContentTitle(titulo)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentText(subtitulo);

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();

        bigTextStyle.setBigContentTitle(titulo);
        bigTextStyle.bigText(conteudo);
        builder.setStyle(bigTextStyle);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notCanal = new NotificationChannel(idCanal, nomeCanal, NotificationManager.IMPORTANCE_DEFAULT);

            notCanal.enableLights(true);
            notCanal.setLightColor(Color.WHITE);
            notCanal.setShowBadge(true);
            notCanal.enableVibration(true);
            notCanal.setImportance(NotificationManager.IMPORTANCE_HIGH);
            builder.setChannelId(idCanal);

            if (notManager != null) {
                notManager.createNotificationChannel(notCanal);
            }

        } else {

            builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);
        }

        Intent youtubeIntent = new Intent(context, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        stackBuilder.addNextIntent(youtubeIntent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(idNot, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pendingIntent);

        if (notManager != null) {
            notManager.notify(idNot, builder.build());
        }
    }
}
