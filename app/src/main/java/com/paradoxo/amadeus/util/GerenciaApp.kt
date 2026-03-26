package com.paradoxo.amadeus.util

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.enums.AcaoEnum
import com.paradoxo.amadeus.enums.ItemEnum
import com.paradoxo.amadeus.modelo.Sentenca
import org.greenrobot.eventbus.EventBus

object GerenciaApp {

    @JvmStatic
    fun encontrarApp(entrada: String, context: Context) {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val appsInstalados = packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)

        for (packageInfo in appsInstalados) {
            val nomeApp = packageInfo.loadLabel(packageManager).toString().lowercase()
            try {
                if (nomeApp == entrada) {
                    val intentAppParaAbrir = packageManager.getLaunchIntentForPackage(packageInfo.activityInfo.packageName)
                    context.startActivity(intentAppParaAbrir)
                    Log.e(TAG, "Abrindo$nomeApp")
                    notificarOutput("Abrindo $nomeApp")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "ERROAo listar o app")
                notificarOutput(context.getString(R.string.erro_abrir_app) + nomeApp)
            }
        }

        val indexEspaco = entrada.indexOf(" ")
        if (indexEspaco == -1) {
            notificarOutputErroApp(entrada, context)
            return
        }
        val nomeDoApp = entrada.substring(indexEspaco).trim()
        if (entrada.length == nomeDoApp.length) {
            notificarOutputErroApp(entrada, context)
        } else {
            encontrarApp(nomeDoApp, context)
            Log.e(TAG, "ERROAo listar o app")
        }
    }

    private fun notificarOutput(mensagem: String) {
        val sentenca = Sentenca(mensagem)
        EventBus.getDefault().post(sentenca)
    }

    private fun notificarOutputErroApp(nomeApp: String, context: Context) {
        Log.e(TAG, "ERROApp não existe")
        val sentenca = Sentenca(nomeApp + context.getString(R.string.app_nao_instalado_no_momento), AcaoEnum.ACAO_ERRO_APP_NAO_EXISTE)
        sentenca.tipo_item = ItemEnum.ITEM_CARD.ordinal
        sentenca.addResposta(context.getString(R.string.app_nao_encontrado))
        EventBus.getDefault().post(sentenca)
    }

    @JvmStatic
    fun buscarAppOnline(entrada: String, context: Context) {
        if (entrada.isNotEmpty()) {
            val intenteBuscarApp = Intent(Intent.ACTION_VIEW)
            intenteBuscarApp.data = Uri.parse("https://play.google.com/store/search?q=$entrada&c=apps")
            context.startActivity(intenteBuscarApp)
        }
    }
}
