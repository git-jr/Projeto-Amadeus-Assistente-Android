package com.paradoxo.amadeus.ia

import com.google.gson.annotations.SerializedName

data class GroqChatCompletionRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val tools: List<GroqToolDefinition>? = null,
    @SerializedName("tool_choice")
    val toolChoice: String? = null,
    val temperature: Double = 0.2,
    @SerializedName("max_completion_tokens")
    val maxCompletionTokens: Int = 1024,
    @SerializedName("parallel_tool_calls")
    val parallelToolCalls: Boolean = false
)

data class GroqChatCompletionResponse(
    val choices: List<GroqChoice> = emptyList()
)

data class GroqChoice(
    val message: GroqMessage,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class GroqMessage(
    val role: String,
    val content: String? = null,
    @SerializedName("tool_call_id")
    val toolCallId: String? = null,
    val name: String? = null,
    @SerializedName("tool_calls")
    val toolCalls: List<GroqToolCall>? = null
)

data class GroqToolDefinition(
    val type: String = "function",
    val function: GroqToolFunction
)

data class GroqToolFunction(
    val name: String,
    val description: String,
    val parameters: Map<String, @JvmSuppressWildcards Any>
)

data class GroqToolCall(
    val id: String,
    val type: String,
    val function: GroqFunctionCall
)

data class GroqFunctionCall(
    val name: String,
    val arguments: String
)

data class GroqShellCommandArgs(
    val command: String = ""
)

data class GroqToolExecutionResult(
    val status: String,
    val command: String,
    val exitCode: Int? = null,
    val stdout: String = "",
    val stderr: String = "",
    val message: String? = null
)

data class GroqConfig(
    val apiKey: String,
    val model: String,
    val shellEnabled: Boolean
)
