package com.paradoxo.amadeus.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.paradoxo.amadeus.dao.SentencaDAO
import com.paradoxo.amadeus.modelo.Sentenca
import com.paradoxo.amadeus.util.Preferencias

class GravaHistoricoService : Service() {
    companion object {
        const val TK = "tk"
        const val ID_ULTIMO_UPLOAD = "idUltimoUpload"
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val idUltimoUpload = intent.getLongExtra(ID_ULTIMO_UPLOAD, -1)
        Log.e(ID_ULTIMO_UPLOAD, idUltimoUpload.toString())

        val historicoParaUpload = SentencaDAO(this, true).listarAPartirDe(idUltimoUpload)
        inseriHistoricoFibrebase(historicoParaUpload)

        return super.onStartCommand(intent, flags, startId)
    }

    private fun inseriHistoricoFibrebase(historicoParaUpload: List<Sentenca>) {
        val tk = Preferencias.getPrefString(TK, applicationContext)

        FirebaseDatabase.getInstance().reference
            .child("historico-beta").child(tk).push()
            .setValue(historicoParaUpload)
            .addOnSuccessListener {
                Preferencias.setPrefLong(
                    ID_ULTIMO_UPLOAD,
                    historicoParaUpload.last().id!!.toLong(),
                    applicationContext
                )
            }
            .addOnFailureListener { e ->
                Log.e("Falha ao upar", "Histórico de conversas")
                e.printStackTrace()
            }

        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.e("Service Historico", "Destruida")
    }
}
