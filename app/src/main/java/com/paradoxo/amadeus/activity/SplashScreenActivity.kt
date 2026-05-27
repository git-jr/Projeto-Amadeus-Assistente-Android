package com.paradoxo.amadeus.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.lifecycleScope
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.cognicao.RemoteConfigManager
import com.paradoxo.amadeus.onboarding.OnboardingActivity
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.DefaultMacroSeeder
import com.paradoxo.amadeus.dao.room.LocalModelSeeder
import com.paradoxo.amadeus.dao.room.JsonAssetLoader
import com.paradoxo.amadeus.dao.room.toEntity
import com.paradoxo.amadeus.dao.room.toHistoricoEntity
import com.paradoxo.amadeus.util.Animacoes
import com.paradoxo.amadeus.util.Preferencias
import com.paradoxo.amadeus.util.Preferencias.appJaFoiAberto
import com.paradoxo.amadeus.util.Preferencias.confirmarAberturaApp
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.Toasts.meuToastLong
import com.paradoxo.amadeus.util.Util.aplicarInsets
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        aplicarInsets(findViewById(R.id.splashScreenLayout))
        configurarToolBarBranca(this)
        iniciar()
    }

    private fun iniciar() {
        lifecycleScope.launch {
            // ── Etapa 0: buscar configurações remotas (prompts, modelo, provider) ─
            RemoteConfigManager.fetchAndActivate()

            // ── Etapa 1: dados legados (tabela mensagem) ────────────────────
            val bancoAntigoExiste = withContext(Dispatchers.IO) {
                AmadeusDatabase.getInstance(this@SplashScreenActivity)
                    .mensagemDAO().contarMensagens() > 0
            }

            if (bancoAntigoExiste) {
                startActivity(Intent(this@SplashScreenActivity, RetroCompatibilidadeActivity::class.java))
                finish()
                return@launch
            }

            // ── Etapa 2: seed de macros padrão e modelos locais (idempotente) ──
            withContext(Dispatchers.IO) {
                val db = AmadeusDatabase.getInstance(this@SplashScreenActivity)
                DefaultMacroSeeder.seed(db)
                LocalModelSeeder.seed(db)
            }

            // ── Etapa 3: carga inicial de assets ───────────────────────────
            verificarBanco(appJaFoiAberto(this@SplashScreenActivity), this@SplashScreenActivity)
        }
    }

    override fun onStart() {
        super.onStart()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    override fun onStop() {
        super.onStop()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    companion object {
        private fun trocarTextoPreCarregamentoBanco(context: Activity) {
            context.findViewById<TextView>(R.id.subTituloTextView)?.text = context.getString(R.string.carregando_banco)
            context.findViewById<TextView>(R.id.subTituloTextView)?.text =
                context.getString(R.string.loading)
            Animacoes.animarComFade(context.findViewById(R.id.logoAmadeusImageView), false)
            Animacoes.animarComFade(context.findViewById(R.id.progressoLayout), true)
        }

        private fun atualizarProgresso(porcentagem: Int, context: Activity) {
            context.findViewById<ProgressBar>(R.id.barraProgresso)?.let {
                it.isIndeterminate = false
                it.progress = porcentagem
            }
            context.findViewById<TextView>(R.id.progressoTextView)?.text = "$porcentagem%"
        }

        private fun decidirParaOndevai(context: Activity) {
            val onboardingVisto = Preferencias.getPrefBool(
                Preferencias.PREF_ONBOARDING_AGENT_SEEN, context, false
            )

            val destino = when {
                !onboardingVisto -> OnboardingActivity::class.java
                Preferencias.getPrefString("nomeUsu", context).isEmpty() -> CadastroInicialActivity::class.java
                else -> MainActivity::class.java
            }
            context.startActivity(Intent(context.applicationContext, destino))
            context.finish()
        }

        private fun verificarBanco(bancoJaInserido: Boolean, context: AppCompatActivity) {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val longVersionCode = PackageInfoCompat.getLongVersionCode(pInfo)

                if (longVersionCode < 18 || !bancoJaInserido) {
                    copiarBanco(context)
                } else {
                    @Suppress("DEPRECATION")
                    Handler().postDelayed({ decidirParaOndevai(context) }, 1000)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                meuToast(context.getString(R.string.erro_iniciar_app), context)
            }
        }

        private fun copiarBanco(context: AppCompatActivity) {
            context.lifecycleScope.launch {
                trocarTextoPreCarregamentoBanco(context)
                var sucesso = false

                withContext(Dispatchers.IO) {
                    try {
                        val db = AmadeusDatabase.getInstance(context)
                        val sentencasDao = db.sentencaDAO()

                        val sentencasJson = JsonAssetLoader.carregarSentencas(context)
                        val sentencasHistoricoJson = JsonAssetLoader.carregarSentencasHistorico(context)
                        val entidadesJson = JsonAssetLoader.carregarEntidades(context)

                        val progressoTotal = sentencasJson.size + sentencasHistoricoJson.size + entidadesJson.size
                        var progressoAtual = 0

                        for (sentenca in sentencasJson) {
                            sentencasDao.inserir(sentenca.toEntity())
                            progressoAtual++
                            val pct = ((progressoAtual / progressoTotal.toFloat()) * 100).toInt()
                            withContext(Dispatchers.Main) { atualizarProgresso(pct, context) }
                        }

                        for (sentenca in sentencasHistoricoJson) {
                            sentencasDao.inserirHistorico(sentenca.toHistoricoEntity())
                            progressoAtual++
                            val pct = ((progressoAtual / progressoTotal.toFloat()) * 100).toInt()
                            withContext(Dispatchers.Main) { atualizarProgresso(pct, context) }
                        }

                        for (entidade in entidadesJson) {
                            db.entidadeDAO().inserir(entidade.toEntity())
                            progressoAtual++
                            val pct = ((progressoAtual / progressoTotal.toFloat()) * 100).toInt()
                            withContext(Dispatchers.Main) { atualizarProgresso(pct, context) }
                        }

                        sucesso = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (sucesso) {
                    confirmarAberturaApp(context)
                    verificarBanco(true, context)
                } else {
                    meuToast("Erro ao carregar o banco de dados", context)
                    meuToastLong("Reinicie o app", context)
                }
            }
        }
    }
}
