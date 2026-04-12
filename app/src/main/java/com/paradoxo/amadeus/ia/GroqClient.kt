package com.paradoxo.amadeus.ia

import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GroqClient {

    private val gson = Gson()

    suspend fun createChatCompletion(
        apiKey: String,
        request: GroqChatCompletionRequest
    ): GroqMessage = withContext(Dispatchers.IO) {
        val connection = (URL(CHAT_COMPLETIONS_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doInput = true
            doOutput = true
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }

        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(gson.toJson(request))
            }

            val responseCode = connection.responseCode
            val payload = readStream(
                if (responseCode in 200..299) connection.inputStream else connection.errorStream
            )

            if (responseCode !in 200..299) {
                throw IllegalStateException(
                    payload.ifBlank { "Falha ao consultar a Groq. Codigo HTTP $responseCode." }
                )
            }

            val response = gson.fromJson(payload, GroqChatCompletionResponse::class.java)
            response.choices.firstOrNull()?.message
                ?: throw IllegalStateException("A Groq nao retornou nenhuma resposta.")
        } finally {
            connection.disconnect()
        }
    }

    private fun readStream(stream: InputStream?): String {
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            buildString {
                var line: String?
                while (true) {
                    line = reader.readLine() ?: break
                    append(line)
                }
            }
        }
    }

    companion object {
        private const val CHAT_COMPLETIONS_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val TIMEOUT_MS = 20_000
    }
}
