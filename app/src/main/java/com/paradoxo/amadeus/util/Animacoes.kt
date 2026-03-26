package com.paradoxo.amadeus.util

import android.view.View
import android.view.ViewGroup
import androidx.transition.Fade
import androidx.transition.TransitionManager

object Animacoes {
    fun animarComFade(viewAnimada: View, mostrar: Boolean) {
        val transition = Fade().apply {
            duration = 100
            addTarget(viewAnimada)
        }
        TransitionManager.beginDelayedTransition(viewAnimada.parent as ViewGroup, transition)
        viewAnimada.visibility = if (mostrar) View.VISIBLE else View.GONE
    }
}
