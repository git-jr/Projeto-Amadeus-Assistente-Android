package com.paradoxo.amadeus.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.adapter.AdapterMacros
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.entities.AgentMacroEntity
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListaMacrosActivity : AppCompatActivity() {

    private lateinit var adapterMacros: AdapterMacros
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_macros)
        configurarToolBarBranca(this)
        configurarInterface()
    }

    private fun configurarInterface() {
        recyclerView = findViewById(R.id.recyclerViewMacros)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)

        adapterMacros = AdapterMacros(mutableListOf())
        recyclerView.adapter = adapterMacros

        adapterMacros.setOnItemClickListener { macro ->
            val intent = Intent(this, EditarMacroActivity::class.java)
            intent.putExtra("macroId", macro.id)
            startActivity(intent)
        }

        findViewById<FloatingActionButton>(R.id.fabNovoMacro).setOnClickListener {
            startActivity(Intent(this, EditarMacroActivity::class.java))
        }

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                confirmarExclusao(pos)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)

        carregarMacros()
    }

    private fun confirmarExclusao(pos: Int) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirmar_exclusao))
            .setMessage("Deseja remover este macro?")
            .setPositiveButton(R.string.ok) { _, _ ->
                val macro = adapterMacros.getItem(pos)
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        AmadeusDatabase.getInstance(this@ListaMacrosActivity).agentMacroDAO().delete(macro)
                    }
                    adapterMacros.remove(pos)
                    atualizarEmptyState()
                    meuToast(getString(R.string.excluido_com_sucesso), this@ListaMacrosActivity)
                }
            }
            .setNegativeButton(R.string.cancelar) { _, _ ->
                adapterMacros.notifyItemChanged(pos)
            }
            .show()
    }

    private fun carregarMacros() {
        lifecycleScope.launch {
            val macros = withContext(Dispatchers.IO) {
                AmadeusDatabase.getInstance(this@ListaMacrosActivity).agentMacroDAO().listAll()
            }
            adapterMacros.trocarLista(macros)
            atualizarEmptyState()
        }
    }

    private fun atualizarEmptyState() {
        emptyStateTextView.visibility = if (adapterMacros.itemCount == 0) View.VISIBLE else View.GONE
        recyclerView.visibility = if (adapterMacros.itemCount == 0) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        carregarMacros()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
