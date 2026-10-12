package com.paradoxo.amadeus.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import com.paradoxo.amadeus.activity.CadastroInicialActivity
import com.paradoxo.amadeus.activity.MainActivity
import com.paradoxo.amadeus.onboarding.ui.OnboardingScreen
import com.paradoxo.amadeus.util.Preferencias

class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                OnboardingScreen(onFinish = ::completeOnboarding)
            }
        }
    }

    private fun completeOnboarding(dontShowAgain: Boolean) {
        if (dontShowAgain) {
            Preferencias.setPrefBool(Preferencias.PREF_ONBOARDING_AGENT_SEEN, true, this)
        }
        val destino = if (Preferencias.getPrefString("nomeUsu", this).isEmpty()) {
            CadastroInicialActivity::class.java
        } else {
            MainActivity::class.java
        }
        startActivity(Intent(this, destino))
        finish()
    }
}
