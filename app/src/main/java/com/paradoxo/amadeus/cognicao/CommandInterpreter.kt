package com.paradoxo.amadeus.cognicao

import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.ActionType

class CommandInterpreter {

    private val separators = Regex("""\s+(e|depois|então|entao|aí|ai|em seguida|por fim)\s+|,\s*""")

    fun interpret(command: String): List<ActionIntent> {
        val parts = command.trim().split(separators)
        return parts.flatMap { interpretOne(it.trim()) }
    }

    private fun interpretOne(text: String): List<ActionIntent> {
        val lower = text.lowercase()

        // "responder X", "responda X" → TYPE(X) + CLICK(enviar)
        matchPrefix(lower, "responder ", "responda ") { value ->
            return listOf(
                ActionIntent(ActionType.TYPE, value = value),
                ActionIntent(ActionType.CLICK, target = "enviar")
            )
        }

        // "enviar X", "envia X", "mandar X", "manda X" → TYPE(X) + CLICK(enviar)
        matchPrefix(lower, "enviar ", "envia ", "mandar ", "manda ") { value ->
            return listOf(
                ActionIntent(ActionType.TYPE, value = value),
                ActionIntent(ActionType.CLICK, target = "enviar")
            )
        }

        // "digitar X", "digite X", "digita X", "escrever X", "escreva X", "preencher X", "preencha X"
        matchPrefix(lower, "digitar ", "digite ", "digita ", "escrever ", "escreva ", "preencher ", "preencha ") { value ->
            return listOf(ActionIntent(ActionType.TYPE, value = value))
        }

        // "clicar em X", "clique em X", "pressionar X", "toque em X"
        matchPrefix(
            lower,
            "clicar em ", "clicar no ", "clicar na ", "clicar nos ", "clicar nas ",
            "clique em ", "clique no ", "clique na ",
            "pressionar ", "pressione ", "toque em ", "toque no ", "toque na "
        ) { target ->
            return listOf(ActionIntent(ActionType.CLICK, target = target))
        }

        // "abrir X", "abre X", "abra X"
        matchPrefix(lower, "abrir o ", "abrir a ", "abrir os ", "abrir as ", "abrir ", "abre ", "abra ") { name ->
            return listOf(ActionIntent(ActionType.OPEN_APP, target = name))
        }

        return emptyList()
    }

    private inline fun matchPrefix(
        text: String,
        vararg prefixes: String,
        block: (String) -> Unit
    ) {
        for (prefix in prefixes) {
            if (text.startsWith(prefix)) {
                val value = text.removePrefix(prefix).trim()
                if (value.isNotEmpty()) {
                    block(value)
                    return
                }
            }
        }
    }
}
