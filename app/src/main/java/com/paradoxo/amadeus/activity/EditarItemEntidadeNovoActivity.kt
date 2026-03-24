package com.paradoxo.amadeus.activity

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.adapter.AdapterSinonimos
import com.paradoxo.amadeus.dao.EntidadeDAO
import com.paradoxo.amadeus.fragments.DialogSimples
import com.paradoxo.amadeus.modelo.Entidade
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca
import com.paradoxo.amadeus.util.Util.esconderTeclado

class EditarItemEntidadeNovoActivity : AppCompatActivity(), DialogSimples.FragmentDialogInterface {

    private var modificado = false

    companion object {
        var entidadeEmUso: Entidade? = null
        var adapter: AdapterSinonimos? = null
        var entradaEditText: TextInputEditText? = null

        private fun atualizarRecycler(sinonimos: List<String>) {
            adapter?.addAll(sinonimos)
        }

        private fun finalizarActivity(context: Activity) {
            context.startActivity(Intent(context, ListaEntidadeActivity::class.java))
            context.finish()
        }

        @Suppress("DEPRECATION")
        private fun carregarEntidade(context: Activity) {
            object : AsyncTask<Void?, Void?, Entidade?>() {
                override fun doInBackground(vararg voids: Void?): Entidade? {
                    val gson = Gson()
                    entidadeEmUso = gson.fromJson(context.intent.getStringExtra("entidade"), Entidade::class.java)
                    return entidadeEmUso
                }

                override fun onPostExecute(entidade: Entidade?) {
                    super.onPostExecute(entidade)
                    if (entidade == null) return
                    entradaEditText?.setText(entidade.nome)
                    atualizarRecycler(entidade.sinonimos)
                }
            }.execute()
        }

        @Suppress("DEPRECATION")
        fun gravarDados(entradaValida: String, context: Activity) {
            object : AsyncTask<Void?, Void?, Void?>() {
                override fun onPreExecute() {
                    super.onPreExecute()
                    meuToast(context.getString(R.string.salvando_dados), context)
                }

                override fun doInBackground(vararg voids: Void?): Void? {
                    val entidadeDAO = EntidadeDAO(context)
                    if (entidadeEmUso == null) {
                        val entidadeGravar = Entidade()
                        entidadeGravar.nome = entradaValida
                        entidadeGravar.sinonimos = adapter!!.itens
                        entidadeDAO.inserir(entidadeGravar)
                    } else {
                        entidadeEmUso!!.nome = entradaValida
                        entidadeEmUso!!.sinonimos = adapter!!.itens
                        entidadeDAO.alterarSentenca(entidadeEmUso)
                    }
                    return null
                }

                override fun onPostExecute(v: Void?) {
                    super.onPostExecute(v)
                    meuToast(context.getString(R.string.salvo), context)
                    finalizarActivity(context)
                }
            }.execute()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_item_novo)
        configurarInterface()
    }

    private fun configurarInterface() {
        configurarToolBarBranca(this)
        configurarTextInput()
        configuarBotaoLinkStart()
        configuarBotaoAddResposta()
        configurarRecycler()
    }

    private fun configurarRecycler() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = AdapterSinonimos(mutableListOf())
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL))

        adapter?.setOnItemClickListener { _, _, pos, tipoDeletar ->
            if (tipoDeletar) {
                abrirDialogExcluir(pos)
            } else {
                abrirDialogEditar(pos, true)
            }
        }
    }

    private fun inserirSinonimo(novoSinonimo: String) {
        adapter?.add(novoSinonimo)
        modificado = true
    }

    private fun alterarSinonimo(pos: Int, novoSinonimo: String) {
        adapter?.altera(pos, novoSinonimo)
        modificado = true
    }

    private fun deletarSinonimo(pos: Int) {
        adapter?.remove(pos)
        modificado = true
    }

    fun abrirDialogEditar(posi: Int?, editando: Boolean) {
        val alertDialogBuilder = Dialog(this)
        alertDialogBuilder.setContentView(
            layoutInflater.inflate(R.layout.dialog_editar_sinonimo, findViewById(R.id.editarItemEntidadeLayout), false)
        )

        val sinonimoEditText = alertDialogBuilder.findViewById<TextInputEditText>(R.id.sinonimoEditText)
        if (editando && posi != null) {
            sinonimoEditText.setText(adapter?.itens?.get(posi))
            sinonimoEditText.setSelection(adapter?.itens?.get(posi)?.length ?: 0)
        }

        val sinonimoTextInput = alertDialogBuilder.findViewById<TextInputLayout>(R.id.sinonimoTextInput)

        val botaoNegar = alertDialogBuilder.findViewById<TextView>(R.id.botaoCancelar)
        botaoNegar.setOnClickListener { view ->
            esconderTeclado(view, this@EditarItemEntidadeNovoActivity)
            alertDialogBuilder.dismiss()
        }

        val botaoSalvar = alertDialogBuilder.findViewById<MaterialButton>(R.id.botaoSalvar)
        botaoSalvar.setOnClickListener { view ->
            if (sinonimoEditText.text.toString().isEmpty()) {
                sinonimoTextInput.error = getString(R.string.entrada_invalida)
            } else {
                if (editando && posi != null) {
                    alterarSinonimo(posi, sinonimoEditText.text.toString())
                } else {
                    inserirSinonimo(sinonimoEditText.text.toString())
                }
                esconderTeclado(view, this@EditarItemEntidadeNovoActivity)
                sinonimoTextInput.isErrorEnabled = false
                alertDialogBuilder.dismiss()
            }
        }

        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.show()
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
            -1 -> deletarSinonimo(posi)
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

    private fun configuarBotaoAddResposta() {
        findViewById<View>(R.id.adicionarSinonimoButton).setOnClickListener { adicionarMaisUmSinonimo() }
    }

    private fun adicionarMaisUmSinonimo() {
        abrirDialogEditar(null, false)
    }

    fun validarInputs() {
        val entradaTextInput = findViewById<TextInputLayout>(R.id.entradaTextInput)
        if (entradaEditText?.text.toString().isEmpty()) {
            entradaTextInput.error = getString(R.string.entrada_invalida)
        } else {
            entradaTextInput.isErrorEnabled = false
            gravarDados(entradaEditText?.text.toString().lowercase().trim() ?: "", this)
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
