package com.paradoxo.amadeus.util

import android.content.Context
import android.content.Context.MODE_PRIVATE

object Preferencias {
    private const val PREFS_USU = "PrefsUsu"
    const val PREF_JA_FOI_ABERTO = "ja_foi_aberto"

    fun confirmarAberturaApp(context: Context) {
        context.getSharedPreferences(PREFS_USU, MODE_PRIVATE).edit()
            .putBoolean(PREF_JA_FOI_ABERTO, true)
            .apply()
    }

    fun appJaFoiAberto(context: Context): Boolean =
        context.getSharedPreferences(PREFS_USU, MODE_PRIVATE)
            .getBoolean(PREF_JA_FOI_ABERTO, false)

    fun setPrefBool(nomeShared: String, valor: Boolean, context: Context) {
        context.getSharedPreferences(PREFS_USU, MODE_PRIVATE).edit()
            .putBoolean(nomeShared, valor)
            .apply()
    }

    fun getPrefBool(nomePref: String, context: Context, valorPadrao: Boolean): Boolean =
        context.getSharedPreferences(PREFS_USU, MODE_PRIVATE)
            .getBoolean(nomePref, valorPadrao)

    fun setPrefString(conteudoShared: String, nomeShared: String, context: Context) {
        context.getSharedPreferences(PREFS_USU, MODE_PRIVATE).edit()
            .putString(nomeShared, conteudoShared)
            .apply()
    }

    fun getPrefString(nomeShared: String, context: Context): String =
        context.getSharedPreferences(PREFS_USU, MODE_PRIVATE)
            .getString(nomeShared, "") ?: ""

    fun getPrefLong(nomeShared: String, context: Context): Long =
        context.getSharedPreferences(PREFS_USU, MODE_PRIVATE)
            .getLong(nomeShared, 0)

    fun setPrefLong(nomeShared: String, valorShared: Long, context: Context) {
        context.getSharedPreferences(PREFS_USU, MODE_PRIVATE).edit()
            .putLong(nomeShared, valorShared)
            .apply()
    }
}
