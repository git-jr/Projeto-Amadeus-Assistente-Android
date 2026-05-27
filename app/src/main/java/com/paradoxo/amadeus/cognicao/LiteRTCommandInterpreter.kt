package com.paradoxo.amadeus.cognicao

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.AgentStepResult
import com.paradoxo.amadeus.modelo.MacroExample
import com.paradoxo.amadeus.modelo.UiElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LiteRTCommandInterpreter(
    private val context: Context,
    private val modelPath: String
) : AgentInterpreter {

    // Engine criado uma vez e reutilizado — inicialização leva alguns segundos
    @Volatile private var _engine: Engine? = null

    private fun getEngine(): Engine =
        _engine ?: synchronized(this) {
            _engine ?: Engine(EngineConfig(modelPath = modelPath, backend = Backend.GPU()))
                .also { it.initialize(); _engine = it }
        }

    override suspend fun interpret(
        command: String,
        elements: List<UiElement>
    ): List<ActionIntent> = withContext(Dispatchers.IO) {
        val elementsList = AgentPromptBuilder.buildElementsList(elements)
        if (elementsList.isBlank()) return@withContext emptyList()
        val prompt = wrapGemmaPrompt(
            AgentPromptBuilder.buildSingleShotPrompt(elementsList, command)
        )
        try {
            getEngine().createConversation().use { conversation ->
                val response = conversation.sendMessage(prompt)
                AgentResponseParser.parseActions(response.toText())
            }
        } catch (e: Exception) {
            Log.e("LiteRTInterpreter", "interpret error: ${e.message}")
            emptyList()
        }
    }

    override suspend fun interpretStep(
        goal: String,
        history: List<String>,
        elements: List<UiElement>,
        mapBitmap: Bitmap,
        fewShotExamples: List<MacroExample>,
        onRetry: (suspend (Int, Long) -> Unit)?
    ): AgentStepResult = withContext(Dispatchers.IO) {
        val elementsList = AgentPromptBuilder.buildElementsList(elements)
            .ifEmpty { "Nenhum elemento interativo encontrado" }
        val historyText = if (history.isEmpty()) "Nenhum — este é o primeiro passo"
            else history.joinToString("\n")
        // Modelos text-only: inclui dimensões da tela como contexto em vez de enviar imagem
        val visualContext = "Tela: ${mapBitmap.width}x${mapBitmap.height}px, ${elements.size} elementos detectados.\n"
        val userTurn = AgentPromptBuilder.buildUserTurn(goal, historyText, elementsList, fewShotExamples)
        val fullPrompt = wrapGemmaPrompt(
            AgentPromptBuilder.buildSystemPrompt() + "\n\n" + visualContext + userTurn
        )
        try {
            getEngine().createConversation().use { conversation ->
                val response = conversation.sendMessage(fullPrompt)
                val text = response.toText()
                Log.w("AgenteLog", "AgenteLog, resposta LiteRT: $text")
                AgentResponseParser.parseStepResult(text)
            }
        } catch (e: Exception) {
            Log.e("LiteRTInterpreter", "interpretStep error: ${e.message}")
            AgentStepResult(done = true, reason = "Erro modelo local: ${e.message}")
        }
    }

    fun release() {
        synchronized(this) {
            _engine?.let { runCatching { it.close() } }
            _engine = null
        }
    }

    // Gemma instruction-tuned chat format
    private fun wrapGemmaPrompt(content: String): String =
        "<start_of_turn>user\n$content<end_of_turn>\n<start_of_turn>model\n"
}

private fun com.google.ai.edge.litertlm.Message.toText(): String =
    contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }
