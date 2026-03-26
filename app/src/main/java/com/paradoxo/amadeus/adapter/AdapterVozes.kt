package com.paradoxo.amadeus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.modelo.Voz

class AdapterVozes(vozes: List<Voz>) : RecyclerView.Adapter<AdapterVozes.ViewHolder>() {
    private val vozes: MutableList<Voz> = vozes.toMutableList()
    private var onItemClickListener: OnItemClickListener? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewNome: TextView = view.findViewById(R.id.nomeVozTextView)
        val textViewIdioma: TextView = view.findViewById(R.id.idiomaTextView)
        val textViewCodigo: TextView = view.findViewById(R.id.codigoTextView)
        val viewCaixaConteudo: View = view.findViewById(R.id.layoutItemComum)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tipo_voz, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val voz = vozes[position]
        holder.textViewNome.text = voz.nome
        holder.textViewIdioma.text = voz.idioma
        holder.textViewCodigo.text = voz.codigo
        holder.viewCaixaConteudo.setOnClickListener { v ->
            onItemClickListener?.onItemClickListener(v, position, voz)
        }
    }

    override fun getItemCount(): Int = vozes.size

    override fun getItemViewType(position: Int): Int = position

    fun setOnItemClickListener(listener: OnItemClickListener) { onItemClickListener = listener }

    fun add(voz: Voz) {
        vozes.add(voz)
        notifyItemInserted(vozes.size)
    }

    fun interface OnItemClickListener {
        fun onItemClickListener(view: View, position: Int, voz: Voz)
    }
}
