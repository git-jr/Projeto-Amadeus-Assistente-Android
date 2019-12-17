package com.paradoxo.amadeus.util;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

public class Preferencias {
    private static final String PREFS_USU = "PrefsUsu";
    public static final String PREF_JA_FOI_ABERTO = "ja_foi_aberto";

    public static void confirmarAberturaApp(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_USU, MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putBoolean(PREF_JA_FOI_ABERTO,true);
        mEditor.apply();
    }

    public static boolean appJaFoiAberto(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_USU, MODE_PRIVATE);
        return sharedPreferences.getBoolean(PREF_JA_FOI_ABERTO, false);
    }

    public static void setPrefBool(String nomeShared, boolean valor, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_USU, MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putBoolean(nomeShared, valor);
        mEditor.apply();
    }

    public static boolean getPrefBool(String nomePref, Context context, boolean valorPadrao) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_USU, MODE_PRIVATE);
        return sharedPreferences.getBoolean(nomePref, valorPadrao);
    }

    public static void setPrefString(String conteudoShared, String nomeShared, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_USU, MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putString(nomeShared, conteudoShared);
        mEditor.apply();
    }

    public static String getPrefString(String nomeShared, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_USU, MODE_PRIVATE);
        return sharedPreferences.getString(nomeShared, "");
    }

    public static Long getPrefLong(String nomeShared, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_USU, MODE_PRIVATE);
        return sharedPreferences.getLong(nomeShared, 0);
    }

    public static void setPrefLong(String nomeShared, long valorShared, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_USU, MODE_PRIVATE);
        SharedPreferences.Editor mEditor = sharedPreferences.edit();
        mEditor.putLong(nomeShared, valorShared);
        mEditor.apply();
    }

}
