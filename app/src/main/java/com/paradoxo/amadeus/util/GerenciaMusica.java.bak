package com.paradoxo.amadeus.util;

import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.util.Log;

import com.paradoxo.amadeus.modelo.Musica;
import com.paradoxo.amadeus.modelo.Sentenca;

import org.greenrobot.eventbus.EventBus;

public class GerenciaMusica {
    private Context context;
    private MediaPlayer mediaPlayer;

    public static final String REPRODUZINDO = "Reproduzindo ";

    public GerenciaMusica() {
    }

    public GerenciaMusica(Context context) {
        this.context = context;
    }

    public Musica encontrarMusica(String nomeMusicaBusada) {
        final Musica musica = new Musica();
        final boolean aleatorio = nomeMusicaBusada == null;
        if (nomeMusicaBusada == null) nomeMusicaBusada = "";
        final String ordernarAleatorio = aleatorio ? "RANDOM()" : null;

        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, ordernarAleatorio);
        if (cursor != null) {
            while (!cursor.isClosed() && cursor.moveToNext()) {
                String caminho = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                String nomeMusica = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));

                if (!caminho.contains("WhatsApp") && !caminho.contains("RecForge") && !caminho.contains("hangouts")) {
                    if (aleatorio || nomeMusica.toLowerCase().contains(nomeMusicaBusada)) {
                        musica.setNome(nomeMusica.substring(0, nomeMusica.lastIndexOf('.')));
                        musica.setCaminho(caminho);
                        musica.setArtista(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                        musica.setAlbum(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
                        String musicaAtual = musica.getNome();
                        break;
                    }
                } else if (aleatorio) {
                    if (!cursor.isClosed()) cursor.close();
                    encontrarMusica(null);
                }
            }
            if (!cursor.isClosed()) cursor.close();
        }

        return musica;
    }

    public void configurarMediPlayer(final Musica musica) {
        pararMusicaSeEstiverTocando();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mediaPlayer -> Log.e("A música", "Acabou"));

        try {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(musica.getCaminho());
            } catch (Exception e) {
                Log.e("Erro load", "Load musica");
                e.printStackTrace();
            }

            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> tocarMusica(musica));

        } catch (Exception e) {
            Log.e("A música", "Não pode ser reproduzida");
        }
    }

    private void tocarMusica(Musica musica) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        } else {
            mediaPlayer.start();
            Log.e(REPRODUZINDO, musica.getNome());
            notificarOutput(REPRODUZINDO + musica.getNome());
        }
    }

    public void pararMusicaSeEstiverTocando() {
        if (musicaEstaTocando()) {
            mediaPlayer.stop();
        }
    }

    public boolean musicaEstaTocando() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    private static void notificarOutput(String mensagem) {
        Sentenca sentenca = new Sentenca(mensagem);
        EventBus.getDefault().post(sentenca);
    }
}
