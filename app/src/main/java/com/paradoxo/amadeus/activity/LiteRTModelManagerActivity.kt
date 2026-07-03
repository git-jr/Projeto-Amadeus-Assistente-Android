package com.paradoxo.amadeus.activity

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.cognicao.localmodel.LocalModelDownloadManager
import com.paradoxo.amadeus.cognicao.localmodel.SupportedModels
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.entities.LocalModelEntity
import com.paradoxo.amadeus.dao.room.entities.ModelStatus
import com.paradoxo.amadeus.util.Preferencias
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val ColorPrimary = Color(0xFFF50057)
private val ColorDarkPurple = Color(0xFF290f3e)
private val ColorSuccess = Color(0xFF4CAF50)
private val ColorSurface = Color(0xFFF5F5F5)

class LiteRTModelManagerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MaterialTheme {
                ModelManagerScreen(onBack = { finish() })
            }
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}

class LiteRTModelManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AmadeusDatabase.getInstance(application)
    private val dao = db.localModelDAO()
    private val downloadManager = LocalModelDownloadManager(application)

    val models: StateFlow<List<LocalModelEntity>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        startProgressPolling()
    }

    private fun startProgressPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val downloading = models.value.filter { it.status == ModelStatus.DOWNLOADING }
                for (model in downloading) {
                    if (model.downloadManagerId >= 0) {
                        downloadManager.checkProgress(model.modelId, model.downloadManagerId)
                    }
                }
                delay(2_000)
            }
        }
    }

    fun downloadModel(descriptor: SupportedModels.ModelDescriptor) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadManager.enqueueDownload(descriptor)
        }
    }

    fun cancelDownload(model: LocalModelEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadManager.cancelDownload(model.modelId, model.downloadManagerId)
        }
    }

    fun setActive(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setActive(modelId)
        }
    }

    fun deleteModel(entity: LocalModelEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadManager.deleteModel(entity)
        }
    }

    fun saveHfToken(token: String) {
        Preferencias.setPrefString(token, Preferencias.PREF_HF_TOKEN, getApplication())
    }

    fun getHfToken(): String =
        Preferencias.getPrefString(Preferencias.PREF_HF_TOKEN, getApplication())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelManagerScreen(onBack: () -> Unit) {
    val vm: LiteRTModelManagerViewModel = viewModel()
    val models by vm.models.collectAsState()
    val activeModel = models.firstOrNull { it.isActive }
    val totalUsedBytes = models.filter { it.status == ModelStatus.DOWNLOADED }
        .sumOf { it.sizeBytes }
    var hfToken by remember { mutableStateOf(vm.getHfToken()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Modelos Locais (Beta)",
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
            ActiveModelCard(activeModel)

            StorageCard(totalUsedBytes)

            val hasGatedModel = SupportedModels.CATALOG.any { it.requiresHfToken }
            if (hasGatedModel) {
                HfTokenCard(
                    token = hfToken,
                    onTokenChange = {
                        hfToken = it
                        vm.saveHfToken(it)
                    }
                )
            }

            SectionHeader("Modelos Disponíveis")

            models.forEach { model ->
                val descriptor = SupportedModels.CATALOG.firstOrNull { it.modelId == model.modelId }
                ModelCard(
                    model = model,
                    descriptor = descriptor,
                    hfToken = hfToken,
                    onDownload = { descriptor?.let { vm.downloadModel(it) } },
                    onCancel = { vm.cancelDownload(model) },
                    onSetActive = { vm.setActive(model.modelId) },
                    onDelete = { vm.deleteModel(model) }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActiveModelCard(activeModel: LocalModelEntity?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorDarkPurple)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.SmartToy,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "Modelo Ativo",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
                Text(
                    activeModel?.displayName ?: "Nenhum modelo ativo",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun StorageCard(totalBytes: Long) {
    val label = formatBytes(totalBytes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Armazenamento utilizado:",
            fontSize = 14.sp,
            color = Color(0xFF757575),
            modifier = Modifier.weight(1f)
        )
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF212121)
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
private fun ModelCard(
    model: LocalModelEntity,
    descriptor: SupportedModels.ModelDescriptor?,
    hfToken: String,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onSetActive: () -> Unit,
    onDelete: () -> Unit
) {
    val needsToken = descriptor?.requiresHfToken == true
    val canDownload = !needsToken || hfToken.isNotBlank()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    model.displayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color(0xFF212121)
                )
                Text(
                    model.description,
                    fontSize = 12.sp,
                    color = Color(0xFF757575),
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatBytes(model.sizeBytes),
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
            if (model.status == ModelStatus.DOWNLOADED) {
                RadioButton(
                    selected = model.isActive,
                    onClick = onSetActive,
                    colors = RadioButtonDefaults.colors(selectedColor = ColorPrimary)
                )
            }
        }

        // Progresso durante download
        if (model.status == ModelStatus.DOWNLOADING) {
            val progress = if (model.sizeBytes > 0) {
                model.downloadedBytes.toFloat() / model.sizeBytes.toFloat()
            } else 0f
            val pct = (progress * 100).toInt()
            Text(
                "Baixando… $pct% (${formatBytes(model.downloadedBytes)} / ${formatBytes(model.sizeBytes)})",
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = ColorPrimary,
                trackColor = Color(0xFFE0E0E0)
            )
        }

        // Status chip + ações
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(model.status)
            Spacer(Modifier.weight(1f))
            when (model.status) {
                ModelStatus.NOT_DOWNLOADED, ModelStatus.ERROR -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Button(
                            onClick = onDownload,
                            enabled = canDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                if (needsToken && !canDownload) Icons.Rounded.Lock else Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Baixar", fontSize = 13.sp)
                        }
                        if (needsToken && !canDownload) {
                            Text(
                                "Requer token HuggingFace",
                                fontSize = 11.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
                ModelStatus.DOWNLOADING -> {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancelar", fontSize = 13.sp, color = Color(0xFF757575))
                    }
                }
                ModelStatus.DOWNLOADED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!model.isActive) {
                            Button(
                                onClick = onSetActive,
                                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Ativar", fontSize = 13.sp)
                            }
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Excluir",
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir modelo?", fontWeight = FontWeight.SemiBold) },
            text = { Text("O arquivo de ${formatBytes(model.sizeBytes)} será removido do dispositivo.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Excluir", color = ColorPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun StatusChip(status: ModelStatus) {
    val (label, bg, fg) = when (status) {
        ModelStatus.NOT_DOWNLOADED -> Triple("Disponível", Color(0xFFE3F2FD), Color(0xFF1565C0))
        ModelStatus.DOWNLOADING    -> Triple("Baixando",   Color(0xFFFFF3E0), Color(0xFFE65100))
        ModelStatus.DOWNLOADED     -> Triple("Baixado",    ColorSuccess.copy(alpha = 0.12f), ColorSuccess)
        ModelStatus.ERROR          -> Triple("Erro",       Color(0xFFFFEBEE), Color(0xFFC62828))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 12.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HfTokenCard(token: String, onTokenChange: (String) -> Unit) {
    var showToken by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ColorDarkPurple.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Key, contentDescription = null, tint = ColorDarkPurple, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Token HuggingFace", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF212121))
                Text(
                    "Necessário para baixar modelos Gemma (restritos).",
                    fontSize = 12.sp, color = Color(0xFF757575), lineHeight = 16.sp
                )
            }
        }
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("hf_...", fontSize = 13.sp, color = Color(0xFFBDBDBD)) },
            singleLine = true,
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(
                        if (showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorPrimary,
                unfocusedBorderColor = Color(0xFFE0E0E0)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 11.sp, color = Color(0xFF9E9E9E))) {
                    append("Gere em: ")
                }
                withLink(
                    LinkAnnotation.Url(
                        url = "https://huggingface.co/settings/tokens",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                fontSize = 11.sp,
                                color = ColorPrimary,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                ) {
                    append("huggingface.co/settings/tokens")
                }
            }
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000L     -> "%.0f MB".format(bytes / 1_000_000.0)
    else                    -> "%.0f KB".format(bytes / 1_000.0)
}
