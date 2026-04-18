package com.paradoxo.amadeus.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.dao.room.entities.AgentMacroEntity
import com.paradoxo.amadeus.modelo.ActionIntent

class AdapterMacros(
    private var itens: MutableList<AgentMacroEntity>
) : RecyclerView.Adapter<AdapterMacros.ViewHolder>() {

    private var onItemClickListener: ((AgentMacroEntity) -> Unit)? = null
    private var onItemLongClickListener: ((AgentMacroEntity, View) -> Unit)? = null
    private val gson = Gson()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tituloTexView: TextView = view.findViewById(R.id.tituloTexView)
        val conteudoTexView: TextView = view.findViewById(R.id.conteudoTexView)
        val badgeTexView: TextView = view.findViewById(R.id.badgeTexView)
        val layoutItemMacro: View = view.findViewById(R.id.layoutItemMacro)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_lista_macro, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val macro = itens[position]

        holder.tituloTexView.text = macro.goalKey

        val actionCount = countActions(macro.actionsJson)
        holder.conteudoTexView.text = "$actionCount ações  •  ${macro.executionCount}× executado"

        if (macro.isConfirmed) {
            holder.badgeTexView.text = "✓ Confirmado"
            setBadgeColor(holder.badgeTexView, Color.parseColor("#4CAF50"))
        } else {
            holder.badgeTexView.text = "⏳ ${macro.executionCount}/3"
            setBadgeColor(holder.badgeTexView, Color.parseColor("#FF9800"))
        }

        holder.layoutItemMacro.setOnClickListener {
            onItemClickListener?.invoke(macro)
        }

        holder.layoutItemMacro.setOnLongClickListener {
            onItemLongClickListener?.invoke(macro, it)
            true
        }
    }

    private fun setBadgeColor(view: TextView, color: Int) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(color)
        }
        view.background = drawable
    }

    private fun countActions(actionsJson: String): Int = try {
        val type = object : TypeToken<List<ActionIntent>>() {}.type
        val list: List<ActionIntent> = gson.fromJson(actionsJson, type) ?: emptyList()
        list.size
    } catch (_: Exception) {
        0
    }

    override fun getItemCount(): Int = itens.size

    fun setOnItemClickListener(listener: (AgentMacroEntity) -> Unit) {
        onItemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: (AgentMacroEntity, View) -> Unit) {
        onItemLongClickListener = listener
    }

    fun remove(position: Int) {
        itens.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getItem(position: Int): AgentMacroEntity = itens[position]

    fun getItemPosition(macro: AgentMacroEntity): Int = itens.indexOf(macro)

    fun trocarLista(novaLista: List<AgentMacroEntity>) {
        itens = novaLista.toMutableList()
        notifyDataSetChanged()
    }
}
