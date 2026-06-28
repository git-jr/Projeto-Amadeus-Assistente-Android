package com.paradoxo.amadeus.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.auth.AuthManager
import com.paradoxo.amadeus.cognicao.AIProvider
import com.paradoxo.amadeus.payments.EntitlementManager
import com.paradoxo.amadeus.payments.SubscriptionPromptDialog
import com.paradoxo.amadeus.payments.findActivity
import com.paradoxo.amadeus.service.AmadeusAccessibilityService
import com.paradoxo.amadeus.service.FloatingButtonAmadeusService
import com.paradoxo.amadeus.util.Preferencias
import com.paradoxo.amadeus.util.canDrawOverlays

private enum class PermissaoDialog { ACESSIBILIDADE, OVERLAY, MICROFONE }

private val ColorPrimary = Color(0xFFF50057)
private val ColorDarkPurple = Color(0xFF290f3e)
private val ColorSuccess = Color(0xFF4CAF50)
private val ColorSurface = Color(0xFFF5F5F5)

class AgentDashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AgentDashboardScreen(onBack = { finish() })
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
private fun AgentDashboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val authManager = remember { AuthManager.getInstance(context) }
    val authState by authManager.state.collectAsState()
    val entitlementManager = remember { EntitlementManager.getInstance(context) }
    val entitlementState by entitlementManager.state.collectAsState()

    var acessibilidadeAtiva by remember { mutableStateOf(false) }
    var overlayAtivo by remember { mutableStateOf(false) }
    var microfoneAtivo by remember { mutableStateOf(false) }
    var botaoFlutuanteAtivo by remember { mutableStateOf(false) }
    var wakeWordAtivo by remember { mutableStateOf(false) }
    var reusarMacrosAtivo by remember { mutableStateOf(false) }
    var currentProvider by remember { mutableStateOf(AIProvider.OPENAI) }
    var currentRemoteProvider by remember { mutableStateOf(AIProvider.OPENAI) }
    var signatureDialogOpened by remember { mutableStateOf(false) }
    // Sinaliza que o usuário saiu desta tela para conceder uma permissão necessária ao agente.
    // Quando voltar com todas as permissões satisfeitas, o botão flutuante é iniciado automaticamente.
    var pendingActivation by remember { mutableStateOf(false) }
    var dialogAberto by remember { mutableStateOf<PermissaoDialog?>(null) }

    fun refreshState() {
        entitlementManager.refreshEntitlements()
        acessibilidadeAtiva = AmadeusAccessibilityService.isActive(context)
        overlayAtivo = context.canDrawOverlays()
        microfoneAtivo = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val allPermsOk = acessibilidadeAtiva && overlayAtivo
        if (pendingActivation && allPermsOk) {
            Preferencias.setPrefBool(ConfigPrimariaActivity.PREF_BOTAO_FLUTUANTE, true, context)
            context.startService(Intent(context, FloatingButtonAmadeusService::class.java))
            pendingActivation = false
        }
        val prefAtivo = Preferencias.getPrefBool(ConfigPrimariaActivity.PREF_BOTAO_FLUTUANTE, context, false)
        botaoFlutuanteAtivo = allPermsOk && prefAtivo
        wakeWordAtivo = botaoFlutuanteAtivo && Preferencias.getPrefBool(ConfigPrimariaActivity.PREF_WAKE_WORD_ATIVO, context, false)
        reusarMacrosAtivo = Preferencias.getPrefBool(ConfigPrimariaActivity.PREF_REUSAR_MACROS_PROMPT_GEMINI, context, false)
        val savedProvider = Preferencias.getPrefString(Preferencias.PREF_AI_PROVIDER, context)
        currentProvider = runCatching { AIProvider.valueOf(savedProvider.uppercase()) }
            .getOrElse { AIProvider.OPENAI }
        val savedRemoteProvider = Preferencias.getPrefString(Preferencias.PREF_AI_REMOTE_PROVIDER, context)
        currentRemoteProvider = runCatching { AIProvider.valueOf(savedRemoteProvider.uppercase()) }
            .getOrElse { AIProvider.OPENAI }
            .let { if (it == AIProvider.LOCAL) AIProvider.OPENAI else it }
    }

    fun restartFloatingServiceIfActive() {
        if (botaoFlutuanteAtivo) {
            context.stopService(Intent(context, FloatingButtonAmadeusService::class.java))
            context.startService(Intent(context, FloatingButtonAmadeusService::class.java))
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshState() }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Modo Agente",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorDarkPurple)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AgentStatusCard(isRunning = botaoFlutuanteAtivo)

            if (!entitlementState.isPremium) {
                AmadeusProPromoCard(
                    onClick = { context.startActivity(Intent(context, AmadeusProActivity::class.java)) }
                )
            }

            SectionHeader("Permissões")

            PermissionCard {
                PermissionRow(
                    icon = Icons.Rounded.Visibility,
                    label = "Acessibilidade",
                    descricao = "Necessária para controlar a tela",
                    granted = acessibilidadeAtiva,
                    onFix = { dialogAberto = PermissaoDialog.ACESSIBILIDADE }
                )
                Divider()
                PermissionRow(
                    icon = Icons.Rounded.Tune,
                    label = "Sobreposição",
                    descricao = "Exibe o botão flutuante sobre apps",
                    granted = overlayAtivo,
                    onFix = { dialogAberto = PermissaoDialog.OVERLAY }
                )
                Divider()
                PermissionRow(
                    icon = Icons.Rounded.Mic,
                    label = "Microfone",
                    descricao = "Ativa comandos por voz",
                    granted = microfoneAtivo,
                    onFix = { dialogAberto = PermissaoDialog.MICROFONE }
                )
            }

            SectionHeader("Controles")

            ToggleCard {
                ToggleRow(
                    icon = Icons.Rounded.AutoAwesome,
                    label = "Botão Flutuante",
                    descricao = "Ativa o agente sobre outros apps",
                    checked = botaoFlutuanteAtivo,
                    enabled = acessibilidadeAtiva && overlayAtivo,
                    onToggle = { enabled ->
                        if (enabled) {
                            Preferencias.setPrefBool(ConfigPrimariaActivity.PREF_BOTAO_FLUTUANTE, true, context)
                            context.startService(Intent(context, FloatingButtonAmadeusService::class.java))
                        } else {
                            Preferencias.setPrefBool(ConfigPrimariaActivity.PREF_BOTAO_FLUTUANTE, false, context)
                            Preferencias.setPrefBool(ConfigPrimariaActivity.PREF_WAKE_WORD_ATIVO, false, context)
                            context.stopService(Intent(context, FloatingButtonAmadeusService::class.java))
                        }
                        refreshState()
                    }
                )
                Divider()
                ToggleRow(
                    icon = Icons.Rounded.Psychology,
                    label = "Usar aprendizado",
                    descricao = "Macros gravadas serão usadas para deixar o app mais inteligente",
                    checked = reusarMacrosAtivo,
                    enabled = true,
                    onToggle = { enabled ->
                        Preferencias.setPrefBool(ConfigPrimariaActivity.PREF_REUSAR_MACROS_PROMPT_GEMINI, enabled, context)
                        refreshState()
                    }
                )
                Divider()
                ToggleRow(
                    icon = Icons.Rounded.RecordVoiceOver,
                    label = "Palavra de Ativação",
                    descricao = "Acorda o agente por voz",
                    checked = wakeWordAtivo,
                    enabled = botaoFlutuanteAtivo,
                    onToggle = { enabled ->
                        Preferencias.setPrefBool(ConfigPrimariaActivity.PREF_WAKE_WORD_ATIVO, enabled, context)
                        val action = if (enabled) FloatingButtonAmadeusService.ACTION_ENABLE_WAKE_WORD
                        else FloatingButtonAmadeusService.ACTION_DISABLE_WAKE_WORD
                        context.startService(Intent(context, FloatingButtonAmadeusService::class.java).apply {
                            this.action = action
                        })
                        refreshState()
                    }
                )

            }

            SectionHeader("Atalhos")

            ShortcutCard {
                ShortcutRow(
                    icon = Icons.Rounded.Bookmarks,
                    label = "Macros aprendidas",
                    descricao = "Sequências de ações aprendidas e criadas",
                    onClick = { context.startActivity(Intent(context, ListaMacrosActivity::class.java)) }
                )
                Divider()
                ShortcutRow(
                    icon = Icons.Rounded.Android,
                    label = stringResource(R.string.definir_assistente_voz),
                    descricao = stringResource(R.string.definir_assistente_voz_desc),
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        } catch (e: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            Log.e("AgentDashboard", "Não foi possível abrir as configurações de assistente de voz, abrindo configurações gerais. Erro: ${e.message}")
                        }
                    }
                )
                Divider()
                ShortcutRow(
                    icon = Icons.Rounded.Category,
                    label = "Entidades",
                    descricao = "Variáveis usadas pelo agente",
                    onClick = { context.startActivity(Intent(context, ListaEntidadeActivity::class.java)) }
                )
                Divider()
                ShortcutRow(
                    icon = Icons.Rounded.Settings,
                    label = "Configurações Gerais",
                    descricao = "Nomes, voz, dados e mais",
                    onClick = { context.startActivity(Intent(context, ConfigPrimariaActivity::class.java)) }
                )
            }

            SectionHeader("Modelo de IA")

            ShortcutCard {
                ProviderSelectorRow(
                    currentProvider = currentProvider,
                    onModeSelected = { isLocal ->
                        val newProvider = if (isLocal) AIProvider.LOCAL else currentRemoteProvider
                        Preferencias.setPrefString(newProvider.name, Preferencias.PREF_AI_PROVIDER, context)
                        currentProvider = newProvider
                        restartFloatingServiceIfActive()
                    }
                )
                Divider()
                ShortcutRow(
                    icon = Icons.Rounded.SmartToy,
                    label = "Modelos Locais",
                    descricao = "Baixar e gerenciar modelos offline",
                    onClick = { context.startActivity(Intent(context, LiteRTModelManagerActivity::class.java)) }
                )
            }

            SectionHeader("Inteligência")

            ShortcutCard {
                IntelligenceSelectorRow(
                    currentRemoteProvider = currentRemoteProvider,
                    canUseAdvancedModels = entitlementState.canUseAdvancedModels,
                    onLockedClick = { signatureDialogOpened = true },
                    onOpenSelector = {
                        context.startActivity(
                            Intent(context, AIProviderSelectorActivity::class.java)
                        )
                    }
                )
            }

            SectionHeader("Extras")

            ShortcutCard {
                ShortcutRow(
                    icon = Icons.Rounded.PlayArrow,
                    label = "Demo Ações",
                    descricao = "Demonstrar cada ação que o agente pode executar",
                    onClick = { context.startActivity(Intent(context, DemoActionsActivity::class.java)) }
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        when (dialogAberto) {
            PermissaoDialog.ACESSIBILIDADE -> AlertDialog(
                onDismissRequest = { dialogAberto = null },
                title = { Text(stringResource(R.string.acessibilidade_rationale_titulo), fontWeight = FontWeight.SemiBold) },
                text = { Text(stringResource(R.string.acessibilidade_rationale_mensagem)) },
                confirmButton = {
                    TextButton(onClick = {
                        dialogAberto = null
                        pendingActivation = true
                        AmadeusAccessibilityService.openSettings(context)
                    }) { Text(stringResource(R.string.ir_para_configuracoes), color = ColorPrimary) }
                },
                dismissButton = {
                    TextButton(onClick = { dialogAberto = null }) {
                        Text(stringResource(R.string.agora_nao))
                    }
                }
            )
            PermissaoDialog.OVERLAY -> AlertDialog(
                onDismissRequest = { dialogAberto = null },
                title = { Text(stringResource(R.string.permissao_overlay_rationale_titulo), fontWeight = FontWeight.SemiBold) },
                text = { Text(stringResource(R.string.permissao_overlay_rationale_mensagem)) },
                confirmButton = {
                    TextButton(onClick = {
                        dialogAberto = null
                        pendingActivation = true
                        overlayLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                        )
                    }) { Text(stringResource(R.string.ir_para_configuracoes), color = ColorPrimary) }
                },
                dismissButton = {
                    TextButton(onClick = { dialogAberto = null }) {
                        Text(stringResource(R.string.agora_nao))
                    }
                }
            )
            PermissaoDialog.MICROFONE -> AlertDialog(
                onDismissRequest = { dialogAberto = null },
                title = { Text(stringResource(R.string.permissao_microfone_rationale_titulo), fontWeight = FontWeight.SemiBold) },
                text = { Text(stringResource(R.string.permissao_microfone_rationale_mensagem)) },
                confirmButton = {
                    TextButton(onClick = {
                        dialogAberto = null
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }) { Text("Permitir", color = ColorPrimary) }
                },
                dismissButton = {
                    TextButton(onClick = { dialogAberto = null }) {
                        Text(stringResource(R.string.agora_nao))
                    }
                }
            )
            null -> {}
        }

        if (signatureDialogOpened) {
            SubscriptionPromptDialog(
                state = entitlementState,
                authState = authState,
                onDismiss = { signatureDialogOpened = false },
                onSubscribe = {
                    context.findActivity()?.let { entitlementManager.purchasePremium(it) }
                }
            )
        }
    }
}

