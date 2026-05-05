package com.paradoxo.amadeus.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.paradoxo.amadeus.R

object Util {
    fun aplicarInsets(view: View) {
        val initialPaddingLeft = view.paddingLeft
        val initialPaddingTop = view.paddingTop
        val initialPaddingRight = view.paddingRight
        val initialPaddingBottom = view.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val types = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            val bars = insets.getInsets(types)
            v.setPadding(
                initialPaddingLeft + bars.left,
                initialPaddingTop + bars.top,
                initialPaddingRight + bars.right,
                initialPaddingBottom + bars.bottom
            )
            insets
        }
    }
    fun configurarToolBarBranca(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.window.apply {
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                statusBarColor = Color.WHITE
            }
            val view = activity.findViewById<View>(android.R.id.content)
            view.systemUiVisibility = view.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    fun configurarCorToolBar(activity: Activity, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.window.apply {
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                statusBarColor = color
            }
            val view = activity.findViewById<View>(android.R.id.content)
            view.systemUiVisibility = view.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    fun esconderTeclado(activity: Activity) {
        try {
            val view = activity.currentFocus ?: return
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (e: Exception) {
            Log.e("Teclado", "Não pode ser carregado")
        }
    }

    fun esconderTeclado(view: View, activity: Activity) {
        try {
            val imm = activity.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (e: Exception) {
            Log.e("Teclado", "Não pode ser carregado")
        }
    }
}

fun AppCompatActivity.setupToolbar(titulo: String? = null) {
    Util.configurarToolBarBranca(this)
    val toolbar = findViewById<Toolbar>(R.id.toolbar) ?: return
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    if (titulo != null) title = titulo
    toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
}
