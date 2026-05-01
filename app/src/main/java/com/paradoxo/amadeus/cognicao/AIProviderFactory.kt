package com.paradoxo.amadeus.cognicao

import com.paradoxo.amadeus.BuildConfig

object AIProviderFactory {

    // Altere esta constante para trocar o provedor de IA
    private val CURRENT: AIProvider = AIProvider.OPENAI

    fun create(): AgentInterpreter = when (CURRENT) {
        AIProvider.GEMINI -> GeminiCommandInterpreter(BuildConfig.GEMINI_API_KEY)
        AIProvider.OPENAI -> OpenAICommandInterpreter(BuildConfig.OPENAI_API_KEY)
    }
}
