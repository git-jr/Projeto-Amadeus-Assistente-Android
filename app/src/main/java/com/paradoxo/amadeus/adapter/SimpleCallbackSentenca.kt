package com.paradoxo.amadeus.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.dao.SentencaDAO
import com.paradoxo.amadeus.util.Toasts

class SimpleCallbackSentenca(
    private val adapterSentenca: AdapterSimples,
    private val context: Context
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
    private val icon = ContextCompat.getDrawable(context, R.drawable.ic_delete)!!.apply {
        colorFilter = LightingColorFilter(Color.WHITE, Color.WHITE)
    }
    private val background = ColorDrawable(Color.RED)

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        val itemView = viewHolder.itemView
        val deslocamentoBorda = 20
        val margemIcone = (itemView.height - icon.intrinsicHeight) / 2
        val iconePosiTop = itemView.top + margemIcone
        val iconePosiBottom = iconePosiTop + icon.intrinsicHeight

        when {
            dX > 0 -> {
                val esquerda = itemView.left + margemIcone
                icon.setBounds(esquerda, iconePosiTop, esquerda + icon.intrinsicWidth, iconePosiBottom)
                background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt() + deslocamentoBorda, itemView.bottom)
            }
            dX < 0 -> {
                val direita = itemView.right - margemIcone
                icon.setBounds(direita - icon.intrinsicWidth, iconePosiTop, direita, iconePosiBottom)
                background.setBounds(itemView.right + dX.toInt() - deslocamentoBorda, itemView.top, itemView.right, itemView.bottom)
            }
            else -> background.setBounds(0, 0, 0, 0)
        }
        background.draw(c)
        icon.draw(c)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        excluirMensagem(viewHolder.adapterPosition)
    }

    private fun excluirMensagem(pos: Int) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.confirmar_exclusao))
            .setMessage(context.getString(R.string.cornfimar_exclusao_item))
            .setPositiveButton(R.string.ok) { _, _ ->
                try {
                    SentencaDAO(context, false).excluir(adapterSentenca.getItens()[pos])
                    adapterSentenca.remove(pos)
                    Toasts.meuToast(context.getString(R.string.excluido_com_sucesso), context)
                    if (adapterSentenca.getItens().isEmpty()) {
                        Toasts.meuToast(context.getString(R.string.sem_sentencas_no_momento), context)
                    }
                } catch (e: Exception) {
                    Toasts.meuToast(context.getString(R.string.erro_excluir_sentenca), context)
                    e.printStackTrace()
                }
            }
            .setNegativeButton(R.string.cancelar) { _, _ -> adapterSentenca.notifyItemChanged(pos) }
            .show()
    }
}
