package com.paradoxo.amadeus.assis

import android.content.Intent
import android.os.Build
import android.service.voice.VoiceInteractionService
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Serviço de interação por voz. A API pública AlwaysOnHotwordDetector foi removida do
 * SDK de compilação em versões recentes; o fluxo de hotword foi desativado para permitir build.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class VoiceCommandService : VoiceInteractionService() {
    companion object {
        private const val TAG = "VoiceCommandService"
    }

    override fun onCreate() {
        Log.e(TAG, "Entered on create")
        super.onCreate()
    }

    override fun onReady() {
        super.onReady()
        Log.e(TAG, "onReady: $this")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
