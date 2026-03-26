package com.paradoxo.amadeus.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.paradoxo.amadeus.R

class AdapterSinonimos(val itens: MutableList<String>) : RecyclerView.Adapter<AdapterSinonimos.ViewHolder>() {
    private var onItemClickListener: OnItemClickListener? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tituloTexView: TextView = view.findViewById(R.id.tituloTexView)
        val botaoDeletearButton: ImageView = view.findViewById(R.id.botaoDeletearButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_lista_tipo_6, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sinonimo = itens[position]
        holder.tituloTexView.text = sinonimo
        holder.tituloTexView.setOnClickListener { v -> onItemClickListener?.onItemClick(v, sinonimo, position, false) }
        holder.botaoDeletearButton.setOnClickListener { v -> onItemClickListener?.onItemClick(v, sinonimo, position, true) }
    }

    override fun getItemCount(): Int = itens.size

    override fun getItemViewType(position: Int): Int = position

    fun setOnItemClickListener(listener: OnItemClickListener) { onItemClickListener = listener }

    fun add(sinonimo: String) { itens.add(sinonimo); notifyItemInserted(itens.size) }
    fun remove(position: Int) {
        Log.e("TM itens", itens.size.toString())
        itens.removeAt(position)
        notifyItemRemoved(position)
        Log.e("TM itens", itens.size.toString())
    }
    fun altera(position: Int, novo: String) { itens[position] = novo; notifyItemChanged(position) }
    fun add(sinonimo: String, posi: Int) { itens.add(posi, sinonimo); notifyItemChanged(posi, sinonimo) }
    fun addAll(novos: List<String>) { itens.addAll(novos); notifyDataSetChanged() }
    fun interface OnItemClickListener {
        fun onItemClick(view: View, sinonimo: String, pos: Int, tipoDeletar: Boolean)
    }
}
