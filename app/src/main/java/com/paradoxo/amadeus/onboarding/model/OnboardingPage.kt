package com.paradoxo.amadeus.onboarding.model

import androidx.compose.ui.graphics.Color

data class OnboardingPage(
    val title: String,
    val titleSuffix: String = "",
    val subtitle: String,
    val description: String,
    val accentColor: Color,
    val bgColor: Color,
    val contentType: ContentType
) {
    enum class ContentType {
        AGENT_MODE,
        AUTO_TASKS,
        CONTROL,
        GET_STARTED
    }

    companion object {
        val pages = listOf(
            OnboardingPage(
                title = "Modo agente",
                titleSuffix = " ✨",
                subtitle = "Fala o que quer. Ela vê a tela e age.",
                description = "O Modo agente usa IA para identificar os elementos da tela e executar ações reais, passo a passo, em qualquer app.",
                accentColor = Color(0xFFE83D6B),
                bgColor = Color(0xFFFDEDF1),
                contentType = ContentType.AGENT_MODE
            ),
            OnboardingPage(
                title = "Tarefas automáticas",
                subtitle = "Um pedido. Vários passos. Zero toque.",
                description = "Ela clica, digita e navega entre apps sozinha, até concluir o que você pediu.",
                accentColor = Color(0xFF7C5CBF),
                bgColor = Color(0xFFEDE9F7),
                contentType = ContentType.AUTO_TASKS
            ),
            OnboardingPage(
                title = "Tudo sob controle",
                subtitle = "Acompanhe cada ação em tempo real.",
                description = "Um overlay te mostra o que ela está fazendo a cada passo. Cancele quando quiser com um toque.",
                accentColor = Color(0xFF4CAF6B),
                bgColor = Color(0xFFE8F5ED),
                contentType = ContentType.CONTROL
            ),
            OnboardingPage(
                title = "Pronto para começar!",
                subtitle = "É só pedir. O resto ela faz.",
                description = "Ative o Modo agente, diga o que precisa e veja a Amadeus ir lá e trazer o resultado.",
                accentColor = Color(0xFF2196F3),
                bgColor = Color(0xFFE3F2FD),
                contentType = ContentType.GET_STARTED
            )
        )
    }
}
