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
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
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
import com.paradoxo.amadeus.service.AmadeusAccessibilityService
import com.paradoxo.amadeus.service.FloatingButtonAmadeusService
import com.paradoxo.amadeus.util.Preferencias
import com.paradoxo.amadeus.util.canDrawOverlays

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

    var acessibilidadeAtiva by remember { mutableStateOf(false) }
    var overlayAtivo by remember { mutableStateOf(false) }
    var microfoneAtivo by remember { mutableStateOf(false) }
    var botaoFlutuanteAtivo by remember { mutableStateOf(false) }
    var wakeWordAtivo by remember { mutableStateOf(false) }
    var reusarMacrosAtivo by remember { mutableStateOf(false) }
    // Sinaliza que o usuário saiu desta tela para conceder uma permissão necessária ao agente.
    // Quando voltar com todas as permissões satisfeitas, o botão flutuante é iniciado automaticamente.
    var pendingActivation by remember { mutableStateOf(false) }

    fun refreshState() {
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

            SectionHeader("Permissões")

            PermissionCard {
                PermissionRow(
                    icon = Icons.Rounded.Visibility,
                    label = "Acessibilidade",
                    descricao = "Necessária para controlar a tela",
                    granted = acessibilidadeAtiva,
                    onFix = {
                        pendingActivation = true
                        AmadeusAccessibilityService.openSettings(context)
                    }
                )
                Divider()
                PermissionRow(
                    icon = Icons.Rounded.Tune,
                    label = "Sobreposição",
                    descricao = "Exibe o botão flutuante sobre apps",
                    granted = overlayAtivo,
                    onFix = {
                        pendingActivation = true
                        overlayLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                )
                Divider()
                PermissionRow(
                    icon = Icons.Rounded.Mic,
                    label = "Microfone",
                    descricao = "Ativa comandos por voz",
                    granted = microfoneAtivo,
                    onFix = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
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

            Spacer(Modifier.height(16.dp))
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
