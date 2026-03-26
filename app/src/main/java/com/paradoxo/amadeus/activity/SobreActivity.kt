package com.paradoxo.amadeus.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca

class SobreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sobre)
        configurarInterface()
    }

    private fun configurarInterface() {
        configurarToolBarBranca(this)
        carregarTextoVersao()
        configurarClickBotoes()
        configurarClikLicencas()
    }

    private fun configurarClickBotoes() {
        configurarClickLink(R.id.irParaYouTubeLayout, getString(R.string.link_playlist_amadeus_yt))
        configurarClickLink(R.id.enviarFeedBackLayout, getString(R.string.linkAmadeusGooglePlayStore))
        configurarClickLink(R.id.gitHubLayout, getString(R.string.linkAmadeusGitHub))
        configurarClickLink(R.id.creditosLottieApi1TextView, getString(R.string.link_lottie_api_1))
        configurarClickLink(R.id.creditosLottieApi2TextView, getString(R.string.link_lottie_api_2))
    }

    private fun carregarTextoVersao() {
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val versao = getString(R.string.versao) + " " + pInfo.versionName
            findViewById<TextView>(R.id.versaoTextView).text = versao
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun configurarClickLink(viewSelecionada: Int, link: String) {
        findViewById<android.view.View>(viewSelecionada).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(link)
            startActivity(intent)
        }
    }

    private fun configurarClikLicencas() {
        findViewById<android.view.View>(R.id.licencaCodigoLayout).setOnClickListener {
            startActivity(Intent(this@SobreActivity, OssLicensesMenuActivity::class.java))
        }
    }
}
