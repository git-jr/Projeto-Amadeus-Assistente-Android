package com.paradoxo.amadeus.fragments

/* Código original: Nelson Glauber
 https://medium.com/@nglauber/adeus-alertdialog-bem-vindo-dialogfragment-dfaa887b575d */

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

class DialogSimples : DialogFragment(), DialogInterface.OnClickListener {
    private var posiItem = 0

    companion object {
        private const val EXTRA_POSI = "posi"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_BUTTONS = "buttons"
        const val DIALOG_TAG = "DialogSimples"

        fun newDialog(title: String, message: String, posi: Int, buttonTexts: IntArray): DialogSimples {
            return DialogSimples().apply {
                arguments = Bundle().apply {
                    putInt(EXTRA_POSI, posi)
                    putString(EXTRA_TITLE, title)
                    putString(EXTRA_MESSAGE, message)
                    putIntArray(EXTRA_BUTTONS, buttonTexts)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val titulo = args.getString(EXTRA_TITLE)
        val mensagem = args.getString(EXTRA_MESSAGE)
        val botoes = args.getIntArray(EXTRA_BUTTONS)!!
        posiItem = args.getInt(EXTRA_POSI)

        return AlertDialog.Builder(requireActivity()).apply {
            setTitle(titulo)
            setMessage(mensagem)
            if (botoes.size >= 3) setNeutralButton(botoes[2], this@DialogSimples)
            if (botoes.size >= 2) setNegativeButton(botoes[1], this@DialogSimples)
            if (botoes.isNotEmpty()) setPositiveButton(botoes[0], this@DialogSimples)
        }.create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        (requireActivity() as FragmentDialogInterface).onClick(posiItem, which)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        super.onCreateView(inflater, container, savedInstanceState)

    fun openDialog(supportFragmentManager: FragmentManager) {
        if (supportFragmentManager.findFragmentByTag(DIALOG_TAG) == null) {
            show(supportFragmentManager, DIALOG_TAG)
        }
    }

    interface FragmentDialogInterface {
        fun onClick(posi: Int, which: Int)
    }
}
