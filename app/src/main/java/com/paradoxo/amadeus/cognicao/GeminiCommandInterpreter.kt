package com.paradoxo.amadeus.cognicao

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.ActionType
import com.paradoxo.amadeus.modelo.AgentStepResult
import com.paradoxo.amadeus.modelo.UiElement

class GeminiCommandInterpreter(private val apiKey: String) {

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash-lite",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.1f
                maxOutputTokens = 512
            }
        )
    }

    private val gson = Gson()

    // ── Single-shot (mantido como fallback rápido) ────────────────────────────

    suspend fun interpret(command: String, elements: List<UiElement>): List<ActionIntent> {
        if (apiKey.isBlank() || apiKey == "sua_chave_aqui") return emptyList()

        val interactive = elements.filter {
            (it.isClickable || it.isEditable) && it.labelLegivel.isNotBlank()
        }
        if (interactive.isEmpty()) return emptyList()

        val elementsList = interactive.mapIndexed { i, el ->
            val tipo = if (el.isEditable) "EDITÁVEL" else "CLICÁVEL"
            val classe = el.className?.substringAfterLast('.') ?: "View"
            "[$i] $tipo | texto: \"${el.labelLegivel}\" | classe: $classe"
        }.joinToString("\n")

        return try {
            val response = model.generateContent(buildSingleShotPrompt(elementsList, command))
            parseActions(response.text ?: return emptyList())
        } catch (e: Exception) {
            Log.e("GeminiInterpreter", "Erro ao interpretar comando: ${e.message}")
            emptyList()
        }
    }

    // ── Loop agêntico ─────────────────────────────────────────────────────────

    suspend fun interpretStep(
        goal: String,
        history: List<String>,
        elements: List<UiElement>,
        mapBitmap: Bitmap
    ): AgentStepResult {
        if (apiKey.isBlank() || apiKey == "sua_chave_aqui") return AgentStepResult(done = true, reason = "API key não configurada")

        val interactive = elements.filter {
            (it.isClickable || it.isEditable) && it.labelLegivel.isNotBlank()
        }

        val elementsList = interactive.mapIndexed { i, el ->
            val tipo = if (el.isEditable) "EDITÁVEL" else "CLICÁVEL"
            val classe = el.className?.substringAfterLast('.') ?: "View"
            "[$i] $tipo | texto: \"${el.labelLegivel}\" | classe: $classe"
        }.joinToString("\n").ifEmpty { "Nenhum elemento interativo encontrado" }

        val historyText = if (history.isEmpty()) "Nenhum — este é o primeiro passo"
        else history.joinToString("\n")

        val prompt = buildLoopPrompt(goal, historyText, elementsList)

        return try {
            val response = model.generateContent(
                content {
                    image(mapBitmap)
                    text(prompt)
                }
            )
            parseStepResult(response.text ?: return AgentStepResult(done = true, reason = "Resposta vazia"))
        } catch (e: Exception) {
            Log.e("GeminiInterpreter", "Erro no passo do loop: ${e.message}")
            AgentStepResult(done = true, reason = "Erro: ${e.message}")
        }
    }

    // ── Prompts ───────────────────────────────────────────────────────────────

    private fun buildSingleShotPrompt(elementsList: String, command: String) = """
        Você é um assistente que controla um celular Android via acessibilidade.

        Elementos interativos visíveis na tela:
        $elementsList

        Comando do usuário: "$command"

        Retorne as ações necessárias como um array JSON. Regras:
        - Apenas JSON puro, sem markdown, sem bloco de código, sem explicação
        - "target" deve ser EXATAMENTE o texto do elemento como aparece na lista acima
        - Para enviar mensagem: TYPE no campo editável + CLICK no botão de enviar
        - Se não for possível executar com os elementos disponíveis, retorne []

        Ações disponíveis:
        - {"action":"CLICK","target":"texto exato do elemento","value":""}
        - {"action":"TYPE","target":"texto do campo","value":"texto a digitar"}
        - {"action":"OPEN_APP","target":"nome do app","value":""}
        - {"action":"SCROLL","target":"up ou down","value":""}
        - {"action":"IME_ACTION","target":"","value":""}  ← pressiona o botão de busca/enviar/ir do teclado

        Use IME_ACTION após TYPE quando o botão de confirmar só existe no teclado (ex: busca no Twitter/X, YouTube).

        Responda apenas com o array JSON:
    """.trimIndent()

    private fun buildLoopPrompt(goal: String, historyText: String, elementsList: String) = """
        Você é um assistente que controla um celular Android via acessibilidade.

        Meta do usuário: "$goal"

        Passos já executados:
        $historyText

        Elementos interativos visíveis na tela atual (a imagem mostra cada elemento marcado com [N]):
        $elementsList

        Ações disponíveis: CLICK, TYPE, OPEN_APP, SCROLL, IME_ACTION.
        Use IME_ACTION (target e value vazios) após TYPE quando o botão de confirmar só existe no teclado (ex: busca no Twitter/X, YouTube).
        Use o texto EXATO do elemento como "target" nas ações.

        A meta foi concluída com base no estado atual da tela?
        Retorne APENAS este JSON sem markdown:
        {
          "done": false,
          "reason": "motivo curto",
          "actions": [
            {"action":"CLICK","target":"texto exato","value":""}
          ]
        }
        Se a meta foi concluída: {"done":true,"reason":"motivo curto","actions":[]}
        Se não há como prosseguir: {"done":true,"reason":"não foi possível completar","actions":[]}
    """.trimIndent()

    // ── Parsers ───────────────────────────────────────────────────────────────

    private fun parseActions(raw: String): List<ActionIntent> {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        return try {
            val type = object : TypeToken<List<Map<String, String>>>() {}.type
            val list: List<Map<String, String>> = gson.fromJson(cleaned, type)
            list.mapNotNull { map ->
                val actionType = try {
                    ActionType.valueOf(map["action"] ?: return@mapNotNull null)
                } catch (_: Exception) { return@mapNotNull null }
                ActionIntent(actionType, map["target"] ?: "", map["value"] ?: "")
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun parseStepResult(raw: String): AgentStepResult {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        return try {
            val map: Map<String, Any> = gson.fromJson(cleaned, object : TypeToken<Map<String, Any>>() {}.type)
            val done = (map["done"] as? Boolean) ?: false
            val reason = (map["reason"] as? String) ?: ""

            @Suppress("UNCHECKED_CAST")
            val actionsRaw = map["actions"] as? List<Map<String, String>> ?: emptyList()
            val actions = actionsRaw.mapNotNull { a ->
                val actionType = try {
                    ActionType.valueOf(a["action"] ?: return@mapNotNull null)
                } catch (_: Exception) { return@mapNotNull null }
                ActionIntent(actionType, a["target"] ?: "", a["value"] ?: "")
            }
            AgentStepResult(done, reason, actions)
        } catch (_: Exception) {
            AgentStepResult(done = true, reason = "Erro ao interpretar resposta")
        }
    }
}