@Composable
private fun AgentStatusCard(isRunning: Boolean) {
    val gradientColors = if (isRunning) {
        listOf(ColorDarkPurple, Color(0xFF6A1B9A))
    } else {
        listOf(Color(0xFF424242), Color(0xFF616161))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) ColorSuccess else Color(0xFF9E9E9E))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Agente Ativo" else "Agente Inativo",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isRunning)
                    "O botão flutuante está rodando. Toque nele para dar um comando."
                else
                    "Ative o botão flutuante e conceda as permissões para usar o agente.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(10.dp))
            val linkText = buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)) {
                    append("Dúvidas sobre como usar o app? ")
                }
                withLink(
                    LinkAnnotation.Url(
                        url = "https://amadeusai.app/",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                ) {
                    append("Acesse o site ou os vídeos")
                }
            }
            Text(text = linkText, style = TextStyle(lineHeight = 18.sp))
        }
    }
}

@Composable
private fun AmadeusProPromoCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(ColorPrimary, Color(0xFFD50000))))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Amadeus Pro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Ações ilimitadas, memória entre sessões e mais",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = ColorDarkPurple,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun PermissionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorSurface)
    ) { content() }
}

@Composable
private fun ToggleCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorSurface)
    ) { content() }
}

@Composable
private fun ShortcutCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorSurface)
    ) { content() }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    label: String,
    descricao: String,
    granted: Boolean,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onFix()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (granted) ColorSuccess.copy(alpha = 0.12f) else ColorPrimary.copy(
                        alpha = 0.10f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (granted) ColorSuccess else ColorPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF212121))
            Text(descricao, fontSize = 12.sp, color = Color(0xFF757575), lineHeight = 16.sp)
        }
        if (granted) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Concedida",
                tint = ColorSuccess,
                modifier = Modifier.size(22.dp)
            )
        } else {
            TextButton(onClick = onFix) {
                Text("Conceder", color = ColorPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    descricao: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ColorDarkPurple.copy(alpha = if (enabled) 0.10f else 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) ColorDarkPurple else Color(0xFFBDBDBD),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = if (enabled) Color(0xFF212121) else Color(0xFFBDBDBD)
            )
            Text(
                descricao,
                fontSize = 12.sp,
                color = if (enabled) Color(0xFF757575) else Color(0xFFBDBDBD),
                lineHeight = 16.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onToggle else null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ColorPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFBDBDBD)
            )
        )
    }
}

