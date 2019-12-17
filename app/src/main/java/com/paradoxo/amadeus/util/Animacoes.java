package com.paradoxo.amadeus.util;

import android.view.View;
import android.view.ViewGroup;

import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

public class Animacoes {
    public static void animarComFade(View viewAnimada, boolean mostrar) {
        Transition transition = new Fade();

        transition.setDuration(100);
        transition.addTarget(viewAnimada);

        TransitionManager.beginDelayedTransition((ViewGroup) viewAnimada.getParent(), transition);
        viewAnimada.setVisibility(mostrar ? View.VISIBLE : View.GONE);
    }
}
