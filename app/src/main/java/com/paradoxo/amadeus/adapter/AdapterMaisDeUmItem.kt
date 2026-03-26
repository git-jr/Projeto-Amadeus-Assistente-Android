package com.paradoxo.amadeus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.modelo.Sentenca

class AdapterMaisDeUmItem(val itens: MutableList<Sentenca>) : RecyclerView.Adapter<AdapterMaisDeUmItem.ViewHolder>() {
    private var onItemClickListener: OnItemClickListener? = null
    private var onLongClickListener: OnLongClickListener? = null

    private val VIEW_ITEM_2 = 2
    private val VIEW_ITEM_3 = 3
    private val VIEW_ITEM_5 = 5

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val conteudoTexView: TextView? = view.findViewById(R.id.conteudoTexView)
        val tituloTexView: TextView? = view.findViewById(R.id.tituloTexView)
        val layoutItemComum: View? = view.findViewById(R.id.layoutItemComum)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = when (viewType) {
            VIEW_ITEM_2 -> R.layout.item_lista_tipo_2
            VIEW_ITEM_3 -> R.layout.item_lista_tipo_3
            VIEW_ITEM_5 -> R.layout.item_lista_tipo_5
            else -> R.layout.item_lista_tipo_1
        }
        return ViewHolder(LayoutInflater.from(parent.context).inflate(layoutId, parent, false))
    }

    override fun getItemViewType(position: Int): Int = when (itens[position].tipo_item) {
        2 -> VIEW_ITEM_2
        3 -> VIEW_ITEM_3
        5 -> VIEW_ITEM_5
        else -> 1
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sentenca = itens[position]
        val itemViewType = getItemViewType(position)

        if (itemViewType == VIEW_ITEM_5) return

        if (sentenca.tipo_item >= 0) {
            holder.conteudoTexView?.text = sentenca.respostas[0]
            if (itemViewType == VIEW_ITEM_3 && sentenca.respostas.size > 1) {
                holder.tituloTexView?.text = sentenca.respostas[1]
                holder.conteudoTexView?.text = sentenca.respostas[0]
            }
            holder.layoutItemComum?.setOnClickListener { v -> onItemClickListener?.onItemClick(v, sentenca, position) }
            holder.layoutItemComum?.setOnLongClickListener { v ->
                onLongClickListener?.onLongClickListener(v, position, sentenca)
                true
            }
        }
    }

    override fun getItemCount(): Int = itens.size

    fun setOnItemClickListener(listener: OnItemClickListener) { onItemClickListener = listener }
    fun setOnLongClickListener(listener: OnLongClickListener) { onLongClickListener = listener }

    fun add(sentenca: Sentenca) { itens.add(sentenca); notifyItemInserted(itemCount) }
    fun remove(position: Int) { itens.removeAt(position); notifyItemRemoved(position) }
    fun add(sentenca: Sentenca, posi: Int) { itens.add(posi, sentenca); notifyItemChanged(posi, sentenca) }
    fun addAll(novos: List<Sentenca>) { itens.addAll(novos); notifyDataSetChanged() }
    fun interface OnItemClickListener {
        fun onItemClick(view: View, sentenca: Sentenca, pos: Int)
    }

    fun interface OnLongClickListener {
        fun onLongClickListener(view: View, position: Int, mensagem: Sentenca)
    }
}
