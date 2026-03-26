package com.paradoxo.amadeus.cognicao

import android.app.Activity
import com.paradoxo.amadeus.modelo.Entidade
import com.paradoxo.amadeus.modelo.Sentenca
import com.paradoxo.amadeus.util.Preferencias.getPrefString
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class Processadora(private val activity: Activity) {

    private val acionadora = Acionadora(activity)

    init {
        EventBus.getDefault().register(this)
    }

    fun processarEntrada(entrada: String) {
        val normalizada = entrada
            .lowercase()
            .replace("?", "")
            .replace("!", "")
            .replace(getPrefString(PREF_NOME_IA, activity), "")
            .trim()

        acionadora.isAcao(normalizada)
    }

    @Subscribe
    fun lidarResultadoProcessamento(entidade: Entidade) {
        EventBus.getDefault().post(Sentenca("Via Event"))
    }

    fun destruir() {
        EventBus.getDefault().unregister(this)
    }

    companion object {
        const val PREF_NOME_IA = "nomeIa"
    }
}
