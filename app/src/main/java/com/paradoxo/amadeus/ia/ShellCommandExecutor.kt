package com.paradoxo.amadeus.ia

import java.util.concurrent.TimeUnit

class ShellCommandExecutor {

    fun execute(command: String): GroqToolExecutionResult {
        val sanitizedCommand = command.trim()
        val validationMessage = validate(sanitizedCommand)
        if (validationMessage != null) {
            return GroqToolExecutionResult(
                status = "blocked",
                command = sanitizedCommand,
                message = validationMessage
            )
        }

        return runCatching {
            val process = ProcessBuilder("sh", "-c", sanitizedCommand)
                .redirectErrorStream(false)
                .start()

            val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@runCatching GroqToolExecutionResult(
                    status = "timeout",
                    command = sanitizedCommand,
                    message = "O comando excedeu o tempo maximo permitido."
                )
            }

            val stdout = process.inputStream.bufferedReader().use { it.readText() }
            val stderr = process.errorStream.bufferedReader().use { it.readText() }

            GroqToolExecutionResult(
                status = if (process.exitValue() == 0) "ok" else "error",
                command = sanitizedCommand,
                exitCode = process.exitValue(),
                stdout = stdout.take(MAX_OUTPUT_CHARS).trim(),
                stderr = stderr.take(MAX_OUTPUT_CHARS).trim()
            )
        }.getOrElse { error ->
            GroqToolExecutionResult(
                status = "error",
                command = sanitizedCommand,
                message = error.message ?: "Falha ao executar o comando."
            )
        }
    }

    private fun validate(command: String): String? {
        if (command.isBlank()) return "Nenhum comando foi informado."
        if (command.length > MAX_COMMAND_CHARS) return "O comando e longo demais."
        if (FORBIDDEN_SYNTAX.containsMatchIn(command)) {
            return "O comando foi bloqueado por conter encadeamento, redirecionamento ou substituicao."
        }
        if (FORBIDDEN_KEYWORDS.containsMatchIn(command)) {
            return "O comando foi bloqueado por conter uma operacao destrutiva ou privilegiada."
        }

        val lowerCommand = command.lowercase()
        val isAllowed = ALLOWED_PREFIXES.any { prefix -> lowerCommand.startsWith(prefix) }
        return if (isAllowed) {
            null
        } else {
            "Comando fora da lista permitida pelo app."
        }
    }

    companion object {
        private const val TIMEOUT_SECONDS = 10L
        private const val MAX_OUTPUT_CHARS = 4_000
        private const val MAX_COMMAND_CHARS = 240

        private val FORBIDDEN_SYNTAX = Regex("""[\n\r;&|`><]|\$\(""")
        private val FORBIDDEN_KEYWORDS = Regex(
            pattern = """(^|\s)(rm|mv|dd|mkfs|reboot|shutdown|poweroff|su|sudo|kill|pkill|killall|chmod|chown|mount|umount|settings\s+put|pm\s+clear|pm\s+uninstall|am\s+force-stop)(\s|$)""",
            options = setOf(RegexOption.IGNORE_CASE)
        )
        private val ALLOWED_PREFIXES = listOf(
            "ls",
            "pwd",
            "echo",
            "cat",
            "date",
            "whoami",
            "id",
            "ps",
            "getprop",
            "pm list packages",
            "pm path",
            "cmd package resolve-activity",
            "am start",
            "am broadcast",
            "dumpsys",
            "settings get",
            "input keyevent",
            "input text",
            "monkey -p"
        )
    }
}
