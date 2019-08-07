package com.paradoxo.amadeus.util;

import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.util.Log;

import com.paradoxo.amadeus.modelo.Musica;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class GerenciaMusica {
    private Context context;
    MediaPlayer mediaPlayer;

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
            while (cursor.moveToNext()) {
                String caminho = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                String nomeMusica = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));

                if (!caminho.contains("WhatsApp") && !caminho.contains("RecForge") && !caminho.contains("hangouts")) {
                    if (aleatorio || nomeMusica.toLowerCase().contains(nomeMusicaBusada)) {
                        musica.setNome(nomeMusica);
                        musica.setCaminho(caminho);
                        musica.setArtista(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                        musica.setAlbum(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
                        Log.e("Nome", musica.getNome());
                        Log.e("Caminho", musica.getCaminho());
                        break;
                    }
                } else if (aleatorio) {
                    encontrarMusica(null);
                }
            }
            cursor.close();
        }

        return musica;

    }

    public static List<Musica> listarMusicas(String nomeMusica, Context context) {
        boolean aleatorio = !nomeMusica.isEmpty();
        String ordernarAleatorio = aleatorio ? "RANDOM()" : null;
        List<Musica> musicas = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, ordernarAleatorio);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String caminho = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

                if (!caminho.contains("WhatsApp") || !caminho.contains("RecForge") || !caminho.contains("hangouts")) {
                    Musica musica = new Musica();
                    musica.setCaminho(caminho);
                    musica.setNome(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)));
                    musica.setCaminho(caminho);
                    musica.setArtista(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                    musica.setAlbum(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
                    musicas.add(musica);
                    Log.e("Nome", musica.getNome());
                    Log.e("Caminho", musica.getCaminho());
                    if (aleatorio) {
                        break;
                    }
                } else if (aleatorio) {
                    listarMusicas(nomeMusica, context);
                }
            }
        }
        cursor.close();
        return musicas;
    }

    public void configurarMediPlayer(final Musica musica) {
        pararMusicaSeEstiverTocando();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.e("A música", "Acabou");
            }
        });

        try {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            try {
                mediaPlayer.setDataSource(musica.getCaminho());
            } catch (Exception e) {
                Log.e("Erro load", "Load musica");
                e.printStackTrace();
            }

            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    tocarMusica(musica);
                }
            });


        } catch (Exception e) {
            Log.e("A música", "Não pode ser reproduzida");
        }
    }

    private void tocarMusica(Musica musica) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();

        } else {
            mediaPlayer.start();
            Log.e("Reproduzindo ", musica.getNome());
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
}
