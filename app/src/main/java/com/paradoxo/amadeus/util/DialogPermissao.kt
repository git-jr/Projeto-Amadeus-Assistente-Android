package com.paradoxo.amadeus.util

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt

object DialogPermissao {

    fun exibirRationale(
        activity: AppCompatActivity,
        titulo: CharSequence,
        mensagem: CharSequence,
        textoConfirmar: CharSequence,
        textoCancelar: CharSequence,
        onConfirmar: () -> Unit,
        onCancelar: () -> Unit = {}
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val tituloPreto = SpannableString(titulo).apply {
            setSpan(ForegroundColorSpan(Color.BLACK), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val mensagemPreta = SpannableString(mensagem).apply {
            setSpan(ForegroundColorSpan(Color.BLACK), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle(tituloPreto)
            .setMessage(mensagemPreta)
            .setPositiveButton(textoConfirmar) { dialog, _ ->
                dialog.dismiss()
                onConfirmar()
            }
            .setNegativeButton(textoCancelar) { dialog, _ ->
                dialog.dismiss()
                onCancelar()
            }
            .setOnCancelListener {
                onCancelar()
            }
            .show()

        dialog.window?.setBackgroundDrawable(Color.WHITE.toDrawable())
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor("#F50057".toColorInt())
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }
}

