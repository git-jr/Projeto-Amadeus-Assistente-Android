package com.paradoxo.amadeus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.modelo.Sentenca

class AdapterSimples(private var itens: MutableList<Sentenca>) : RecyclerView.Adapter<AdapterSimples.ViewHolder>() {
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
        val sentenca = itens[position]
        holder.tituloTexView.text = sentenca.chave
        holder.conteudoTexView.text = sentenca.respostas[0]
        holder.layoutItemComum.setOnClickListener { v -> onItemClickListener?.onItemClick(v, sentenca, position) }
        holder.layoutItemComum.setOnLongClickListener { v ->
            onLongClickListener?.onLongClickListener(v, position, sentenca)
            true
        }
    }

    override fun getItemCount(): Int = itens.size

    fun setOnItemClickListener(listener: OnItemClickListener) { onItemClickListener = listener }
    fun setOnLongClickListener(listener: OnLongClickListener) { onLongClickListener = listener }

    fun add(sentenca: Sentenca) { itens.add(sentenca); notifyItemInserted(itens.size) }
    fun remove(position: Int) { itens.removeAt(position); notifyItemRemoved(position) }
    fun add(sentenca: Sentenca, posi: Int) { itens.add(posi, sentenca); notifyItemChanged(posi, sentenca) }
    fun addAll(novoItens: List<Sentenca>) { itens.addAll(novoItens); notifyDataSetChanged() }
    fun getItens(): MutableList<Sentenca> = itens
    fun trocarLista(novaLista: List<Sentenca>) { itens = novaLista.toMutableList(); notifyDataSetChanged() }

    fun interface OnItemClickListener {
        fun onItemClick(view: View, sentenca: Sentenca, pos: Int)
    }

    fun interface OnLongClickListener {
        fun onLongClickListener(view: View, position: Int, sentenca: Sentenca)
    }
}
