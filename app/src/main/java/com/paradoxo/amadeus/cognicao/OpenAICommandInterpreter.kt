package com.paradoxo.amadeus.cognicao

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.ImagePart
import com.aallam.openai.api.chat.TextPart
import com.aallam.openai.api.exception.OpenAIException
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.AgentStepResult
import com.paradoxo.amadeus.modelo.MacroExample
import com.paradoxo.amadeus.modelo.UiElement
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream

class OpenAICommandInterpreter(private val apiKey: String) : AgentInterpreter {

    private val client by lazy { OpenAI(token = apiKey) }

    override suspend fun interpret(command: String, elements: List<UiElement>): List<ActionIntent> {
        if (apiKey.isBlank() || apiKey == "sua_chave_aqui") return emptyList()

        val elementsList = AgentPromptBuilder.buildElementsList(elements)
        if (elementsList.isBlank()) return emptyList()

        val prompt = AgentPromptBuilder.buildSingleShotPrompt(elementsList, command)

        return try {
            val response = withRetry {
                client.chatCompletion(
                    ChatCompletionRequest(
                        model = ModelId(RemoteConfigManager.openaiModelSingleShot),
                        messages = listOf(ChatMessage(role = ChatRole.User, content = prompt)),
                        maxTokens = 1028
                    )
                )
            }
            val text = response.choices.firstOrNull()?.message?.content ?: return emptyList()
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
        if (apiKey.isBlank() || apiKey == "sua_chave_aqui") return AgentStepResult(done = true, reason = "API key não configurada")

        val elementsList = AgentPromptBuilder.buildElementsList(elements)
            .ifEmpty { "Nenhum elemento interativo encontrado" }
        val historyText = if (history.isEmpty()) "Nenhum — este é o primeiro passo" else history.joinToString("\n")
        val systemPrompt = AgentPromptBuilder.buildSystemPrompt()
        val userTurn = AgentPromptBuilder.buildUserTurn(goal, historyText, elementsList, fewShotExamples)

        Log.w("AgenteLog", "AgenteLog, prompt enviado OpenAI: \n$userTurn")

        return try {
            val base64Image = bitmapToBase64(mapBitmap)
            val response = withRetry(onRetry = onRetry) {
                client.chatCompletion(
                    ChatCompletionRequest(
                        model = ModelId(RemoteConfigManager.openaiModelAgent),
                        messages = listOf(
                            ChatMessage(
                                role = ChatRole.System,
                                content = systemPrompt
                            ),
                            ChatMessage(
                                role = ChatRole.User,
                                content = listOf(
                                    ImagePart(imageUrl = ImagePart.ImageURL(url = "data:image/jpeg;base64,$base64Image")),
                                    TextPart(text = userTurn)
                                )
                            )
                        ),
//                        maxTokens = 1028
                    )
                )
            }
            val text = response.choices.firstOrNull()?.message?.content
            Log.w("AgenteLog", "AgenteLog, resposta do modelo: ${text}")
            AgentResponseParser.parseStepResult(text ?: return AgentStepResult(done = true, reason = "Resposta vazia"))
        } catch (e: Exception) {
            Log.e("OpenAIInterpreter", "Erro no passo do loop: ${e.message}", e)
            AgentStepResult(done = true, reason = "Erro: ${e.message}")
        }
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
        if (e is OpenAIException) {
            val msg = e.message ?: ""
            return msg.contains("rate", ignoreCase = true) ||
                   msg.contains("429") ||
                   msg.contains("503") ||
                   msg.contains("timeout", ignoreCase = true)
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
