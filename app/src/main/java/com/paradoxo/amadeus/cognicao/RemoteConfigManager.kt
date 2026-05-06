package com.paradoxo.amadeus.cognicao

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.paradoxo.amadeus.BuildConfig
import kotlinx.coroutines.tasks.await

object RemoteConfigManager {

    private const val KEY_AI_PROVIDER = "ai_provider"
    private const val KEY_OPENAI_MODEL_SINGLE = "openai_model_single_shot"
    private const val KEY_OPENAI_MODEL_AGENT = "openai_model_agent"
    private const val KEY_GEMINI_MODEL = "gemini_model"
    private const val KEY_SYSTEM_PROMPT = "system_prompt"
    private const val KEY_SINGLE_SHOT_TEMPLATE = "single_shot_prompt_template"
    private const val KEY_USER_TURN_TEMPLATE = "user_turn_prompt_template"

    private val config: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else 3600L)
                    .build()
            )
            setDefaultsAsync(
                mapOf(
                    KEY_AI_PROVIDER to "OPENAI",
                    KEY_OPENAI_MODEL_SINGLE to "gpt-4o-mini",
                    KEY_OPENAI_MODEL_AGENT to "gpt-5.4-nano",
                    KEY_GEMINI_MODEL to "gemini-3.1-flash-lite-preview",
                    KEY_SYSTEM_PROMPT to AgentPromptBuilder.DEFAULT_SYSTEM_PROMPT,
                    KEY_SINGLE_SHOT_TEMPLATE to AgentPromptBuilder.DEFAULT_SINGLE_SHOT_TEMPLATE,
                    KEY_USER_TURN_TEMPLATE to AgentPromptBuilder.DEFAULT_USER_TURN_TEMPLATE
                )
            )
        }
    }

    val aiProvider: String get() = config.getString(KEY_AI_PROVIDER).ifBlank { "OPENAI" }
    val openaiModelSingleShot: String get() = config.getString(KEY_OPENAI_MODEL_SINGLE).ifBlank { "gpt-4o-mini" }
    val openaiModelAgent: String get() = config.getString(KEY_OPENAI_MODEL_AGENT).ifBlank { "gpt-5.4-nano" }
    val geminiModel: String get() = config.getString(KEY_GEMINI_MODEL).ifBlank { "gemini-3.1-flash-lite-preview" }
    val systemPrompt: String get() = config.getString(KEY_SYSTEM_PROMPT).ifBlank { AgentPromptBuilder.DEFAULT_SYSTEM_PROMPT }
    val singleShotTemplate: String get() = config.getString(KEY_SINGLE_SHOT_TEMPLATE).ifBlank { AgentPromptBuilder.DEFAULT_SINGLE_SHOT_TEMPLATE }
    val userTurnTemplate: String get() = config.getString(KEY_USER_TURN_TEMPLATE).ifBlank { AgentPromptBuilder.DEFAULT_USER_TURN_TEMPLATE }

    suspend fun fetchAndActivate() {
        try {
            config // trigger lazy init before await
            config.fetchAndActivate().await()
            Log.d("RemoteConfig", "Configs atualizados com sucesso")
        } catch (e: Exception) {
            Log.w("RemoteConfig", "Fetch falhou, usando defaults: ${e.message}")
        }
    }
}
