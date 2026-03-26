package com.paradoxo.amadeus.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.modelo.Entidade

class AdapterSimplesEntidade(var itens: MutableList<Entidade>) : RecyclerView.Adapter<AdapterSimplesEntidade.ViewHolder>() {
    private var onItemClickListener: OnItemClickListener? = null
    private var onLongClickListener: OnLongClickListener? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tituloTexView: TextView = view.findViewById(R.id.tituloTexView)
        val conteudoTexView: TextView = view.findViewById(R.id.conteudoTexView)
        val layoutItemComum: View = view.findViewById(R.id.layoutItemComum)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_lista_tipo_4, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entidade = itens[position]
        holder.tituloTexView.text = entidade.nome
        holder.conteudoTexView.text = TextUtils.join(",", entidade.sinonimos ?: emptyList<String>())
        holder.layoutItemComum.setOnClickListener { v -> onItemClickListener?.onItemClick(v, entidade, position) }
        holder.layoutItemComum.setOnLongClickListener { v ->
            onLongClickListener?.onLongClickListener(v, position, entidade)
            true
        }
    }

    override fun getItemCount(): Int = itens.size

    override fun getItemViewType(position: Int): Int = position

    fun setOnItemClickListener(listener: OnItemClickListener) { onItemClickListener = listener }
    fun setOnLongClickListener(listener: OnLongClickListener) { onLongClickListener = listener }

    fun add(entidade: Entidade) { itens.add(entidade); notifyItemInserted(itens.size) }
    fun remove(position: Int) { itens.removeAt(position); notifyItemRemoved(position) }
    fun add(entidade: Entidade, posi: Int) { itens.add(posi, entidade); notifyItemChanged(posi, entidade) }
    fun addAll(novos: List<Entidade>) { itens.addAll(novos); notifyDataSetChanged() }
    fun trocarLista(novaLista: List<Entidade>) { itens = novaLista.toMutableList(); notifyDataSetChanged() }

    fun interface OnItemClickListener {
        fun onItemClick(view: View, entidade: Entidade, pos: Int)
    }

    fun interface OnLongClickListener {
        fun onLongClickListener(view: View, position: Int, entidade: Entidade)
    }
}
