package com.paradoxo.amadeus.activity

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.adapter.AdapterSinonimos
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.toEntity
import com.paradoxo.amadeus.fragments.DialogSimples
import com.paradoxo.amadeus.modelo.Entidade
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.Util.aplicarInsets
import com.paradoxo.amadeus.util.Util.esconderTeclado
import com.paradoxo.amadeus.util.setupToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditarItemEntidadeNovoActivity : AppCompatActivity(), DialogSimples.FragmentDialogInterface {

    private var modificado = false
    private var deletandoSignificado = false

    companion object {
        var entidadeEmUso: Entidade? = null
        var adapter: AdapterSinonimos? = null
        var adapterSignificados: AdapterSinonimos? = null
        var entradaEditText: TextInputEditText? = null

        private fun finalizarActivity(context: Activity) {
            context.startActivity(Intent(context, ListaEntidadeActivity::class.java))
            context.finish()
        }

        private fun carregarEntidade(context: AppCompatActivity) {
            context.lifecycleScope.launch(Dispatchers.IO) {
                val gson = Gson()
                val entidade = gson.fromJson(context.intent.getStringExtra("entidade"), Entidade::class.java)
                entidadeEmUso = entidade
                withContext(Dispatchers.Main) {
                    if (entidade == null) return@withContext
                    entradaEditText?.setText(entidade.nome)
                    adapter?.addAll(entidade.sinonimos ?: emptyList())
                    adapterSignificados?.addAll(entidade.significado ?: emptyList())
                }
            }
        }

        fun gravarDados(entradaValida: String, context: AppCompatActivity) {
            context.lifecycleScope.launch {
                meuToast(context.getString(R.string.salvando_dados), context)
                withContext(Dispatchers.IO) {
                    val dao = AmadeusDatabase.getInstance(context).entidadeDAO()
                    if (entidadeEmUso == null) {
                        val entidadeGravar = Entidade().apply {
                            nome = entradaValida
                            sinonimos = adapter!!.itens
                            significado = adapterSignificados!!.itens
                        }
                        dao.inserir(entidadeGravar.toEntity())
                    } else {
                        entidadeEmUso!!.nome = entradaValida
                        entidadeEmUso!!.sinonimos = adapter!!.itens
                        entidadeEmUso!!.significado = adapterSignificados!!.itens
                        dao.alterar(entidadeEmUso!!.toEntity())
                    }
                }
                meuToast(context.getString(R.string.salvo), context)
                finalizarActivity(context)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_item_novo)
        aplicarInsets(findViewById(R.id.editarItemEntidadeRootLayout))
        configurarInterface()
    }

    private fun configurarInterface() {
        setupToolbar(getString(R.string.entidade))
        configurarTextInput()
        configuarBotaoLinkStart()
        configurarRecyclerSinonimos()
        configurarRecyclerSignificados()
    }

    private fun configurarRecyclerSinonimos() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = AdapterSinonimos(mutableListOf())
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL))

        adapter?.setOnItemClickListener { _, _, pos, tipoDeletar ->
            if (tipoDeletar) {
                deletandoSignificado = false
                abrirDialogExcluir(pos)
            } else {
                mostrarDialogItem(adapter!!, pos, editando = true)
            }
        }

        findViewById<View>(R.id.adicionarSinonimoButton).setOnClickListener {
            mostrarDialogItem(adapter!!, null, editando = false)
        }
    }

    private fun configurarRecyclerSignificados() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewSignificados)
        adapterSignificados = AdapterSinonimos(mutableListOf())
        recyclerView.adapter = adapterSignificados
        recyclerView.addItemDecoration(DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL))

        adapterSignificados?.setOnItemClickListener { _, _, pos, tipoDeletar ->
            if (tipoDeletar) {
                deletandoSignificado = true
                abrirDialogExcluir(pos)
            } else {
                mostrarDialogItem(adapterSignificados!!, pos, editando = true)
            }
        }

        findViewById<View>(R.id.adicionarSignificadoButton).setOnClickListener {
            mostrarDialogItem(adapterSignificados!!, null, editando = false)
        }
    }

    private fun mostrarDialogItem(adap: AdapterSinonimos, posi: Int?, editando: Boolean) {
        val dialog = Dialog(this)
        dialog.setContentView(
            layoutInflater.inflate(R.layout.dialog_editar_sinonimo, findViewById(R.id.editarItemEntidadeLayout), false)
        )

        val sinonimoEditText = dialog.findViewById<TextInputEditText>(R.id.sinonimoEditText)
        if (editando && posi != null) {
            sinonimoEditText.setText(adap.itens[posi])
            sinonimoEditText.setSelection(adap.itens[posi].length)
        }

        val sinonimoTextInput = dialog.findViewById<TextInputLayout>(R.id.sinonimoTextInput)

        dialog.findViewById<TextView>(R.id.botaoCancelar).setOnClickListener { view ->
            esconderTeclado(view, this)
            dialog.dismiss()
        }

        dialog.findViewById<MaterialButton>(R.id.botaoSalvar).setOnClickListener { view ->
            val texto = sinonimoEditText.text.toString()
            if (texto.isEmpty()) {
                sinonimoTextInput.error = getString(R.string.entrada_invalida)
            } else {
                if (editando && posi != null) {
                    adap.altera(posi, texto)
                } else {
                    adap.add(texto)
                }
                modificado = true
                esconderTeclado(view, this)
                sinonimoTextInput.isErrorEnabled = false
                dialog.dismiss()
            }
        }

        dialog.setCancelable(false)
        dialog.show()
    }

    fun abrirDialogExcluir(posi: Int) {
        val dialog = DialogSimples.newDialog(
            getString(R.string.confirmar_exclusao),
            getString(R.string.tem_certeza_que_deseja_exluir_esse_item),
            posi,
            intArrayOf(android.R.string.ok, android.R.string.cancel)
        )
        dialog.openDialog(supportFragmentManager)
    }

    override fun onClick(posi: Int, which: Int) {
        when (which) {
            -1 -> {
                if (deletandoSignificado) {
                    adapterSignificados?.remove(posi)
                } else {
                    adapter?.remove(posi)
                }
                modificado = true
            }
            -2 -> {}
        }
    }

    private fun configurarTextInput() {
        (findViewById<TextView>(R.id.tituloTexView)).setText(R.string.entidade)
        (findViewById<TextView>(R.id.titulo2TexView)).setText(R.string.sinonimos)

        entradaEditText = findViewById(R.id.entradaEditText)
        entradaEditText?.setOnFocusChangeListener { _, _ -> modificado = true }

        carregarEntidade(this)
    }

    private fun configuarBotaoLinkStart() {
        findViewById<View>(R.id.okButton).setOnClickListener { validarInputs() }
    }

    fun validarInputs() {
        val entradaTextInput = findViewById<TextInputLayout>(R.id.entradaTextInput)
        if (entradaEditText?.text.toString().isEmpty()) {
            entradaTextInput.error = getString(R.string.entrada_invalida)
        } else {
            entradaTextInput.isErrorEnabled = false
            gravarDados(entradaEditText?.text.toString().lowercase().trim(), this)
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (modificado) {
            val snackbar = Snackbar.make(
                findViewById(R.id.editarItemEntidadeLayout),
                getString(R.string.alteracao_ainda_nao_salva),
                Snackbar.LENGTH_LONG
            )
            snackbar.setActionTextColor(Color.YELLOW)
            snackbar.setAction(getString(R.string.sim_sair)) {
                startActivity(Intent(applicationContext, ListaEntidadeActivity::class.java))
                finish()
            }
            snackbar.show()
            modificado = false
        } else {
            finalizarActivity(this)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
