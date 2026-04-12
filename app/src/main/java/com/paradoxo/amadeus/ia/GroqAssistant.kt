package com.paradoxo.amadeus.ia

import android.content.Context
import com.google.gson.Gson
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.toModel
import com.paradoxo.amadeus.enums.ItemEnum
import com.paradoxo.amadeus.util.Preferencias

class GroqAssistant(private val context: Context) {

    private val client = GroqClient()
    private val gson = Gson()
    private val shellExecutor = ShellCommandExecutor()

    suspend fun responder(entrada: String): String {
        val config = getConfig()
            ?: return MENSAGEM_SEM_CONFIGURACAO

        val messages = buildMessages(entrada)
        val tools = if (config.shellEnabled) listOf(buildShellToolDefinition()) else null

        return executarLoop(
            config = config,
            initialMessages = messages,
            tools = tools
        )
    }

    private suspend fun executarLoop(
        config: GroqConfig,
        initialMessages: List<GroqMessage>,
        tools: List<GroqToolDefinition>?
    ): String {
        val messages = initialMessages.toMutableList()

        repeat(MAX_TOOL_ROUNDS) {
            val responseMessage = client.createChatCompletion(
                apiKey = config.apiKey,
                request = GroqChatCompletionRequest(
                    model = config.model,
                    messages = messages,
                    tools = tools,
                    toolChoice = if (tools.isNullOrEmpty()) null else "auto",
                    temperature = 0.2,
                    maxCompletionTokens = 1024,
                    parallelToolCalls = false
                )
            )

            val toolCalls = responseMessage.toolCalls.orEmpty()
            if (toolCalls.isEmpty()) {
                return responseMessage.content
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: MENSAGEM_RESPOSTA_VAZIA
            }

            messages += responseMessage

            toolCalls.forEach { toolCall ->
                val toolResult = executeToolCall(toolCall, config)
                messages += GroqMessage(
                    role = "tool",
                    toolCallId = toolCall.id,
                    name = toolCall.function.name,
                    content = gson.toJson(toolResult)
                )
            }
        }

        return MENSAGEM_LIMITE_DE_FERRAMENTAS
    }

    private fun executeToolCall(
        toolCall: GroqToolCall,
        config: GroqConfig
    ): GroqToolExecutionResult {
        if (toolCall.function.name != TOOL_SHELL_COMMAND) {
            return GroqToolExecutionResult(
                status = "blocked",
                command = "",
                message = "Ferramenta desconhecida."
            )
        }

        if (!config.shellEnabled) {
            return GroqToolExecutionResult(
                status = "blocked",
                command = "",
                message = "A ferramenta de shell esta desativada nas configuracoes."
            )
        }

        val args = runCatching {
            gson.fromJson(toolCall.function.arguments, GroqShellCommandArgs::class.java)
        }.getOrNull()

        return shellExecutor.execute(args?.command.orEmpty())
    }

    private suspend fun buildMessages(entrada: String): List<GroqMessage> {
        val history = AmadeusDatabase.getInstance(context)
            .sentencaDAO()
            .listarHistoricoComLimite(HISTORY_LIMIT)
            .asReversed()
            .map { it.toModel() }
            .filter { it.tipo_item == ItemEnum.IA.ordinal || it.tipo_item == ItemEnum.USUARIO.ordinal }
            .mapNotNull { sentenca ->
                val content = sentenca.respostas.firstOrNull()?.trim().orEmpty()
                when {
                    content.isBlank() -> null
                    sentenca.tipo_item == ItemEnum.USUARIO.ordinal -> GroqMessage("user", content)
                    else -> GroqMessage("assistant", content)
                }
            }
            .toMutableList()

        val currentMessage = entrada.trim()
        val shouldAppendCurrentMessage = history.lastOrNull()?.content != currentMessage
        if (currentMessage.isNotBlank() && shouldAppendCurrentMessage) {
            history += GroqMessage(role = "user", content = currentMessage)
        }

        return buildList {
            add(GroqMessage(role = "system", content = buildSystemPrompt()))
            addAll(history)
        }
    }

    private fun buildShellToolDefinition(): GroqToolDefinition {
        val parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "command" to mapOf(
                    "type" to "string",
                    "description" to "Single Android shell command to execute without chaining or redirection."
                )
            ),
            "required" to listOf("command")
        )

        return GroqToolDefinition(
            function = GroqToolFunction(
                name = TOOL_SHELL_COMMAND,
                description = "Run a short Android shell command only when the user explicitly asks to inspect or execute something on the device.",
                parameters = parameters
            )
        )
    }

    private fun buildSystemPrompt(): String {
        val assistantName = Preferencias.getPrefString(PREF_NOME_IA, context).ifBlank { "Amadeus" }
        val userName = Preferencias.getPrefString(PREF_NOME_USU, context).ifBlank { "usuario" }

        return """
            Voce e $assistantName, a assistente principal do app Android Amadeus.
            Responda sempre em portugues do Brasil.
            Fale de forma objetiva, util e amigavel.
            O usuario principal se chama $userName.
            Algumas acoes locais do app ja sao tratadas fora de voce, como abrir apps, musica, data e hora.
            Nao afirme que executou uma acao do dispositivo sem confirmacao de ferramenta.
            Se a ferramenta run_shell_command estiver disponivel, use-a somente quando o usuario pedir explicitamente para executar, verificar ou diagnosticar algo no shell do aparelho.
            Nunca invente saida de shell.
            Nunca solicite, revele ou repita chaves de API.
            Se uma ferramenta falhar, explique a limitacao e sugira um caminho seguro.
        """.trimIndent()
    }

    private fun getConfig(): GroqConfig? {
        val apiKey = Preferencias.getPrefString(PREF_GROQ_API_KEY, context).trim()
        if (apiKey.isBlank()) return null

        val model = Preferencias.getPrefString(PREF_GROQ_MODEL, context)
            .trim()
            .ifBlank { DEFAULT_MODEL }

        return GroqConfig(
            apiKey = apiKey,
            model = model,
            shellEnabled = Preferencias.getPrefBool(PREF_SHELL_TOOL_ATIVO, context, false)
        )
    }

    companion object {
        const val PREF_GROQ_API_KEY = "groq_api_key"
        const val PREF_GROQ_MODEL = "groq_model"
        const val PREF_SHELL_TOOL_ATIVO = "shell_tool_ativo"
        private const val PREF_NOME_IA = "nomeIA"
        private const val PREF_NOME_USU = "nomeUsu"
        const val DEFAULT_MODEL = "llama-3.1-8b-instant"
        private const val HISTORY_LIMIT = 12L
        private const val MAX_TOOL_ROUNDS = 3
        private const val TOOL_SHELL_COMMAND = "run_shell_command"
        const val MENSAGEM_SEM_CONFIGURACAO =
            "Configure sua chave da Groq em Perfil e IA para ativar respostas generativas."
        private const val MENSAGEM_RESPOSTA_VAZIA =
            "A Groq nao retornou texto util desta vez. Tente reformular o pedido."
        private const val MENSAGEM_LIMITE_DE_FERRAMENTAS =
            "Nao consegui concluir essa tarefa com seguranca apos varias tentativas."

        fun isConfigured(context: Context): Boolean =
            Preferencias.getPrefString(PREF_GROQ_API_KEY, context).isNotBlank()
    }
}
