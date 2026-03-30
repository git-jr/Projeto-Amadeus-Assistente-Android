package com.paradoxo.amadeus.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.toModel
import com.paradoxo.amadeus.modelo.Sentenca
import com.paradoxo.amadeus.util.Preferencias
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class GravaHistoricoService : Service() {
    companion object {
        const val TK = "tk"
        const val ID_ULTIMO_UPLOAD = "idUltimoUpload"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val idUltimoUpload = intent.getLongExtra(ID_ULTIMO_UPLOAD, -1)
        Log.e(ID_ULTIMO_UPLOAD, idUltimoUpload.toString())

        serviceScope.launch {
            val historico = AmadeusDatabase.getInstance(this@GravaHistoricoService)
                .sentencaDAO().listarAPartirDe(idUltimoUpload)
            if (historico.isNotEmpty()) {
                inseriHistoricoFibrebase(historico.map { it.toModel() })
            } else {
                stopSelf()
            }
        }

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
        serviceScope.cancel()
        Log.e("Service Historico", "Destruida")
    }
}
