package com.paradoxo.amadeus.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.provider.MediaStore
import android.util.Log
import com.paradoxo.amadeus.modelo.Musica
import com.paradoxo.amadeus.modelo.Sentenca
import org.greenrobot.eventbus.EventBus

class GerenciaMusica(private val context: Context? = null) {
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val REPRODUZINDO = "Reproduzindo "
    }

    fun encontrarMusica(nomeMusicaBuscada: String?): Musica {
        val musica = Musica()
        val aleatorio = nomeMusicaBuscada == null
        val nomeBusca = nomeMusicaBuscada ?: ""
        val ordenarAleatorio = if (aleatorio) "RANDOM()" else null

        val cursor = context?.contentResolver?.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, ordenarAleatorio
        )
        cursor?.use {
            while (!it.isClosed && it.moveToNext()) {
                val caminho = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val nomeMusica = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))

                if (!caminho.contains("WhatsApp") && !caminho.contains("RecForge") && !caminho.contains("hangouts")) {
                    if (aleatorio || nomeMusica.lowercase().contains(nomeBusca)) {
                        musica.nome = nomeMusica.substring(0, nomeMusica.lastIndexOf('.'))
                        musica.caminho = caminho
                        musica.artista = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                        musica.album = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                        break
                    }
                } else if (aleatorio) {
                    if (!it.isClosed) it.close()
                    encontrarMusica(null)
                }
            }
        }
        return musica
    }

    fun configurarMediPlayer(musica: Musica) {
        pararMusicaSeEstiverTocando()
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener { Log.e("A música", "Acabou") }
            try {
                @Suppress("DEPRECATION")
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                try {
                    setDataSource(musica.caminho)
                } catch (e: Exception) {
                    Log.e("Erro load", "Load musica")
                    e.printStackTrace()
                }
                prepareAsync()
                setOnPreparedListener { tocarMusica(musica) }
            } catch (e: Exception) {
                Log.e("A música", "Não pode ser reproduzida")
            }
        }
    }

    private fun tocarMusica(musica: Musica) {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            else {
                it.start()
                Log.e(REPRODUZINDO, musica.nome)
                notificarOutput(REPRODUZINDO + musica.nome)
            }
        }
    }

    fun pararMusicaSeEstiverTocando() {
        if (musicaEstaTocando()) mediaPlayer?.stop()
    }

    fun musicaEstaTocando(): Boolean = mediaPlayer?.isPlaying == true

    private fun notificarOutput(mensagem: String) {
        EventBus.getDefault().post(Sentenca(mensagem))
    }
}
