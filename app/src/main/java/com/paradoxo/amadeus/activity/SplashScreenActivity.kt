package com.paradoxo.amadeus.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.dao.AutorDAO
import com.paradoxo.amadeus.dao.EntidadeDAO
import com.paradoxo.amadeus.dao.MensagemDAO
import com.paradoxo.amadeus.dao.SentencaDAO
import com.paradoxo.amadeus.modelo.Autor
import com.paradoxo.amadeus.modelo.Mensagem
import com.paradoxo.amadeus.util.Animacoes
import com.paradoxo.amadeus.util.Preferencias
import com.paradoxo.amadeus.util.Preferencias.appJaFoiAberto
import com.paradoxo.amadeus.util.Preferencias.confirmarAberturaApp
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.Toasts.meuToastLong
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var context: Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        context = this
        checarVersaoAntiga()
    }

    private fun iniciarApp() {
        configurarToolBarBranca(this)
        verificarBanco(appJaFoiAberto(this), this)
    }

    private fun checarVersaoAntiga() {
        val mensagemDAO = MensagemDAO(this)
        val bancoAntigoExiste = mensagemDAO.verificarExistencia(Mensagem(0))

        if (bancoAntigoExiste) {
            startActivity(Intent(this, RetroCompatibilidadeActivity::class.java))
            finish()
        } else {
            iniciarApp()
        }
    }

    @Suppress("DEPRECATION")
    private fun inseriMensagemTesteRetrocompatibilidade() {
        val autor = Autor(nome = "NomeAutor1")
        val autorDAO = AutorDAO(this)
        autor.id = autorDAO.inserirAutor(autor).toInt()

        val mensagemDAO = MensagemDAO(this)
        var i = 1
        while (i != 2500) {
            mensagemDAO.inserirMensagemImportada("boa $i", 1)
            mensagemDAO.inserirMensagemImportada("notche $i", 1)
            mensagemDAO.inserirRespostaImportada(i, i + 1)
            i++
            Log.e("Gravando...", i.toString())
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
            (context.findViewById<TextView>(R.id.subTituloTextView)).text = context.getString(R.string.carregando_banco)
            Animacoes.animarComFade(context.findViewById(R.id.logoAmadeusImageView), false)
            Animacoes.animarComFade(context.findViewById(R.id.progressoLayout), true)
        }

        private fun decidirParaOndevai(context: Activity) {
            if (Preferencias.getPrefString("nomeUsu", context).isNotEmpty()) {
                context.startActivity(Intent(context.applicationContext, MainActivity::class.java))
            } else {
                context.startActivity(Intent(context.applicationContext, ConfigPrimariaActivity::class.java))
            }
            context.finish()
        }

        @Suppress("DEPRECATION")
        private fun verificarBanco(bancoJaInserido: Boolean, context: Activity) {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val longVersionCode = PackageInfoCompat.getLongVersionCode(pInfo)

                if (longVersionCode < 18 || !bancoJaInserido) {
                    copiarBanco(context)
                } else {
                    Handler().postDelayed({ decidirParaOndevai(context) }, 1000)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                meuToast(context.getString(R.string.erro_iniciar_app), context)
            }
        }

        @Suppress("DEPRECATION")
        private fun copiarBanco(context: Activity) {
            object : AsyncTask<Void?, Int, Boolean>() {
                override fun onPreExecute() {
                    super.onPreExecute()
                    trocarTextoPreCarregamentoBanco(context)
                }

                override fun doInBackground(vararg voids: Void?): Boolean {
                    return try {
                        val sentencaDAO = SentencaDAO(context, false)
                        val sentencaDAOHistorico = SentencaDAO(context, true)
                        val entidadeDAO = EntidadeDAO(context)

                        val sentencasJson = sentencaDAO.sentencasPadraoJson
                        val sentencasHistoricoJson = sentencaDAOHistorico.sentencasHistoricoPadraoJson
                        val entidadesJson = entidadeDAO.entidadesPadraoJson

                        val progressoTotal = sentencasJson.size + sentencasHistoricoJson.size + entidadesJson.size
                        var progressoAtual = 0

                        for (sentenca in sentencasJson) {
                            sentencaDAO.inserir(sentenca)
                            progressoAtual++
                            publishProgress(((progressoAtual / progressoTotal.toFloat()) * 100).toInt())
                        }

                        for (sentenca in sentencasHistoricoJson) {
                            sentencaDAOHistorico.inserir(sentenca)
                            progressoAtual++
                            publishProgress(((progressoAtual / progressoTotal.toFloat()) * 100).toInt())
                        }

                        for (entidade in entidadesJson) {
                            entidadeDAO.inserir(entidade)
                            progressoAtual++
                            publishProgress(((progressoAtual / progressoTotal.toFloat()) * 100).toInt())
                        }

                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }

                override fun onPostExecute(bancoInserido: Boolean) {
                    super.onPostExecute(bancoInserido)
                    if (bancoInserido) {
                        confirmarAberturaApp(context)
                        verificarBanco(true, context)
                    } else {
                        meuToast("Erro ao carregar o banco de dados", context)
                        meuToastLong("Reinicie o app", context)
                    }
                }

                override fun onProgressUpdate(vararg values: Int?) {
                    super.onProgressUpdate(*values)
                    val porcentagemProgresso = values[0] ?: 0
                    val progressoAtual = "$porcentagemProgresso%"
                    (context.findViewById<TextView>(R.id.progressoTextView)).text = progressoAtual
                    (context.findViewById<ProgressBar>(R.id.barraProgresso)).progress = porcentagemProgresso
                }
            }.execute()
        }
    }
}
