package com.paradoxo.amadeus.cognicao

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.paradoxo.amadeus.auth.AuthManager
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.AgentStepResult
import com.paradoxo.amadeus.modelo.MacroExample
import com.paradoxo.amadeus.modelo.UiElement
import com.paradoxo.amadeus.payments.EntitlementManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class OpenAICommandInterpreter(private val context: Context) : AgentInterpreter {

    private val functions by lazy { FirebaseFunctions.getInstance() }

    override suspend fun interpret(command: String, elements: List<UiElement>): List<ActionIntent> {
        val elementsList = AgentPromptBuilder.buildElementsList(elements)
        if (elementsList.isBlank()) return emptyList()

        val prompt = AgentPromptBuilder.buildSingleShotPrompt(elementsList, command)

        return try {
            AuthManager.getInstance(context).ensureAnonymousUser()
            val text = withRetry {
                callChatCompletion(
                    model = RemoteConfigManager.openaiModelSingleShot,
                    messages = listOf(mapOf("role" to "user", "content" to prompt)),
                    maxTokens = 1028
                )
            }
            AgentResponseParser.parseActions(text)
        } catch (e: Exception) {
            Log.e("OpenAIInterpreter", "Erro ao interpretar comando: ${e.message}")
            emptyList()
        }
    }

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
        val historyText = if (history.isEmpty()) "Nenhum — este é o primeiro passo" else history.joinToString("\n")
        val systemPrompt = AgentPromptBuilder.buildSystemPrompt()
        val userTurn = AgentPromptBuilder.buildUserTurn(goal, historyText, elementsList, fewShotExamples)

        Log.w("AgenteLog", "AgenteLog, prompt enviado OpenAI: \n$userTurn")

        val isPremium = EntitlementManager.getInstance(context).state.value.isPremium
        val model = if (isPremium) RemoteConfigManager.openaiModelAgentPro else RemoteConfigManager.openaiModelAgent

        return try {
            AuthManager.getInstance(context).ensureAnonymousUser()
            val base64Image = bitmapToBase64(mapBitmap)
            val text = withRetry(onRetry = onRetry) {
                callChatCompletion(
                    model = model,
                    messages = listOf(
                        mapOf("role" to "system", "content" to systemPrompt),
                        mapOf(
                            "role" to "user",
                            "content" to listOf(
                                mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64Image")),
                                mapOf("type" to "text", "text" to userTurn)
                            )
                        )
                    )
                )
            }
            Log.w("AgenteLog", "AgenteLog, resposta do modelo: $text")
            AgentResponseParser.parseStepResult(text.ifBlank { return AgentStepResult(done = true, reason = "Resposta vazia") })
        } catch (e: Exception) {
            Log.e("OpenAIInterpreter", "Erro no passo do loop: ${e.message}", e)
            AgentStepResult(done = true, reason = "Erro: ${e.message}")
        }
    }

    private suspend fun callChatCompletion(
        model: String,
        messages: List<Map<String, Any>>,
        maxTokens: Int? = null
    ): String {
        val data = mutableMapOf<String, Any>("model" to model, "messages" to messages)
        if (maxTokens != null) data["maxTokens"] = maxTokens
        val result = functions.getHttpsCallable("chatCompletion").call(data).await()
        @Suppress("UNCHECKED_CAST")
        val payload = result.data as? Map<String, Any>
        return payload?.get("text") as? String ?: ""
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val scaled = scaleBitmap(bitmap, maxSide = 1024)
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSide: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxSide && h <= maxSide) return bitmap
        val ratio = if (w > h) maxSide.toFloat() / w else maxSide.toFloat() / h
        return Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }

    private fun isRetryable(e: Exception): Boolean {
        if (e is FirebaseFunctionsException) {
            return when (e.code) {
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED,
                FirebaseFunctionsException.Code.UNAVAILABLE,
                FirebaseFunctionsException.Code.INTERNAL,
                FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> true
                else -> false
            }
        }
        val msg = e.message ?: e.cause?.message ?: ""
        return msg.contains("timeout", ignoreCase = true) ||
               msg.contains("SocketTimeout", ignoreCase = true)
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
                    Log.w("OpenAIInterpreter", "Erro temporário (tentativa ${attempt + 1}/$maxRetries), aguardando ${waitMs}ms... — ${e.message}")
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
