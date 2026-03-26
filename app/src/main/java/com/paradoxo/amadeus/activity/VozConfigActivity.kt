package com.paradoxo.amadeus.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.util.Preferencias.getPrefBool
import com.paradoxo.amadeus.util.Preferencias.setPrefBool
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca

class VozConfigActivity : AppCompatActivity() {

    companion object {
        const val PREF_VOZ_ATIVA = "voz_ativa"
        const val PREF_FALAR_RESPOSTA_NAO_ENCONTRADA = "falar_resposta_nao_encontrada"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voz_config)
        configurarInterface()
    }

    private fun configurarInterface() {
        configurarToolBarBranca(this)
        configurarToggleButton()
        configurarItensMenu()
    }

    private fun configurarToggleButton() {
        val modoFalaToggleButton = findViewById<ToggleButton>(R.id.modoFalaToggleButton)
        val falarRespostasNaoEncontradasToggleButton = findViewById<ToggleButton>(R.id.falarRespostasNaoEncontradasToggleButton)

        modoFalaToggleButton.isChecked = getPrefBool(PREF_VOZ_ATIVA, this, false)
        falarRespostasNaoEncontradasToggleButton.isChecked = getPrefBool(PREF_FALAR_RESPOSTA_NAO_ENCONTRADA, this, false)

        modoFalaToggleButton.setOnCheckedChangeListener { _, valor -> setPrefBool(PREF_VOZ_ATIVA, valor, applicationContext) }
        falarRespostasNaoEncontradasToggleButton.setOnCheckedChangeListener { _, valor -> setPrefBool(PREF_FALAR_RESPOSTA_NAO_ENCONTRADA, valor, applicationContext) }
    }

    private fun configurarItensMenu() {
        val tipoVozLayout = findViewById<LinearLayout>(R.id.tipoVozLayout)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tipoVozLayout.setOnClickListener {
                startActivity(Intent(applicationContext, TrocarVozActivity::class.java))
            }
        } else {
            tipoVozLayout.visibility = View.GONE
        }
    }
}
