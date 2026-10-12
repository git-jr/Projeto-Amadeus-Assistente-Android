package com.paradoxo.amadeus.ui.demoactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwipeVertical
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.ActionType

private data class DemoScenarioDisplay(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val actions: List<ActionIntent>,
    val isReadScreen: Boolean = false,
)

private val ColorPrimary = Color(0xFFF50057)
private val ColorDarkPurple = Color(0xFF290f3e)
private val ColorSuccess = Color(0xFF4CAF50)

private val DEMOS = listOf(
    DemoScenarioDisplay(
        id = "open_app",
        title = "Abrir apps",
        description = "Abre YouTube por comando, volta Amadeus",
        icon = Icons.Rounded.Android,
        color = Color(0xFF4CAF50),
        actions = listOf(
            ActionIntent(ActionType.OPEN_APP, target = "YouTube"),
            ActionIntent(ActionType.WAIT, value = "2000"),
            ActionIntent(ActionType.RECENTS),
            ActionIntent(ActionType.WAIT, value = "500"),
            ActionIntent(ActionType.OPEN_APP, target = "Amadeus"),
            ActionIntent(ActionType.WAIT, value = "5 00"),
            ActionIntent(ActionType.HOME),
        )
    ),
    DemoScenarioDisplay(
        id = "read_screen",
        title = "Ler o conteúdo da tela",
        description = "Analisa todos os elementos visíveis na tela atual",
        icon = Icons.Rounded.Visibility,
        color = Color(0xFF2196F3),
        actions = emptyList(),
        isReadScreen = true,
    ),
    DemoScenarioDisplay(
        id = "tap",
        title = "Executar toques",
        description = "Abre YouTube e clica em alguns elementos da tela",
        icon = Icons.Rounded.TouchApp,
        color = Color(0xFFFF9800),
        actions = listOf(
            ActionIntent(ActionType.OPEN_APP, target = "YouTube"),
            ActionIntent(ActionType.WAIT, value = "200"),
            ActionIntent(ActionType.CLICK, target = "Novidades para você"),
            ActionIntent(ActionType.CLICK, target = "Música"),
            ActionIntent(ActionType.CLICK, target = "Tudo"),
        )
    ),
    DemoScenarioDisplay(
        id = "type",
        title = "Digitar textos",
        description = "Abre pesquisa e digita texto automaticamente",
        icon = Icons.Rounded.Keyboard,
        color = Color(0xFF9C27B0),
        actions = listOf(
            ActionIntent(ActionType.OPEN_APP, target = "Google"),
            ActionIntent(ActionType.WAIT, value = "1000"),
            ActionIntent(ActionType.CLICK_COORDS, value = "500,580"),
            ActionIntent(ActionType.TYPE, value = "Olá, eu sou Amadeus"),
            ActionIntent(ActionType.WAIT, value = "1000"),
            ActionIntent(ActionType.HOME),
        )
    ),
    DemoScenarioDisplay(
        id = "scroll",
        title = "Deslizar pela tela",
        description = "Rola para baixo e para cima em Configurações",
        icon = Icons.Rounded.SwipeVertical,
        color = Color(0xFF00BCD4),
        actions = listOf(
            ActionIntent(ActionType.OPEN_APP, target = "config."),
            ActionIntent(ActionType.WAIT, value = "1500"),
            ActionIntent(ActionType.SCROLL, value = "down"),
            ActionIntent(ActionType.SCROLL, value = "down"),
            ActionIntent(ActionType.SCROLL, value = "down"),
            ActionIntent(ActionType.WAIT, value = "500"),
            ActionIntent(ActionType.SCROLL, value = "up"),
            ActionIntent(ActionType.SCROLL, value = "up"),
            ActionIntent(ActionType.HOME),
        )
    ),
    DemoScenarioDisplay(
        id = "navigation",
        title = "Ações de navegação",
        description = "Abre app Play Store, volta, abre recentes, vai para home",
        icon = Icons.Rounded.Navigation,
        color = Color(0xFFF44336),
        actions = listOf(
            ActionIntent(ActionType.OPEN_APP, target = "Play Store"),
            ActionIntent(ActionType.WAIT, value = "1000"),
            ActionIntent(ActionType.BACK),
            ActionIntent(ActionType.WAIT, value = "1000"),
            ActionIntent(ActionType.HOME),
            ActionIntent(ActionType.WAIT, value = "1000"),
            ActionIntent(ActionType.RECENTS),
            ActionIntent(ActionType.WAIT, value = "500"),
            ActionIntent(ActionType.OPEN_APP, target = "Amadeus"),
        )
    ),
    DemoScenarioDisplay(
        id = "all",
        title = "Tudo em sequência",
        description = "Roda todas as demos em sequência para GIF completo",
        icon = Icons.Rounded.AutoAwesome,
        color = Color(0xFFE91E63),
        actions = listOf(
            ActionIntent(ActionType.OPEN_APP, target = "WhatsApp"),
            ActionIntent(ActionType.WAIT, value = "2500"),
            ActionIntent(ActionType.HOME),
            ActionIntent(ActionType.WAIT, value = "1000"),
            ActionIntent(ActionType.OPEN_APP, target = "Configurações"),
            ActionIntent(ActionType.WAIT, value = "1500"),
            ActionIntent(ActionType.CLICK, target = "Aplicativos"),
            ActionIntent(ActionType.WAIT, value = "1500"),
            ActionIntent(ActionType.HOME),
            ActionIntent(ActionType.WAIT, value = "1000"),
            ActionIntent(ActionType.OPEN_APP, target = "Google"),
            ActionIntent(ActionType.WAIT, value = "2000"),
            ActionIntent(ActionType.TYPE, value = "Olá, eu sou Amadeus"),
            ActionIntent(ActionType.WAIT, value = "2000"),
            ActionIntent(ActionType.HOME),
            ActionIntent(ActionType.WAIT, value = "1000"),
            ActionIntent(ActionType.OPEN_APP, target = "Configurações"),
            ActionIntent(ActionType.WAIT, value = "1500"),
            ActionIntent(ActionType.SCROLL, value = "down"),
            ActionIntent(ActionType.SCROLL, value = "down"),
            ActionIntent(ActionType.SCROLL, value = "up"),
            ActionIntent(ActionType.HOME),
            ActionIntent(ActionType.WAIT, value = "1000"),
            ActionIntent(ActionType.OPEN_APP, target = "WhatsApp"),
            ActionIntent(ActionType.WAIT, value = "2000"),
            ActionIntent(ActionType.BACK),
            ActionIntent(ActionType.WAIT, value = "1000"),
            ActionIntent(ActionType.RECENTS),
            ActionIntent(ActionType.WAIT, value = "1500"),
            ActionIntent(ActionType.HOME),
        )
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DemoActionsScreen(onBack: () -> Unit) {
    val vm: DemoActionsViewModel = viewModel()
    val uiState by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Demo Ações", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!uiState.serviceActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFEBEE))
                        .padding(12.dp)
                ) {
                    Text(
                        "⚠️ Serviço de acessibilidade não está ativo. Ative-o em Configurações > Acessibilidade > Amadeus.",
                        color = Color(0xFFC62828),
                        fontSize = 13.sp
                    )
                }
            }

            if (uiState.countdown > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A2E))
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Executando em:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${uiState.countdown}",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            if (uiState.statusText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6))
                        .padding(12.dp)
                ) {
                    Text(uiState.statusText, fontSize = 13.sp, color = Color(0xFF374151))
                }
            }

            if (uiState.readResult.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE3F2FD))
                        .padding(12.dp)
                ) {
                    Text(uiState.readResult, fontSize = 12.sp, color = Color(0xFF0D47A1))
                }
            }

            DEMOS.forEach { demo ->
                DemoCard(
                    demo = demo,
                    isRunning = uiState.runningId == demo.id,
                    isDisabled = uiState.runningId != null && uiState.runningId != demo.id,
                    onRun = {
                        vm.runDemo(
                            DemoScenario(
                                id = demo.id,
                                title = demo.title,
                                description = demo.description,
                                actions = demo.actions,
                                isReadScreen = demo.isReadScreen
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DemoCard(
    demo: DemoScenarioDisplay,
    isRunning: Boolean,
    isDisabled: Boolean,
    onRun: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(demo.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = demo.icon,
                    contentDescription = null,
                    tint = demo.color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = demo.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = demo.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                if (isRunning) {
                    Text(
                        text = "▶ executando...",
                        fontSize = 11.sp,
                        color = demo.color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = onRun,
                enabled = !isDisabled && !isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = demo.color,
                    disabledContainerColor = Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(width = 70.dp, height = 50.dp)
            ) {
                if (isRunning) {
                    Text("...", fontSize = 12.sp)
                } else {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Executar",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ver ações",
                fontSize = 11.sp,
                color = demo.color,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = demo.color,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(demo.color.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (demo.isReadScreen) {
                    ActionRow(label = "getVisibleElements()", detail = "lê todos os elementos visíveis", color = demo.color)
                } else {
                    demo.actions.forEach { action ->
                        val (label, detail) = actionLabel(action)
                        ActionRow(label = label, detail = detail, color = demo.color)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ActionRow(label: String, detail: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        if (detail.isNotEmpty()) {
            Text(
                text = detail,
                fontSize = 11.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

private fun actionLabel(action: ActionIntent): Pair<String, String> = when (action.type) {
    ActionType.OPEN_APP -> "OPEN_APP" to action.target
    ActionType.CLICK -> "CLICK" to "\"${action.target}\""
    ActionType.CLICK_COORDS -> "CLICK_COORDS" to action.value
    ActionType.TYPE -> "TYPE" to "\"${action.value}\""
    ActionType.SCROLL -> "SCROLL" to action.value
    ActionType.SCROLL_HORIZONTAL -> "SCROLL_H" to action.value
    ActionType.IME_ACTION -> "IME_ACTION" to ""
    ActionType.BACK -> "BACK" to ""
    ActionType.HOME -> "HOME" to ""
    ActionType.RECENTS -> "RECENTS" to ""
    ActionType.NOTIFICATIONS -> "NOTIFICATIONS" to ""
    ActionType.QUICK_SETTINGS -> "QUICK_SETTINGS" to ""
    ActionType.WAIT -> "WAIT" to "${(action.value.toLongOrNull() ?: 0) / 1000}s"
}
