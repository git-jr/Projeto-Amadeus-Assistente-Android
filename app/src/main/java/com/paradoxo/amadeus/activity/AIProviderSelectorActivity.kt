package com.paradoxo.amadeus.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.auth.AuthManager
import com.paradoxo.amadeus.cognicao.AIProvider
import com.paradoxo.amadeus.cognicao.RemoteConfigManager
import com.paradoxo.amadeus.payments.EntitlementManager
import com.paradoxo.amadeus.payments.SubscriptionPromptDialog
import com.paradoxo.amadeus.payments.findActivity
import com.paradoxo.amadeus.service.AmadeusAccessibilityService
import com.paradoxo.amadeus.service.FloatingButtonAmadeusService
import com.paradoxo.amadeus.util.Preferencias
import com.paradoxo.amadeus.util.canDrawOverlays

private val SelColorPrimary = Color(0xFFF50057)
private val SelColorDarkPurple = Color(0xFF290f3e)
private val SelColorSurface = Color(0xFFF5F5F5)

private data class ProviderOption(
    val provider: AIProvider?, // null representa o modelo básico (sem override, padrão do app)
    val name: String,
    val model: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

class AIProviderSelectorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MaterialTheme {
                AIProviderSelectorScreen(onBack = { finish() })
            }
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AIProviderSelectorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val entitlementManager = remember { EntitlementManager.getInstance(context) }
    val entitlementState by entitlementManager.state.collectAsState()
    val authManager = remember { AuthManager.getInstance(context) }
    val authState by authManager.state.collectAsState()

    var currentRemoteProvider by remember {
        val saved = Preferencias.getPrefString(Preferencias.PREF_AI_REMOTE_PROVIDER, context)
        mutableStateOf(
            runCatching { AIProvider.valueOf(saved.uppercase()) }
                .getOrElse { AIProvider.OPENAI }
                .let { if (it == AIProvider.LOCAL) AIProvider.OPENAI else it }
        )
    }
    var isBasicActive by remember {
        mutableStateOf(Preferencias.getPrefString(Preferencias.PREF_AI_PROVIDER, context).isBlank())
    }
    var showSubscriptionDialog by remember { mutableStateOf(false) }

    val options = listOf(
        ProviderOption(
            provider = null,
            name = "Básico",
            model = "Padrão do app",
            description = "Modelo padrão, usado automaticamente por todos os usuários, sem necessidade de assinatura.",
            icon = Icons.Rounded.Star,
            accentColor = Color(0xFF757575)
        ),
        ProviderOption(
            provider = AIProvider.OPENAI,
            name = "OpenAI",
            model = RemoteConfigManager.openaiModelAgentPro,
            description = "Modelos rápidos e versáteis, ideais para tarefas cotidianas e respostas precisas do agente.",
            icon = Icons.Rounded.AutoAwesome,
            accentColor = Color(0xFF10A37F)
        ),
        ProviderOption(
            provider = AIProvider.GEMINI,
            name = "Google Gemini",
            model = RemoteConfigManager.geminiModel,
            description = "Modelos Google via Firebase AI, com forte suporte a contexto longo e raciocínio.",
            icon = Icons.Rounded.Hub,
            accentColor = Color(0xFF4285F4)
        )
    )

    fun restartFloatingServiceIfActive() {
        val botaoAtivo =
            Preferencias.getPrefBool(ConfigPrimariaActivity.PREF_BOTAO_FLUTUANTE, context, false) &&
                context.canDrawOverlays() &&
                AmadeusAccessibilityService.isActive(context)
        if (botaoAtivo) {
            context.stopService(Intent(context, FloatingButtonAmadeusService::class.java))
            context.startService(Intent(context, FloatingButtonAmadeusService::class.java))
        }
    }

    fun selectProvider(provider: AIProvider?) {
        if (provider == null) {
            // Básico: remove o override e volta a usar o provedor padrão do app (RemoteConfig).
            Preferencias.setPrefString("", Preferencias.PREF_AI_PROVIDER, context)
            isBasicActive = true
            restartFloatingServiceIfActive()
            return
        }

        Preferencias.setPrefString(provider.name, Preferencias.PREF_AI_REMOTE_PROVIDER, context)
        val mainProvider = runCatching {
            AIProvider.valueOf(
                Preferencias.getPrefString(Preferencias.PREF_AI_PROVIDER, context).uppercase()
            )
        }.getOrElse { AIProvider.OPENAI }

        if (mainProvider != AIProvider.LOCAL) {
            Preferencias.setPrefString(provider.name, Preferencias.PREF_AI_PROVIDER, context)
            isBasicActive = false
            restartFloatingServiceIfActive()
        }
        currentRemoteProvider = provider
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Modelo de Inteligência",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SelColorDarkPurple)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SelColorSurface)
                    .padding(16.dp)
            ) {
                Text(
                    "Escolha qual provedor de IA processa os comandos do agente. O modelo exibido é definido remotamente e pode ser atualizado sem necessidade de atualizar o app.",
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    lineHeight = 18.sp
                )
            }

            options.forEach { option ->
                val isLocked = option.provider != null && !entitlementState.canUseAdvancedModels
                ProviderCard(
                    option = option,
                    isSelected = if (option.provider == null) isBasicActive else {
                        !isBasicActive && option.provider == currentRemoteProvider
                    },
                    isLocked = isLocked,
                    onClick = {
                        if (isLocked) {
                            showSubscriptionDialog = true
                        } else {
                            selectProvider(option.provider)
                        }
                    }
                )
            }
        }

        if (showSubscriptionDialog) {
            SubscriptionPromptDialog(
                state = entitlementState,
                authState = authState,
                onDismiss = { showSubscriptionDialog = false },
                onSubscribe = {
                    context.findActivity()?.let { entitlementManager.purchasePremium(it) }
                }
            )
        }
    }
}

@Composable
private fun ProviderCard(
    option: ProviderOption,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, option.accentColor, RoundedCornerShape(16.dp))
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) option.accentColor.copy(alpha = 0.05f) else SelColorSurface
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(option.accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = option.accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        option.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF212121)
                    )
                    Text(
                        option.model,
                        fontSize = 12.sp,
                        color = option.accentColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SelColorPrimary.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = SelColorPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Pro",
                                fontSize = 11.sp,
                                color = SelColorPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selecionado",
                        tint = option.accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                option.description,
                fontSize = 13.sp,
                color = Color(0xFF757575),
                lineHeight = 18.sp
            )
        }
    }
}