@Composable
private fun ShortcutRow(
    icon: ImageVector,
    label: String,
    descricao: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ColorDarkPurple.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ColorDarkPurple,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF212121))
            Text(descricao, fontSize = 12.sp, color = Color(0xFF757575), lineHeight = 16.sp)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 68.dp)
            .height(0.5.dp)
            .background(Color(0x1F000000))
    )
}

@Composable
private fun ProviderSelectorRow(
    currentProvider: AIProvider,
    onModeSelected: (isLocal: Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isLocal = currentProvider == AIProvider.LOCAL
    val (icon, descricao) = if (isLocal) {
        Icons.Rounded.CloudOff to "Modelo local (offline)"
    } else {
        Icons.Rounded.Hub to "Modelo remoto (nuvem)"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ColorDarkPurple.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ColorDarkPurple,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Modo de execução",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF212121)
            )
            Text(descricao, fontSize = 12.sp, color = Color(0xFF757575), lineHeight = 16.sp)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(14.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Modelo local (offline)") },
                leadingIcon = { Icon(Icons.Rounded.CloudOff, contentDescription = null) },
                onClick = { expanded = false; onModeSelected(true) }
            )
            DropdownMenuItem(
                text = { Text("Modelo remoto (nuvem)") },
                leadingIcon = { Icon(Icons.Rounded.Hub, contentDescription = null) },
                onClick = { expanded = false; onModeSelected(false) }
            )
        }
    }
}

@Composable
private fun IntelligenceSelectorRow(
    currentRemoteProvider: AIProvider,
    canUseAdvancedModels: Boolean,
    onLockedClick: () -> Unit,
    onOpenSelector: () -> Unit
) {
    val (icon, descricao) = when (currentRemoteProvider) {
        AIProvider.GEMINI -> Icons.Rounded.Hub to "Google Gemini (Firebase AI)"
        else -> Icons.Rounded.AutoAwesome to "OpenAI"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (canUseAdvancedModels) onOpenSelector() else onLockedClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ColorDarkPurple.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ColorDarkPurple,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Modelo avançado",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF212121)
            )
            Text(descricao, fontSize = 12.sp, color = Color(0xFF757575), lineHeight = 16.sp)
            if (!canUseAdvancedModels) {
                Text(
                    "Requer assinatura",
                    fontSize = 11.sp,
                    color = ColorPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(14.dp)
        )
    }
}
