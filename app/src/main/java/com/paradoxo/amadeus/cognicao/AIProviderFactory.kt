package com.paradoxo.amadeus.cognicao

import android.content.Context
import android.util.Log
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.util.Preferencias
import kotlinx.coroutines.runBlocking

object AIProviderFactory {

    fun create(context: Context): AgentInterpreter {
        val pref = Preferencias.getPrefString(Preferencias.PREF_AI_PROVIDER, context)
        val providerName = pref.ifBlank { RemoteConfigManager.aiProvider }
        val provider = runCatching { AIProvider.valueOf(providerName.uppercase()) }
            .getOrElse {
                Log.w("AIProviderFactory", "Provider desconhecido '$providerName', usando OPENAI")
                AIProvider.OPENAI
            }
        return when (provider) {
            AIProvider.GEMINI -> GeminiCommandInterpreter()
            AIProvider.OPENAI -> OpenAICommandInterpreter(context)
            AIProvider.LOCAL -> {
                val active = runBlocking {
                    AmadeusDatabase.getInstance(context).localModelDAO().getActive()
                }
                if (active?.filePath != null) {
                    LiteRTCommandInterpreter(context, active.filePath)
                } else {
                    Log.w("AIProviderFactory", "LOCAL selecionado mas nenhum modelo ativo. Fallback OPENAI.")
                    OpenAICommandInterpreter(context)
                }
            }
        }
    }
}
