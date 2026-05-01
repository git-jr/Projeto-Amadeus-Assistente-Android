package com.paradoxo.amadeus.cognicao

import com.paradoxo.amadeus.modelo.MacroExample
import com.paradoxo.amadeus.modelo.UiElement
import com.paradoxo.amadeus.service.AmadeusAccessibilityService

object AgentPromptBuilder {

    fun buildElementsList(elements: List<UiElement>): String {
        val interactive = elements.filter {
            (it.isClickable || it.isEditable || it.text != null || it.contentDescription != null) && it.labelLegivel.isNotBlank()
        }
        return interactive.mapIndexed { i, el ->
            val tipo = when {
                el.isEditable -> "EDITÁVEL"
                el.isClickable -> "CLICÁVEL"
                else -> "IMAGEM"
            }
            val classe = el.className?.substringAfterLast('.') ?: "View"
            val coords = if (!el.isClickable && !el.isEditable) " | coords: ${el.bounds.centerX()},${el.bounds.centerY()}" else ""
            val labelPart = if (el.isEditable) "(preenchido: \"${el.labelLegivel}\")" else "| texto: \"${el.labelLegivel}\""
            "[$i] $tipo $labelPart | classe: $classe$coords"
        }.joinToString("\n")
    }

    fun buildSingleShotPrompt(elementsList: String, command: String) = """
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
        - {"action":"CLICK_COORDS","target":"descrição opcional","value":"x,y"}  ← toca em coordenadas (use para elementos IMAGEM da lista)
        - {"action":"TYPE","target":"texto do campo","value":"texto a digitar"}
        - {"action":"OPEN_APP","target":"nome do app","value":""}
        - {"action":"SCROLL","target":"up ou down","value":""}           ← rola a tela verticalmente
        - {"action":"SCROLL_HORIZONTAL","target":"left ou right","value":""}  ← rola horizontalmente
        - {"action":"IME_ACTION","target":"","value":""}                 ← pressiona o botão de busca/enviar/ir do teclado
        - {"action":"BACK","target":"","value":""}                       ← botão Voltar do Android
        - {"action":"HOME","target":"","value":""}                       ← vai para a tela inicial
        - {"action":"RECENTS","target":"","value":""}                    ← abre a lista de apps recentes
        - {"action":"NOTIFICATIONS","target":"","value":""}              ← abre a barra de notificações
        - {"action":"QUICK_SETTINGS","target":"","value":""}             ← abre as configurações rápidas

        PRINCÍPIO: leia o estado atual antes de agir. Se resultados relevantes à meta já são visíveis (sugestões, listas, elementos IMAGEM relacionados), interaja com eles — não refaça buscas nem redigite campos EDITÁVEL que já aparecem como (preenchido:).
        Não use TYPE em campo EDITÁVEL (preenchido:) com conteúdo correto. Não use TYPE + IME_ACTION se resultados relevantes já estão na tela.
        Para confirmar/enviar após TYPE: prefira CLICK no botão visível (ex: "Enviar", "Send", seta de envio). Use IME_ACTION apenas quando não houver botão visível na tela (ex: barra de busca do Twitter/X, YouTube).
        Use CLICK_COORDS para elementos do tipo IMAGEM que aparecem na lista com coordenadas — quando não há nó clicável correspondente (ex: banners de imagem na Netflix). O value deve ser EXATAMENTE as coordenadas fornecidas na lista.
        Use BACK para cancelar, fechar diálogos ou navegar para a tela anterior.
        Use HOME quando precisar sair de qualquer app e retornar à tela inicial.

        Responda apenas com o array JSON:
    """.trimIndent()

    fun buildFewShotBlock(examples: List<MacroExample>): String {
        if (examples.isEmpty()) return ""
        val sb = StringBuilder(
            "\n⚡ MACROS CONFIRMADOS — sequências que já funcionaram para metas similares." +
            " Siga a mesma ordem de ações, adaptando apenas o \"target\" para o texto exato visível na tela:\n"
        )
        examples.forEach { ex ->
            val json = ex.actions.joinToString(",", "[", "]") { a ->
                """{"action":"${a.type.name}","target":"${a.target}","value":"${a.value}"}"""
            }
            sb.append("• \"${ex.goalKey}\" → $json\n")
        }
        return sb.toString()
    }

    fun buildLoopPrompt(
        goal: String,
        historyText: String,
        elementsList: String,
        fewShotExamples: List<MacroExample> = emptyList()
    ) = """
        Você é um assistente que controla um celular Android via acessibilidade.

        App aberto: ${AmadeusAccessibilityService.currentForegroundPackage.ifEmpty { "desconhecido" }}
        Meta do usuário: "$goal"

        Passos já executados:
        $historyText
        ${buildFewShotBlock(fewShotExamples)}
        Elementos interativos visíveis na tela atual (a imagem mostra cada elemento marcado com [N]):
        $elementsList

        Ações disponíveis e quando usar cada uma:
        - CLICK        → toca em um elemento; "target" = texto exato do elemento
        - CLICK_COORDS → toca em coordenadas exatas; "target" = descrição opcional, "value" = "x,y" das coordenadas da lista
        - TYPE         → digita texto em campo editável; "target" = texto do campo, "value" = texto a digitar
        - OPEN_APP     → abre um aplicativo; "target" = nome do app
        - SCROLL       → rola verticalmente; "target" = "up" ou "down"
        - SCROLL_HORIZONTAL → rola horizontalmente; "target" = "left" ou "right"
        - IME_ACTION   → pressiona o botão de enviar/buscar/ir do teclado; target e value vazios
        - BACK         → pressiona o botão Voltar do Android; target e value vazios
        - HOME         → vai para a tela inicial do Android; target e value vazios
        - RECENTS      → abre a lista de apps recentes; target e value vazios
        - NOTIFICATIONS → abre a barra de notificações; target e value vazios
        - QUICK_SETTINGS → abre as configurações rápidas; target e value vazios

        Regras importantes:
        - MACROS CONFIRMADOS: se houver macros de referência acima com meta similar, siga a mesma sequência de tipos de ação (OPEN_APP → CLICK → TYPE → CLICK etc.), ajustando o "target" para o texto exato que aparecer na tela. Não invente uma abordagem diferente quando uma sequência confirmada existe.
        - PRINCÍPIO: leia o estado atual da tela antes de decidir. Se resultados relevantes à meta já são visíveis (sugestões, listas, elementos IMAGEM relacionados), interaja com eles diretamente — não refaça buscas nem redigite campos EDITÁVEL que já aparecem como (preenchido:).
        - Use o texto EXATO do elemento como "target" nas ações CLICK e TYPE.
        - Não use TYPE em campo EDITÁVEL que já está (preenchido:) com o conteúdo correto para a meta.
        - Não use TYPE + IME_ACTION se resultados relevantes já são visíveis na tela.
        - Para confirmar/enviar após TYPE: prefira CLICK no botão visível (ex: "Enviar", "Send", seta de envio). Use IME_ACTION apenas quando não houver botão de confirmação visível na tela (ex: barra de busca do Twitter/X, YouTube).
        - Use CLICK_COORDS para elementos do tipo IMAGEM que aparecem na lista com coordenadas (ex: banners de imagem na Netflix sem nó clicável). O value deve ser EXATAMENTE as coordenadas fornecidas na lista.
        - Use BACK para fechar diálogos, popups ou navegar à tela anterior.
        - Prefira sequências de ações curtas e diretas — isso facilita o aprendizado de macros.
        - Se a tela não mudou após um passo, tente uma abordagem diferente em vez de repetir.

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
}
