package com.paradoxo.amadeus.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.adapter.AdapterAcoes
import com.paradoxo.amadeus.cognicao.AgentMemoryManager
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.entities.AgentMacroEntity
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.ActionType
import com.paradoxo.amadeus.service.AmadeusAccessibilityService
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.Util.aplicarInsets
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditarMacroActivity : AppCompatActivity() {

    private lateinit var adapterAcoes: AdapterAcoes
    private var macro: AgentMacroEntity? = null
    private val memoryManager by lazy { AgentMemoryManager(this) }
    private val modoEdicao get() = macro != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_macro)
        aplicarInsets(findViewById(R.id.editarMacroLayout))
        configurarToolBarBranca(this)

        val macroId = intent.getIntExtra("macroId", -1)
        val recordedJson = intent.getStringExtra(EXTRA_RECORDED_ACTIONS_JSON)

        if (macroId == -1) {
            configurarInterface(null)
            if (!recordedJson.isNullOrBlank()) {
                val recorded = memoryManager.deserializeActions(recordedJson)
                if (recorded.isNotEmpty()) {
                    adapterAcoes.setActions(recorded)
                    meuToast("${recorded.size} ações gravadas. Revise e salve.", this)
                }
            }
        } else {
            lifecycleScope.launch {
                val encontrado = withContext(Dispatchers.IO) {
                    AmadeusDatabase.getInstance(this@EditarMacroActivity).agentMacroDAO()
                        .findById(macroId)
                }
                if (encontrado == null) {
                    finish(); return@launch
                }
                macro = encontrado
                configurarInterface(encontrado)
            }
        }
    }

    companion object {
        const val EXTRA_RECORDED_ACTIONS_JSON = "extra_recorded_actions_json"
    }

    private fun configurarInterface(macroCarregado: AgentMacroEntity?) {
        val nomeLayout = findViewById<TextInputLayout>(R.id.nomeLayout)
        val nomeEditText = findViewById<TextInputEditText>(R.id.nomeEditText)

        if (macroCarregado != null) {
            nomeEditText.setText(macroCarregado.goalKey)
            nomeLayout.hint = "Nome do macro"
        }

        val acoes =
            macroCarregado?.let { memoryManager.deserializeActions(it.actionsJson).toMutableList() }
                ?: mutableListOf()
        adapterAcoes = AdapterAcoes(acoes) { onPicked ->
            lifecycleScope.launch {
                val entidades = withContext(Dispatchers.IO) {
                    AmadeusDatabase.getInstance(this@EditarMacroActivity).entidadeDAO().listar()
                }
                if (entidades.isEmpty()) {
                    meuToast("Nenhuma entidade cadastrada no banco.", this@EditarMacroActivity)
                    return@launch
                }
                val nomes = entidades.mapNotNull { it.nome }.toTypedArray()
                AlertDialog.Builder(this@EditarMacroActivity)
                    .setTitle("Selecionar referência")
                    .setItems(nomes) { _, i -> onPicked(nomes[i]) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewAcoes)
        recyclerView.adapter = adapterAcoes

        findViewById<View>(R.id.adicionarAcaoButton).setOnClickListener {
            adapterAcoes.addAcao(ActionIntent(ActionType.CLICK))
            recyclerView.scrollToPosition(adapterAcoes.itemCount - 1)
        }

        findViewById<MaterialButton>(R.id.testarButton).setOnClickListener {
            testarAcoes()
        }

        findViewById<MaterialButton>(R.id.salvarButton).setOnClickListener {
            salvarMacro()
        }

        val excluirButton = findViewById<MaterialButton>(R.id.excluirButton)
        if (macroCarregado != null) {
            excluirButton.visibility = View.VISIBLE
            excluirButton.setOnClickListener { confirmarExclusao() }
        }
    }

    private fun confirmarExclusao() {
        AlertDialog.Builder(this)
            .setTitle("Excluir macro")
            .setMessage("Deseja excluir o macro \"${macro?.goalKey}\"? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
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
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun testarAcoes() {
        val acoes = adapterAcoes.getAcoes()
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
                    onFeedback = { /* feedback visual ignorado fora do overlay */ },
                    onDone = { meuToast("Teste concluído!", applicationContext) }
                )
            }, 500)
        }
    }

    private fun salvarMacro() {
        val novasAcoes = adapterAcoes.getAcoes()
        if (novasAcoes.isEmpty()) {
            meuToast("Adicione pelo menos uma ação antes de salvar.", this)
            return
        }

        val nomeEditText = findViewById<TextInputEditText>(R.id.nomeEditText)
        val nome = nomeEditText.text?.toString()?.trim() ?: ""
        if (nome.isBlank()) {
            meuToast("Informe um nome para o macro.", this)
            nomeEditText.requestFocus()
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
