package com.paradoxo.amadeus.util;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

public class Preferencias {
    private static final String PREFS_USU = "PrefsUsu";
    private Context context;

    public Preferencias(Context context) {
        this.context = context;
    }

    public static void confirmarAberturaApp(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_USU, MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putBoolean("ja_foi_aberto", true);
        mEditor.apply();
    }

    public static boolean appJaFoiAberto(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_USU, MODE_PRIVATE);
        return sharedPreferences.getBoolean("ja_foi_aberto", false);
    }

    private void setPrefBool(String nomeShared, boolean valor) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_USU, MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putBoolean(nomeShared, valor);
        mEditor.apply();
    }

    private boolean getPrefBool(String nomePref) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_USU, MODE_PRIVATE);
        return sharedPreferences.getBoolean(nomePref, false);
    }

    public static void setPrefString(String textoShared, String nomeShared, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_USU, MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putString(nomeShared, textoShared);
        mEditor.apply();
    }

    public static String getPrefString(String nomeShared, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_USU, MODE_PRIVATE);
        return sharedPreferences.getString(nomeShared, "");
    }
}
