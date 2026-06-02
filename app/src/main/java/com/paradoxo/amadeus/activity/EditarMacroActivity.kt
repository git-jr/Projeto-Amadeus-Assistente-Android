package com.paradoxo.amadeus.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.cognicao.AgentMemoryManager
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.entities.AgentMacroEntity
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.ActionType
import com.paradoxo.amadeus.service.AmadeusAccessibilityService
import com.paradoxo.amadeus.util.Toasts.meuToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditarMacroActivity : AppCompatActivity() {

    private var macro: AgentMacroEntity? = null
    private val memoryManager by lazy { AgentMemoryManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val macroId = intent.getIntExtra("macroId", -1)
        val recordedJson = intent.getStringExtra(EXTRA_RECORDED_ACTIONS_JSON)

        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                val actions = remember { mutableStateListOf<ActionIntent>() }
                var nome by rememberSaveable { mutableStateOf("") }
                var loading by rememberSaveable { mutableStateOf(true) }
                var title by rememberSaveable { mutableStateOf("Nova macro") }
                var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
                var showReferenceDialog by rememberSaveable { mutableStateOf(false) }
                var referenceOptions by remember { mutableStateOf<List<String>>(emptyList()) }
                var pendingReferenceActionIndex by remember { mutableStateOf<Int?>(null) }
                var nameError by rememberSaveable { mutableStateOf(false) }
                val nameFocusRequester = remember { FocusRequester() }

                LaunchedEffect(macroId, recordedJson) {
                    if (macroId == -1) {
                        if (!recordedJson.isNullOrBlank()) {
                            val recorded = memoryManager.deserializeActions(recordedJson)
                            if (recorded.isNotEmpty()) {
                                actions.clear()
                                actions.addAll(recorded)
                                meuToast("${recorded.size} ações gravadas. Revise e salve.", this@EditarMacroActivity)
                            }
                        }
                        loading = false
                    } else {
                        val encontrado = withContext(Dispatchers.IO) {
                            AmadeusDatabase.getInstance(this@EditarMacroActivity).agentMacroDAO()
                                .findById(macroId)
                        }
                        if (encontrado == null) {
                            finish()
                            return@LaunchedEffect
                        }
                        macro = encontrado
                        title = "Editar macro"
                        nome = encontrado.goalKey
                        actions.clear()
                        actions.addAll(memoryManager.deserializeActions(encontrado.actionsJson))
                        loading = false
                    }
                }

                if (showDeleteDialog) {
                    ConfirmDeleteDialog(
                        macroName = macro?.goalKey.orEmpty(),
                        onDismiss = { showDeleteDialog = false },
                        onConfirm = {
                            showDeleteDialog = false
                            confirmarExclusao()
                        }
                    )
                }

                if (showReferenceDialog) {
                    ReferenceDialog(
                        options = referenceOptions,
                        onDismiss = {
                            showReferenceDialog = false
                            pendingReferenceActionIndex = null
                        },
                        onPick = { entityName ->
                            val index = pendingReferenceActionIndex
                            if (index != null && index in actions.indices) {
                                actions[index] = insertReference(actions[index], entityName)
                            }
                            showReferenceDialog = false
                            pendingReferenceActionIndex = null
                        }
                    )
                }

                EditarMacroScreen(
                    title = title,
                    loading = loading,
                    nome = nome,
                    nameError = nameError,
                    nameFocusRequester = nameFocusRequester,
                    actions = actions,
                    isEditMode = macro != null,
                    onBack = { finish() },
                    onNameChange = {
                        nome = it
                        if (it.isNotBlank()) nameError = false
                    },
                    onActionChange = { index, action ->
                        if (index in actions.indices) actions[index] = action
                    },
                    onAddAction = {
                        actions.add(ActionIntent(ActionType.CLICK))
                    },
                    onMoveUp = { index ->
                        if (index > 0 && index in actions.indices) {
                            val moved = actions.removeAt(index)
                            actions.add(index - 1, moved)
                        }
                    },
                    onMoveDown = { index ->
                        if (index >= 0 && index < actions.lastIndex) {
                            val moved = actions.removeAt(index)
                            actions.add(index + 1, moved)
                        }
                    },
                    onDeleteAction = { index ->
                        if (index in actions.indices) actions.removeAt(index)
                    },
                    onPickReference = { index ->
                        scope.launch {
                            val nomes = withContext(Dispatchers.IO) {
                                AmadeusDatabase.getInstance(this@EditarMacroActivity)
                                    .entidadeDAO()
                                    .listar()
                                    .mapNotNull { it.nome }
                            }
                            if (nomes.isEmpty()) {
                                meuToast("Nenhuma entidade cadastrada no banco.", this@EditarMacroActivity)
                            } else {
                                pendingReferenceActionIndex = index
                                referenceOptions = nomes
                                showReferenceDialog = true
                            }
                        }
                    },
                    onTest = { testarAcoes(actions.toList()) },
                    onSave = {
                        val trimmedName = nome.trim()
                        if (trimmedName.isBlank()) {
                            nameError = true
                            nameFocusRequester.requestFocus()
                        } else {
                            salvarMacro(trimmedName, actions.toList())
                        }
                    },
                    onDeleteMacro = { showDeleteDialog = true }
                )
            }
        }
    }

    companion object {
        const val EXTRA_RECORDED_ACTIONS_JSON = "extra_recorded_actions_json"
    }

    private fun confirmarExclusao() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                macro?.let {
                    AmadeusDatabase.getInstance(this@EditarMacroActivity).agentMacroDAO()
                        .delete(it)
                }
            }
            meuToast("Macro excluído.", this@EditarMacroActivity)
            finish()
        }
    }

    private fun testarAcoes(acoes: List<ActionIntent>) {
        if (acoes.isEmpty()) {
            meuToast("Adicione pelo menos uma ação antes de testar.", this)
            return
        }
        if (!AmadeusAccessibilityService.isActive(this)) {
            meuToast("Serviço de acessibilidade inativo. Ative nas configurações do Android.", this)
            return
        }
        moveTaskToBack(true)
        lifecycleScope.launch {
            val acoesResolvidas = withContext(Dispatchers.IO) {
                memoryManager.resolveActionsReferences(acoes)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                AmadeusAccessibilityService.executeActions(
                    acoesResolvidas,
                    onFeedback = { },
                    onDone = { meuToast("Teste concluído!", applicationContext) }
                )
            }, 500)
        }
    }

    private fun salvarMacro(nome: String, novasAcoes: List<ActionIntent>) {
        if (novasAcoes.isEmpty()) {
            meuToast("Adicione pelo menos uma ação antes de salvar.", this)
            return
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val novoJson = memoryManager.serializeActions(novasAcoes)
                val db = AmadeusDatabase.getInstance(this@EditarMacroActivity)
                val macroAtual = macro
                if (macroAtual != null) {
                    db.agentMacroDAO()
                        .update(macroAtual.copy(goalKey = nome, actionsJson = novoJson))
                } else {
                    db.agentMacroDAO().insert(
                        AgentMacroEntity(
                            goalKey = nome,
                            actionsJson = novoJson,
                            isConfirmed = true
                        )
                    )
                }
            }
            meuToast("Macro salvo!", this@EditarMacroActivity)
            finish()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarMacroScreen(
    title: String,
    loading: Boolean,
    nome: String,
    nameError: Boolean,
    nameFocusRequester: FocusRequester,
    actions: List<ActionIntent>,
    isEditMode: Boolean,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onActionChange: (Int, ActionIntent) -> Unit,
    onAddAction: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDeleteAction: (Int) -> Unit,
    onPickReference: (Int) -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onDeleteMacro: () -> Unit,
) {
    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = topBarScrollBehavior
            )
        },
        bottomBar = {
            if (!loading) {
                MacroBottomBar(
                    isEditMode = isEditMode,
                    onAddAction = onAddAction,
                    onTest = onTest,
                    onSave = onSave,
                    onDeleteMacro = onDeleteMacro
                )
            }
        },
        containerColor = Color(0xFFF7F7F9)
    ) { padding ->
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AmadeusPink)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MacroNameCard(
                    nome = nome,
                    nameError = nameError,
                    focusRequester = nameFocusRequester,
                    onNameChange = onNameChange
                )
            }

            if (actions.isEmpty()) {
                item {
                    EmptyActionsCard()
                }
            }

            itemsIndexed(actions) { index, action ->
                MacroActionCard(
                    index = index,
                    action = action,
                    isFirst = index == 0,
                    isLast = index == actions.lastIndex,
                    onActionChange = { onActionChange(index, it) },
                    onMoveUp = { onMoveUp(index) },
                    onMoveDown = { onMoveDown(index) },
                    onDelete = { onDeleteAction(index) },
                    onPickReference = { onPickReference(index) }
                )
            }
        }
    }
}

