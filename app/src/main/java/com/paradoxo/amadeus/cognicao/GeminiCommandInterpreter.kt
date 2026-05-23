package com.paradoxo.amadeus.cognicao

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.AgentStepResult
import com.paradoxo.amadeus.modelo.MacroExample
import com.paradoxo.amadeus.modelo.UiElement
import kotlinx.coroutines.delay

class GeminiCommandInterpreter : AgentInterpreter {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.vertexAI()).generativeModel(
            modelName = RemoteConfigManager.geminiModel,
            generationConfig = generationConfig {
                temperature = 0.1f
                maxOutputTokens = 512
            },
            systemInstruction = content { text(AgentPromptBuilder.buildSystemPrompt()) }
        )
    }

    // ── Single-shot (mantido como fallback rápido) ────────────────────────────

    override suspend fun interpret(command: String, elements: List<UiElement>): List<ActionIntent> {
        val elementsList = AgentPromptBuilder.buildElementsList(elements)
        if (elementsList.isBlank()) return emptyList()

        return try {
            val response = withRetry { model.generateContent(AgentPromptBuilder.buildSingleShotPrompt(elementsList, command)) }
            AgentResponseParser.parseActions(response.text ?: return emptyList())
        } catch (e: Exception) {
            Log.e("GeminiInterpreter", "Erro ao interpretar comando: ${e.message}")
            emptyList()
        }
    }

    // ── Loop agêntico ─────────────────────────────────────────────────────────

    override suspend fun interpretStep(
        goal: String,
        history: List<String>,
        elements: List<UiElement>,
        mapBitmap: Bitmap,
        fewShotExamples: List<MacroExample>,
        onRetry: (suspend (attempt: Int, waitMs: Long) -> Unit)?
    ): AgentStepResult {
        val elementsList = AgentPromptBuilder.buildElementsList(elements)
            .ifEmpty { "Nenhum elemento interativo encontrado" }

        val historyText = if (history.isEmpty()) "Nenhum — este é o primeiro passo"
        else history.joinToString("\n")

        val prompt = AgentPromptBuilder.buildUserTurn(goal, historyText, elementsList, fewShotExamples)
        Log.w("AgenteLog", "AgenteLog, prompt enviado Gemini: \n$prompt")

        return try {
            val response = withRetry(onRetry = onRetry) {
                model.generateContent(
                    content {
                        image(mapBitmap)
                        text(prompt)
                    }
                )
            }
            Log.w("AgenteLog", "AgenteLog, resposta do modelo: ${response.text}")
            AgentResponseParser.parseStepResult(response.text ?: return AgentStepResult(done = true, reason = "Resposta vazia"))
        } catch (e: Exception) {
            Log.e("GeminiInterpreter", "Erro no passo do loop: ${e.message}\nCausa: ${e.cause?.message}", e)
            AgentStepResult(done = true, reason = "Erro: ${e.message}")
        }
    }

    // ── Retry (503 / MissingFieldException) ──────────────────────────────────

    private fun isRetryable(e: Exception): Boolean {
        val msg = e.message ?: e.cause?.message ?: ""
        return msg.contains("503") ||
               msg.contains("UNAVAILABLE", ignoreCase = true) ||
               msg.contains("high demand", ignoreCase = true) ||
               msg.contains("currently experiencing", ignoreCase = true) ||
               e::class.simpleName == "MissingFieldException" ||
               e.cause?.let { it::class.simpleName == "MissingFieldException" } == true
    }

    private suspend fun <T> withRetry(
        maxRetries: Int = 4,
        initialDelayMs: Long = 8000L,
        onRetry: (suspend (attempt: Int, waitMs: Long) -> Unit)? = null,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                if (isRetryable(e)) {
                    lastException = e
                    val waitMs = initialDelayMs * (attempt + 1)
                    Log.w("GeminiInterpreter", "Serviço indisponível (tentativa ${attempt + 1}/$maxRetries), aguardando ${waitMs}ms... — ${e.message}")
                    onRetry?.invoke(attempt + 1, waitMs)
                    delay(waitMs)
                } else {
                    throw e
                }
            }
        }
        throw lastException!!
    }
}
