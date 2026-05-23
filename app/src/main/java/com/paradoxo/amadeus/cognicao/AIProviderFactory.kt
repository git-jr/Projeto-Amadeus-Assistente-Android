package com.paradoxo.amadeus.cognicao

import android.util.Log
import com.paradoxo.amadeus.BuildConfig

object AIProviderFactory {

    fun create(): AgentInterpreter {
        val providerName = RemoteConfigManager.aiProvider
        val provider = runCatching { AIProvider.valueOf(providerName.uppercase()) }
            .getOrElse {
                Log.w("AIProviderFactory", "Provider desconhecido '$providerName', usando OPENAI")
                AIProvider.OPENAI
            }
        return when (provider) {
            AIProvider.GEMINI -> GeminiCommandInterpreter()
            AIProvider.OPENAI -> OpenAICommandInterpreter(BuildConfig.OPENAI_API_KEY)
        }
    }
}