@Composable
private fun MacroNameCard(
    nome: String,
    nameError: Boolean,
    focusRequester: FocusRequester,
    onNameChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Identificação",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = nome,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                label = { Text("Nome do macro") },
                singleLine = true,
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Informe um nome para o macro.") }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun EmptyActionsCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E2E6), RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Rounded.SwapVert,
                contentDescription = null,
                tint = AmadeusPink,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Nenhuma ação adicionada",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Use o botão abaixo para montar a sequência do macro.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MacroActionCard(
    index: Int,
    action: ActionIntent,
    isFirst: Boolean,
    isLast: Boolean,
    onActionChange: (ActionIntent) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onPickReference: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(AmadeusPink.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = AmadeusPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "Ação ${index + 1}",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Mover para cima")
                }
                IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Mover para baixo")
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Remover ação",
                        tint = Color(0xFFD32F2F)
                    )
                }
            }

            ActionTypeDropdown(
                selected = action.type,
                onSelected = { selected ->
                    onActionChange(normalizeActionForType(action.copy(type = selected)))
                }
            )

            val fields = visibleFields(action.type)
            if (fields.showTarget) {
                OutlinedTextField(
                    value = action.target,
                    onValueChange = { onActionChange(action.copy(target = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Elemento / App") },
                    singleLine = true
                )
            }

            if (fields.showValue) {
                OutlinedTextField(
                    value = action.value,
                    onValueChange = { onActionChange(action.copy(value = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(valueLabel(action.type)) },
                    singleLine = true
                )
            }

            if (action.type == ActionType.TYPE) {
                TextButton(
                    onClick = onPickReference,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "@ INSERIR REFERÊNCIA DO BANCO",
                        color = AmadeusPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionTypeDropdown(
    selected: ActionType,
    onSelected: (ActionType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFBDBDC4), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected.actionLabel(),
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = "Selecionar tipo de ação",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 340.dp)
        ) {
            ActionType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.actionLabel()) },
                    onClick = {
                        expanded = false
                        onSelected(type)
                    }
                )
            }
        }
    }
}

