package com.paradoxo.amadeus.dao.room

import com.google.gson.Gson
import com.paradoxo.amadeus.dao.room.entities.AgentMacroEntity
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.ActionType

object DefaultMacroSeeder {

    private val gson = Gson()

    private data class MacroSeed(val goalKey: String, val actions: List<ActionIntent>)

    private val macros = listOf(
        MacroSeed(
            goalKey = "Manda mensagem no WhatsApp para a Maria",
            actions = listOf(
                ActionIntent(ActionType.OPEN_APP, target = "WhatsApp"),
                ActionIntent(ActionType.CLICK, target = "Pesquisar"),
                ActionIntent(ActionType.TYPE, value = "Maria"),
                ActionIntent(ActionType.CLICK, target = "Maria"),
                ActionIntent(ActionType.CLICK, target = "Mensagem"),
                ActionIntent(ActionType.TYPE, value = "oi, tudo bem?"),
                ActionIntent(ActionType.CLICK, target = "Enviar")
            )
        ),
        MacroSeed(
            goalKey = "Cria lembrete de reunião às 10h",
            actions = listOf(
                ActionIntent(ActionType.OPEN_APP, target = "Agenda"),
                ActionIntent(ActionType.CLICK, target = "Novo evento"),
                ActionIntent(ActionType.TYPE, value = "Reunião"),
                ActionIntent(ActionType.CLICK, target = "Salvar")
            )
        ),
        MacroSeed(
            goalKey = "Abre Spotify e toca The Beatles",
            actions = listOf(
                ActionIntent(ActionType.OPEN_APP, target = "Spotify"),
                ActionIntent(ActionType.CLICK, target = "Pesquisar"),
                ActionIntent(ActionType.TYPE, value = "The Beatles"),
                ActionIntent(ActionType.IME_ACTION),
                ActionIntent(ActionType.CLICK, target = "The Beatles"),
                ActionIntent(ActionType.CLICK, target = "Reproduzir")
            )
        ),
        MacroSeed(
            goalKey = "Compartilha screenshot no WhatsApp para o João",
            actions = listOf(
                ActionIntent(ActionType.QUICK_SETTINGS),
                ActionIntent(ActionType.CLICK, target = "Captura de tela"),
                ActionIntent(ActionType.OPEN_APP, target = "WhatsApp"),
                ActionIntent(ActionType.CLICK, target = "Pesquisar"),
                ActionIntent(ActionType.TYPE, value = "João"),
                ActionIntent(ActionType.CLICK, target = "João"),
                ActionIntent(ActionType.CLICK, target = "Anexar"),
                ActionIntent(ActionType.CLICK, target = "Galeria"),
                ActionIntent(ActionType.CLICK, target = "Enviar")
            )
        ),
        MacroSeed(
            goalKey = "Compõe e-mail no Gmail com assunto Reunião",
            actions = listOf(
                ActionIntent(ActionType.OPEN_APP, target = "Gmail"),
                ActionIntent(ActionType.CLICK, target = "Escrever"),
                ActionIntent(ActionType.TYPE, value = "jr@email.com"),
                ActionIntent(ActionType.CLICK, target = "Assunto"),
                ActionIntent(ActionType.TYPE, value = "Reunião"),
                ActionIntent(ActionType.CLICK, target = "Corpo do e-mail")
            )
        ),
        MacroSeed(
            goalKey = "Toca podcast mais recente no Spotify",
            actions = listOf(
                ActionIntent(ActionType.OPEN_APP, target = "Spotify"),
                ActionIntent(ActionType.CLICK, target = "Sua Biblioteca"),
                ActionIntent(ActionType.CLICK, target = "Podcasts"),
                ActionIntent(ActionType.CLICK, target = "Reproduzir")
            )
        ),
        MacroSeed(
            goalKey = "Manda mensagem no Telegram para o grupo Trabalho",
            actions = listOf(
                ActionIntent(ActionType.OPEN_APP, target = "Telegram"),
                ActionIntent(ActionType.CLICK, target = "Pesquisar"),
                ActionIntent(ActionType.TYPE, value = "Trabalho"),
                ActionIntent(ActionType.CLICK, target = "Trabalho"),
                ActionIntent(ActionType.TYPE, value = "Já terminei"),
                ActionIntent(ActionType.CLICK, target = "Enviar")
            )
        ),
        MacroSeed(
            goalKey = "Chamada de vídeo para Ana no WhatsApp",
            actions = listOf(
                ActionIntent(ActionType.OPEN_APP, target = "WhatsApp"),
                ActionIntent(ActionType.CLICK, target = "Pesquisar"),
                ActionIntent(ActionType.TYPE, value = "Ana"),
                ActionIntent(ActionType.CLICK, target = "Ana"),
                ActionIntent(ActionType.CLICK, target = "Videochamada")
            )
        ),
        MacroSeed(
            goalKey = "Navega para Aeroporto de Guarulhos no Maps",
            actions = listOf(
                ActionIntent(ActionType.OPEN_APP, target = "Maps"),
                ActionIntent(ActionType.CLICK, target = "Pesquisar"),
                ActionIntent(ActionType.TYPE, value = "Aeroporto de Guarulhos"),
                ActionIntent(ActionType.IME_ACTION),
                ActionIntent(ActionType.CLICK, target = "Como chegar"),
                ActionIntent(ActionType.CLICK, target = "Iniciar")
            )
        ),
        MacroSeed(
            goalKey = "Ativa modo economia de bateria",
            actions = listOf(
                ActionIntent(ActionType.OPEN_APP, target = "Configurações"),
                ActionIntent(ActionType.CLICK, target = "Bateria"),
                ActionIntent(ActionType.CLICK, target = "Economia de bateria"),
                ActionIntent(ActionType.CLICK, target = "Ativar agora")
            )
        )
    )

    suspend fun seed(db: AmadeusDatabase) {
        val dao = db.agentMacroDAO()
        val now = System.currentTimeMillis()
        for (macro in macros) {
            if (dao.findByGoalKey(macro.goalKey) == null) {
                dao.insert(
                    AgentMacroEntity(
                        goalKey = macro.goalKey,
                        actionsJson = gson.toJson(macro.actions),
                        executionCount = 3,
                        isConfirmed = true,
                        successCount = 3,
                        createdAt = now,
                        lastUsedAt = now
                    )
                )
            }
        }
    }
}
