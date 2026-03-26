package com.paradoxo.amadeus.util.voz

import android.app.Service
import android.content.Intent
import android.os.IBinder

class EscutadaoraService : Service() {
    private lateinit var vozParaTexto: VozParaTexto

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        vozParaTexto = VozParaTexto(applicationContext)
        vozParaTexto.backgroundVoiceListener.run()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        vozParaTexto.backgroundVoiceListener.interrupt()
    }
}
