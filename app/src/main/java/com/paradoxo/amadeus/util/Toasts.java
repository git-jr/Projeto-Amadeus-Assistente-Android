package com.paradoxo.amadeus.util;

import android.content.Context;
import android.widget.Toast;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;

public class Toasts {
    public static void meuToast(String texto, Context context) {
        Toast.makeText(context, texto, LENGTH_SHORT).show();
    }
    public static void meuToastLong(String texto, Context context) {
        Toast.makeText(context, texto, LENGTH_LONG).show();
    }
}
