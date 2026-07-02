package com.paradoxo.amadeus.activity

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import coil3.compose.AsyncImage
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.auth.AuthManager
import com.paradoxo.amadeus.auth.AuthUiState
import com.paradoxo.amadeus.payments.EntitlementManager
import com.paradoxo.amadeus.payments.EntitlementSource
import com.paradoxo.amadeus.payments.EntitlementStatus
import com.paradoxo.amadeus.payments.EntitlementUiState
import com.paradoxo.amadeus.service.AmadeusAccessibilityService
import com.paradoxo.amadeus.service.FloatingButtonAmadeusService
import com.paradoxo.amadeus.util.Preferencias
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.canDrawOverlays
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CfgColorPrimary = Color(0xFFF50057)
private val CfgColorDarkPurple = Color(0xFF290f3e)
private val CfgColorSurface = Color(0xFFF5F5F5)

private enum class CfgDialog { ACESSIBILIDADE, OVERLAY }

class ConfigPrimariaActivity : ComponentActivity() {

    companion object {
        const val PREF_NOME_IA = "nomeIA"
        const val PREF_NOME_USU = "nomeUsu"
        const val PREF_VOZ_ATIVA = "voz_ativa"
        const val PREF_UPLOAD_DADOS_AUTORIZADO = "upload_dados_autorizado"
        const val PREF_BOTAO_FLUTUANTE = "botao_flutuante"
        const val PREF_WAKE_WORD_ATIVO = "wake_word_ativo"
        const val PREF_REUSAR_MACROS_PROMPT_GEMINI = "reusar_macros_prompt_gemini"
        const val EXTRA_SOLICITAR_ACESSIBILIDADE = "solicitar_acessibilidade"
        const val EXTRA_ATIVAR_BOTAO_FLUTUANTE_APOS_ACESSIBILIDADE =
            "ativar_botao_flutuante_apos_acessibilidade"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestAcessibilidade =
            intent?.getBooleanExtra(EXTRA_SOLICITAR_ACESSIBILIDADE, false) ?: false
        val activateAfterAcessibilidade = intent?.getBooleanExtra(
            EXTRA_ATIVAR_BOTAO_FLUTUANTE_APOS_ACESSIBILIDADE, true
        ) ?: true
        setContent {
            MaterialTheme {
                ConfigPrimariaScreen(
                    initialRequestAccessibility = requestAcessibilidade,
                    initialActivateFloatingAfterAccessibility = activateAfterAcessibilidade,
                    onSignIn = {
                        AuthManager.getInstance(this).signInWithGoogleAsync(this) { success, _, errorMessage ->
                            if (success) {
                                EntitlementManager.getInstance(this).refreshEntitlements()
                                meuToast(getString(R.string.conta_conectada), this)
                            } else {
                                errorMessage?.let { meuToast(it, this) }
                            }
                        }
                    },
                    onBack = { finish() }
                )
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
private fun ConfigPrimariaScreen(
    initialRequestAccessibility: Boolean,
    initialActivateFloatingAfterAccessibility: Boolean,
    onSignIn: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val authManager = remember { AuthManager.getInstance(context) }
    val authState by authManager.state.collectAsState()
    val entitlementManager = remember { EntitlementManager.getInstance(context) }
    val entitlementState by entitlementManager.state.collectAsState()

    // Atualiza ao abrir a tela e sempre que o usuário autenticado mudar,
    // independentemente de onde o login tenha sido realizado.
    LaunchedEffect(authState.uid) {
        entitlementManager.refreshEntitlements()
    }

    // Toggle state
    var modoFalaAtivo by remember { mutableStateOf(false) }
    var acessoDadosAtivo by remember { mutableStateOf(false) }
    var acessibilidadeAtiva by remember { mutableStateOf(false) }
    var botaoFlutuanteAtivo by remember { mutableStateOf(false) }
    var wakeWordAtivo by remember { mutableStateOf(false) }
    var reusarMacrosAtivo by remember { mutableStateOf(false) }

    // Name fields — loaded once from prefs
    var nomeUsuario by remember {
        mutableStateOf(Preferencias.getPrefString(ConfigPrimariaActivity.PREF_NOME_USU, context))
    }
    var nomeIA by remember {
        mutableStateOf(Preferencias.getPrefString(ConfigPrimariaActivity.PREF_NOME_IA, context))
    }
    var nomeUsuarioError by remember { mutableStateOf(false) }
    var nomeIAError by remember { mutableStateOf(false) }

    // Pending permission flow state
    var ativarBotaoFlutuanteAposAcessibilidade by remember {
        mutableStateOf(initialActivateFloatingAfterAccessibility)
    }
    var aguardandoRetornoAcessibilidade by remember { mutableStateOf(false) }
    var aguardandoRetornoAcessibilidadeAtivacao by remember { mutableStateOf(false) }
    var ativarBotaoAposAtivarAcessibilidade by remember { mutableStateOf(false) }
    var solicitarAcessibilidadeAoEntrar by remember { mutableStateOf(initialRequestAccessibility) }

    var dialogAberto by remember { mutableStateOf<CfgDialog?>(null) }

    fun ativarBotaoFlutuante() {
        if (!context.canDrawOverlays() || !AmadeusAccessibilityService.isActive(context)) return
        Preferencias.setPrefBool(ConfigPrimariaActivity.PREF_BOTAO_FLUTUANTE, true, context)
        context.startService(Intent(context, FloatingButtonAmadeusService::class.java))
    }

    fun desativarBotaoFlutuanteCompleto() {
        Preferencias.setPrefBool(ConfigPrimariaActivity.PREF_BOTAO_FLUTUANTE, false, context)
        Preferencias.setPrefBool(ConfigPrimariaActivity.PREF_WAKE_WORD_ATIVO, false, context)
        context.stopService(Intent(context, FloatingButtonAmadeusService::class.java))
    }

    fun syncToggles() {
        val acessibilidade = AmadeusAccessibilityService.isActive(context)
        val overlay = context.canDrawOverlays()
        val prefBotao =
            Preferencias.getPrefBool(ConfigPrimariaActivity.PREF_BOTAO_FLUTUANTE, context, false)
        val allPermsOk = acessibilidade && overlay
        val botaoAtivo = allPermsOk && prefBotao

        if (!botaoAtivo) {
            Preferencias.setPrefBool(ConfigPrimariaActivity.PREF_BOTAO_FLUTUANTE, false, context)
            Preferencias.setPrefBool(ConfigPrimariaActivity.PREF_WAKE_WORD_ATIVO, false, context)
        }

        acessibilidadeAtiva = acessibilidade
        modoFalaAtivo =
            Preferencias.getPrefBool(ConfigPrimariaActivity.PREF_VOZ_ATIVA, context, false)
        acessoDadosAtivo = Preferencias.getPrefBool(
            ConfigPrimariaActivity.PREF_UPLOAD_DADOS_AUTORIZADO, context, true
        )
        reusarMacrosAtivo = Preferencias.getPrefBool(
            ConfigPrimariaActivity.PREF_REUSAR_MACROS_PROMPT_GEMINI, context, false
        )
        botaoFlutuanteAtivo = botaoAtivo
        wakeWordAtivo = botaoAtivo && Preferencias.getPrefBool(
            ConfigPrimariaActivity.PREF_WAKE_WORD_ATIVO, context, false
        )
    }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (context.canDrawOverlays()) {
            if (AmadeusAccessibilityService.isActive(context)) {
                ativarBotaoFlutuante()
            } else {
                ativarBotaoFlutuanteAposAcessibilidade = true
                aguardandoRetornoAcessibilidade = true
                dialogAberto = CfgDialog.ACESSIBILIDADE
            }
        }
        syncToggles()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (aguardandoRetornoAcessibilidadeAtivacao) {
                    aguardandoRetornoAcessibilidadeAtivacao = false
                    if (ativarBotaoAposAtivarAcessibilidade &&
                        AmadeusAccessibilityService.isActive(context) &&
                        context.canDrawOverlays()
                    ) {
                        ativarBotaoFlutuante()
                    }
                    ativarBotaoAposAtivarAcessibilidade = false
                    syncToggles()
                    return@LifecycleEventObserver
                }

                if (aguardandoRetornoAcessibilidade) {
                    aguardandoRetornoAcessibilidade = false
                    if (AmadeusAccessibilityService.isActive(context) &&
                        context.canDrawOverlays() &&
                        ativarBotaoFlutuanteAposAcessibilidade
                    ) {
                        ativarBotaoFlutuante()
                    }
                    ativarBotaoFlutuanteAposAcessibilidade =
                        initialActivateFloatingAfterAccessibility
                    syncToggles()
                    return@LifecycleEventObserver
                }

                if (solicitarAcessibilidadeAoEntrar &&
                    !AmadeusAccessibilityService.isActive(context)
                ) {
                    solicitarAcessibilidadeAoEntrar = false
                    dialogAberto = CfgDialog.ACESSIBILIDADE
                }

                syncToggles()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun salvarNomes(): Boolean {
        if (nomeUsuario.isBlank()) {
            nomeUsuarioError = true
            return false
        }
        if (nomeIA.isBlank()) {
            nomeIAError = true
            return false
        }
        Preferencias.setPrefString(
            nomeUsuario.trim(),
            ConfigPrimariaActivity.PREF_NOME_USU,
            context
        )
        Preferencias.setPrefString(nomeIA.trim(), ConfigPrimariaActivity.PREF_NOME_IA, context)
        return true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Configurações",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
            // Profile / account card
            AccountHeaderCard(
                authState = authState,
                entitlementState = entitlementState,
                onSignIn = { onSignIn() },
                onSignOut = { authManager.signOut() }
            )

            // Names section
            CfgSectionHeader("Identidade")

            CfgCard {
                CfgNameField(
                    label = stringResource(R.string.digite_seu_nome),
                    value = nomeUsuario,
                    isError = nomeUsuarioError,
                    imeAction = ImeAction.Next,
                    onValueChange = { nomeUsuario = it; nomeUsuarioError = false },
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                )
                CfgDivider()
                CfgNameField(
                    label = stringResource(R.string.digite_um_nome_para_ia),
                    value = nomeIA,
                    isError = nomeIAError,
                    imeAction = ImeAction.Done,
                    onValueChange = { nomeIA = it; nomeIAError = false },
                    onImeAction = { if (salvarNomes()) onBack() }
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { if (salvarNomes()) onBack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CfgColorDarkPurple)
                ) {
                    Text(stringResource(R.string.salvar), fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
            }

            // Voice & data section
            CfgSectionHeader("Voz e Dados")

            CfgCard {
                CfgToggleRow(
                    icon = Icons.Rounded.Mic,
                    label = stringResource(R.string.modo_fala),
                    descricao = stringResource(R.string.respostas_em_voz_alta),
                    checked = modoFalaAtivo,
                    onToggle = {
                        Preferencias.setPrefBool(ConfigPrimariaActivity.PREF_VOZ_ATIVA, it, context)
                        modoFalaAtivo = it
                    }
                )
                CfgDivider()
                CfgToggleRow(
                    icon = Icons.Rounded.CloudUpload,
                    label = stringResource(R.string.permitir_uso_de_dados),
                    descricao = stringResource(R.string.permitir_uso_de_dados_descri_curta),
                    checked = acessoDadosAtivo,
                    onToggle = {
                        Preferencias.setPrefBool(
                            ConfigPrimariaActivity.PREF_UPLOAD_DADOS_AUTORIZADO, it, context
                        )
                        acessoDadosAtivo = it
                    }
                )
                CfgDivider()
                CfgToggleRow(
                    icon = Icons.Rounded.Psychology,
                    label = stringResource(R.string.reusar_macros_prompt_gemini),
                    descricao = "Macros confirmadas são usadas como referência no prompt do agente",
                    checked = reusarMacrosAtivo,
                    onToggle = {
                        Preferencias.setPrefBool(
                            ConfigPrimariaActivity.PREF_REUSAR_MACROS_PROMPT_GEMINI, it, context
                        )
                        reusarMacrosAtivo = it
                    }
                )
            }

            // Agent section
            CfgSectionHeader("Agente")

            CfgCard {
                CfgToggleRow(
                    icon = Icons.Rounded.Visibility,
                    label = stringResource(R.string.observar_tela),
                    descricao = stringResource(R.string.acessibilidade_descricao),
                    checked = acessibilidadeAtiva,
                    onToggle = { ativado ->
                        if (ativado) {
                            if (!AmadeusAccessibilityService.isActive(context)) {
                                aguardandoRetornoAcessibilidadeAtivacao = true
                                ativarBotaoAposAtivarAcessibilidade = context.canDrawOverlays()
                                dialogAberto = CfgDialog.ACESSIBILIDADE
                            }
                        } else {
                            desativarBotaoFlutuanteCompleto()
                            syncToggles()
                        }
                    }
                )
                CfgDivider()
                CfgToggleRow(
                    icon = Icons.Rounded.AutoAwesome,
                    label = stringResource(R.string.botao_flutuante),
                    descricao = "Ativa o agente flutuante sobre outros apps",
                    checked = botaoFlutuanteAtivo,
                    onToggle = { ativado ->
                        if (ativado) {
                            when {
                                !context.canDrawOverlays() -> dialogAberto = CfgDialog.OVERLAY
                                !AmadeusAccessibilityService.isActive(context) -> {
                                    ativarBotaoFlutuanteAposAcessibilidade = true
                                    aguardandoRetornoAcessibilidade = true
                                    dialogAberto = CfgDialog.ACESSIBILIDADE
                                }

                                else -> {
                                    ativarBotaoFlutuante()
                                    syncToggles()
                                }
                            }
                        } else {
                            desativarBotaoFlutuanteCompleto()
                            syncToggles()
                        }
                    }
                )
                CfgDivider()
                CfgToggleRow(
                    icon = Icons.Rounded.RecordVoiceOver,
                    label = stringResource(R.string.escuta_frase_ativacao),
                    descricao = stringResource(R.string.escuta_frase_ativacao_desc),
                    checked = wakeWordAtivo,
                    enabled = botaoFlutuanteAtivo,
                    onToggle = { ativado ->
                        if (!botaoFlutuanteAtivo) return@CfgToggleRow
                        Preferencias.setPrefBool(
                            ConfigPrimariaActivity.PREF_WAKE_WORD_ATIVO, ativado, context
                        )
                        val action =
                            if (ativado) FloatingButtonAmadeusService.ACTION_ENABLE_WAKE_WORD
                            else FloatingButtonAmadeusService.ACTION_DISABLE_WAKE_WORD
                        context.startService(
                            Intent(context, FloatingButtonAmadeusService::class.java).apply {
                                this.action = action
                            }
                        )
                        wakeWordAtivo = ativado
                    }
                )
            }

            // Shortcuts section
            CfgSectionHeader("Atalhos")

            CfgCard {
                CfgShortcutRow(
                    icon = Icons.Rounded.Android,
                    label = stringResource(R.string.definir_assistente_voz),
                    descricao = stringResource(R.string.definir_assistente_voz_desc),
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                )
                CfgDivider()
                CfgShortcutRow(
                    icon = Icons.Rounded.Bookmarks,
                    label = "Macros aprendidas",
                    descricao = "Ver e editar ações gravadas pelo agente",
                    onClick = {
                        context.startActivity(Intent(context, ListaMacrosActivity::class.java))
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        // Permission dialogs
        when (dialogAberto) {
            CfgDialog.ACESSIBILIDADE -> AlertDialog(
                onDismissRequest = {
                    dialogAberto = null
                    aguardandoRetornoAcessibilidadeAtivacao = false
                },
                title = {
                    Text(
                        stringResource(R.string.acessibilidade_rationale_titulo),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = { Text(stringResource(R.string.acessibilidade_rationale_mensagem)) },
                confirmButton = {
                    TextButton(onClick = {
                        dialogAberto = null
                        aguardandoRetornoAcessibilidade = true
                        AmadeusAccessibilityService.openSettings(context)
                    }) {
                        Text(
                            stringResource(R.string.ir_para_configuracoes),
                            color = CfgColorPrimary
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        dialogAberto = null
                        aguardandoRetornoAcessibilidadeAtivacao = false
                    }) {
                        Text(stringResource(R.string.agora_nao))
                    }
                }
            )

            CfgDialog.OVERLAY -> AlertDialog(
                onDismissRequest = { dialogAberto = null },
                title = {
                    Text(
                        stringResource(R.string.permissao_overlay_rationale_titulo),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = { Text(stringResource(R.string.permissao_overlay_rationale_mensagem)) },
                confirmButton = {
                    TextButton(onClick = {
                        dialogAberto = null
                        overlayLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                        )
                    }) {
                        Text(
                            stringResource(R.string.ir_para_configuracoes),
                            color = CfgColorPrimary
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialogAberto = null }) {
                        Text(stringResource(R.string.agora_nao))
                    }
                }
            )

            null -> {}
        }
    }
}

@Composable
private fun SubscriptionInfoSection(entitlementState: EntitlementUiState) {
    val e = entitlementState.entitlement
    val isPremium = entitlementState.isPremium

    val (statusLabel, statusColor) = when (e.status) {
        EntitlementStatus.ACTIVE -> "Ativo" to Color(0xFF2E7D32)
        EntitlementStatus.EXPIRED -> "Expirado" to Color(0xFFC62828)
        EntitlementStatus.CANCELED -> "Cancelado" to Color(0xFFE65100)
        EntitlementStatus.PENDING -> "Pendente" to Color(0xFFF57F17)
        EntitlementStatus.INACTIVE -> "Inativo" to Color(0xFF757575)
    }

    val sourceLabel = when (e.source) {
        EntitlementSource.GOOGLE_PLAY -> "via Google Play"
        EntitlementSource.STRIPE -> "via Stripe"
        EntitlementSource.MANUAL -> "concedido manualmente"
        EntitlementSource.UNKNOWN -> ""
    }

    val expiryLabel = e.expiresAtMillis?.let {
        val fmt = SimpleDateFormat("dd 'de' MMM. 'de' yyyy", Locale("pt", "BR"))
        "Válida até ${fmt.format(Date(it))}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPremium) CfgColorPrimary.copy(alpha = 0.06f) else Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPremium) CfgColorPrimary.copy(alpha = 0.15f)
                        else Color.Black.copy(alpha = 0.06f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = if (isPremium) CfgColorPrimary else Color(0xFF9E9E9E),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPremium) "Amadeus Pro" else "Plano gratuito",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isPremium) CfgColorPrimary else Color(0xFF616161)
                )
                if (isPremium) {
                    if (sourceLabel.isNotBlank()) {
                        Text(
                            text = sourceLabel,
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                    if (expiryLabel != null) {
                        Text(
                            text = expiryLabel,
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
            if (isPremium) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountHeaderCard(
    authState: AuthUiState,
    entitlementState: EntitlementUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    var signingOut by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                val photoUrl = authState.photoUrl
                val initial = authState.displayName?.firstOrNull()?.uppercaseChar()?.toString()
                if (authState.isGoogleSignedIn && photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else if (authState.isGoogleSignedIn && initial != null) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (authState.isGoogleSignedIn) {
                Text(
                    text = authState.displayName ?: "",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = authState.email ?: "",
                    color = Color.Black.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(14.dp))
                SubscriptionInfoSection(entitlementState)
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = {
                        signingOut = true
                        onSignOut()
                    },
                    enabled = !signingOut,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color.Black.copy(alpha = 0.5f)
                    )
                ) {
                    if (signingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            stringResource(R.string.sair_da_conta),
                            fontSize = 13.sp,
                            color = Color.Black
                        )
                    }
                }
            } else {
                Text(
                    text = "Bem-vindo a Amadeus",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.login_google_opcional_desc),
                    color = Color.Black.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onSignIn,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = CfgColorDarkPurple
                    )
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.entrar_com_google),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CfgSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = CfgColorDarkPurple,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun CfgCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CfgColorSurface)
    ) { content() }
}

@Composable
private fun CfgNameField(
    label: String,
    value: String,
    isError: Boolean,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    onImeAction: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        isError = isError,
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            keyboardType = KeyboardType.Text,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() }
        ),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CfgColorDarkPurple,
            focusedLabelColor = CfgColorDarkPurple,
            cursorColor = CfgColorDarkPurple
        )
    )
}

@Composable
private fun CfgToggleRow(
    icon: ImageVector,
    label: String,
    descricao: String,
    checked: Boolean,
    enabled: Boolean = true,
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
                .background(CfgColorDarkPurple.copy(alpha = if (enabled) 0.10f else 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) CfgColorDarkPurple else Color(0xFFBDBDBD),
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
                checkedTrackColor = CfgColorPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFBDBDBD)
            )
        )
    }
}

@Composable
private fun CfgShortcutRow(
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
                .background(CfgColorDarkPurple.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CfgColorDarkPurple,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
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
    }
}

@Composable
private fun CfgDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 68.dp)
            .height(0.5.dp)
            .background(Color(0x1F000000))
    )
}