@Composable
private fun MacroBottomBar(
    isEditMode: Boolean,
    onAddAction: () -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onDeleteMacro: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddAction,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmadeusPink)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Adicionar", fontWeight = FontWeight.Bold, letterSpacing = 0.sp, fontSize = 12.sp)
                }

                Button(
                    onClick = onTest,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmadeusPink)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Testar", fontWeight = FontWeight.Bold, letterSpacing = 0.sp, fontSize = 12.sp)
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmadeusPink)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Salvar", fontWeight = FontWeight.Bold, letterSpacing = 0.sp, fontSize = 12.sp)
                }
            }

            if (isEditMode) {
                OutlinedButton(
                    onClick = onDeleteMacro,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Excluir macro", fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    macroName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir macro") },
        text = { Text("Deseja excluir o macro \"$macroName\"? Esta ação não pode ser desfeita.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Excluir", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ReferenceDialog(
    options: List<String>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar referência") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(options) { index, option ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPick(option) }
                            .padding(horizontal = 4.dp, vertical = 10.dp)
                    ) {
                        Text(option, fontSize = 15.sp)
                    }
                    if (index < options.lastIndex) {
                        HorizontalDivider(color = Color(0xFFE8E8EC))
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private data class VisibleFields(
    val showTarget: Boolean,
    val showValue: Boolean,
)

private val AmadeusPink = Color(0xFFF50057)

private fun visibleFields(type: ActionType): VisibleFields = when (type) {
    ActionType.CLICK -> VisibleFields(showTarget = true, showValue = false)
    ActionType.OPEN_APP -> VisibleFields(showTarget = true, showValue = false)
    ActionType.CLICK_COORDS -> VisibleFields(showTarget = false, showValue = true)
    ActionType.TYPE -> VisibleFields(showTarget = false, showValue = true)
    ActionType.SCROLL -> VisibleFields(showTarget = false, showValue = true)
    ActionType.SCROLL_HORIZONTAL -> VisibleFields(showTarget = false, showValue = true)
    ActionType.WAIT -> VisibleFields(showTarget = false, showValue = true)
    ActionType.IME_ACTION,
    ActionType.BACK,
    ActionType.HOME,
    ActionType.RECENTS,
    ActionType.NOTIFICATIONS,
    ActionType.QUICK_SETTINGS -> VisibleFields(showTarget = false, showValue = false)
}

private fun normalizeActionForType(action: ActionIntent): ActionIntent {
    val fields = visibleFields(action.type)
    return action.copy(
        target = if (fields.showTarget) action.target else "",
        value = if (fields.showValue) action.value else ""
    )
}

private fun valueLabel(type: ActionType): String = when (type) {
    ActionType.WAIT -> "Tempo em ms (ex: 2000)"
    ActionType.SCROLL,
    ActionType.SCROLL_HORIZONTAL -> "Direção (up/down ou left/right)"
    else -> "Valor"
}

private fun insertReference(action: ActionIntent, entityName: String): ActionIntent {
    val token = "{{$entityName}}"
    return action.copy(value = action.value + token)
}

private fun ActionType.actionLabel(): String = when (this) {
    ActionType.CLICK -> "Clicar em elemento"
    ActionType.CLICK_COORDS -> "Clicar em coordenadas"
    ActionType.TYPE -> "Digitar texto"
    ActionType.OPEN_APP -> "Abrir app"
    ActionType.SCROLL -> "Rolar verticalmente"
    ActionType.SCROLL_HORIZONTAL -> "Rolar horizontalmente"
    ActionType.IME_ACTION -> "Ação do teclado"
    ActionType.BACK -> "Voltar"
    ActionType.HOME -> "Início"
    ActionType.RECENTS -> "Recentes"
    ActionType.NOTIFICATIONS -> "Notificações"
    ActionType.QUICK_SETTINGS -> "Configurações rápidas"
    ActionType.WAIT -> "Aguardar"
}
