package com.paradoxo.amadeus.cognicao

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.ActionType
import com.paradoxo.amadeus.modelo.AgentStepResult

object AgentResponseParser {

    private val gson = Gson()

    fun parseActions(raw: String): List<ActionIntent> {
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

    fun parseStepResult(raw: String): AgentStepResult {
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
        } catch (e: Exception) {
            Log.e("AgentResponseParser", "Erro ao parsear resultado do passo: ${e.message}")
            AgentStepResult(done = true, reason = "Erro ao interpretar resposta")
        }
    }
}
