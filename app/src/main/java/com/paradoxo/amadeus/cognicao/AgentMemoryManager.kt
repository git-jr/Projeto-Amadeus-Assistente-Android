package com.paradoxo.amadeus.cognicao

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.entities.AgentMacroEntity
import com.paradoxo.amadeus.extensions.normalize
import com.paradoxo.amadeus.modelo.ActionIntent

class AgentMemoryManager(context: Context) {

    private val dao = AmadeusDatabase.getInstance(context).agentMacroDAO()
    private val gson = Gson()

    companion object {
        private const val PROMOTION_THRESHOLD = 3
        private const val FAILURE_DEMOTION_THRESHOLD = 3

        private val FILLER_WORDS = setOf(
            "por", "favor", "me", "meu", "minha", "o", "a", "os", "as",
            "de", "do", "da", "dos", "das", "em", "no", "na", "para", "pra",
            "abrir", "abra", "abre", "vai", "ir", "e", "ou", "um", "uma"
        )
    }

    fun normalizeGoal(goal: String): String {
        val normalized = goal.normalize()
            .replace(Regex("[^\\w\\s]"), "")
        val filtered = normalized.split("\\s+".toRegex())
            .filter { it.isNotBlank() && it !in FILLER_WORDS }
            .joinToString(" ")
        return filtered.ifBlank { normalized.trim() }
    }

    fun serializeActions(actions: List<ActionIntent>): String =
        gson.toJson(actions)

    fun deserializeActions(json: String): List<ActionIntent> = try {
        val type = object : TypeToken<List<ActionIntent>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun recallMacro(goal: String): AgentMacroEntity? =
        dao.findConfirmedByGoalKey(normalizeGoal(goal))

    suspend fun recordExecution(goal: String, actions: List<ActionIntent>) {
        if (actions.isEmpty()) return
        val key = normalizeGoal(goal)
        val newJson = serializeActions(actions)
        val existing = dao.findByGoalKey(key)
        val now = System.currentTimeMillis()

        when {
            existing == null -> {
                dao.insert(
                    AgentMacroEntity(
                        goalKey = key,
                        actionsJson = newJson,
                        createdAt = now,
                        lastUsedAt = now
                    )
                )
            }

            existing.actionsJson == newJson -> {
                val newCount = existing.executionCount + 1
                dao.updateCounts(existing.id, newCount, newCount >= PROMOTION_THRESHOLD, now)
            }

            else -> {
                dao.delete(existing)
                dao.insert(
                    AgentMacroEntity(
                        goalKey = key,
                        actionsJson = newJson,
                        createdAt = now,
                        lastUsedAt = now
                    )
                )
            }
        }
    }

    suspend fun markMacroSuccess(macro: AgentMacroEntity) {
        dao.updateSuccessCount(macro.id, macro.successCount + 1, System.currentTimeMillis())
    }

    suspend fun markMacroFailure(macro: AgentMacroEntity) {
        val newFailures = macro.failureCount + 1
        val stillConfirmed = newFailures < FAILURE_DEMOTION_THRESHOLD
        dao.updateFailureCount(macro.id, newFailures, stillConfirmed)
    }
}
