package com.paradoxo.amadeus.activity

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.adapter.AdapterVozes
import com.paradoxo.amadeus.modelo.Voz
import com.paradoxo.amadeus.util.Preferencias.getPrefString
import com.paradoxo.amadeus.util.Preferencias.setPrefString
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca
import java.util.Locale

class TrocarVozActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private var estiloVozAlterado = false

    companion object {
        const val PREF_ESTILO_DE_VOZ_PADRAO = "estiloDeVozPadrao"

        var estiloDeVozPadrao: String = ""
        var textToSpeech: TextToSpeech? = null

        private fun configurarRecycler(vozes: List<Voz>, context: Activity) {
            val recyclerViewVozes = context.findViewById<RecyclerView>(R.id.recyclerView)
            val adapterVozes = AdapterVozes(vozes)
            recyclerViewVozes.adapter = adapterVozes

            adapterVozes.setOnItemClickListener { _, _, voz ->
                estiloDeVozPadrao = voz.codigo
                falar(context)
                alterarTextoToolbar(context)
            }
        }

        private fun alterarTextoToolbar(context: Activity) {
            (context.findViewById<Toolbar>(R.id.toolbar)).title = estiloDeVozPadrao
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun falar(context: Activity) {
            val voz = Voice(estiloDeVozPadrao, Locale.getDefault(), 1, 1, false, null)
            textToSpeech?.setVoice(voz)

            val textViewTextoFalar = context.findViewById<EditText>(R.id.textoFalarEditText)
            var textoASerDito = textViewTextoFalar.text.toString()
            if (textoASerDito.isEmpty()) textoASerDito = "Essa é uma frase teste"

            textToSpeech?.speak(textoASerDito, TextToSpeech.QUEUE_FLUSH, null)
        }

        private fun mostrarLayoutVozes(context: Activity) {
            context.findViewById<View>(R.id.layoutLoad).visibility = View.GONE
            context.findViewById<View>(R.id.layoutPrinicipal).visibility = View.VISIBLE
        }

        @Suppress("DEPRECATION")
        private fun carregarVozes(carregarTodas: Boolean, context: Activity) {
            object : AsyncTask<Void?, Void?, List<Voz>>() {
                override fun doInBackground(vararg voids: Void?): List<Voz> {
                    val vozes = mutableListOf<Voz>()
                    try {
                        for (voice in textToSpeech!!.voices) {
                            val voz = Voz(voice.locale.displayName, voice.locale.displayLanguage, voice.name)
                            if (carregarTodas) {
                                vozes.add(voz)
                            } else if (voice.name.contains("-language")) {
                                vozes.add(voz)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return vozes
                }

                override fun onPostExecute(vozes: List<Voz>) {
                    super.onPostExecute(vozes)
                    configurarRecycler(vozes, context)
                    mostrarLayoutVozes(context)
                    meuToast("${vozes.size} ${context.getString(R.string.numero_vozes_econtradas)}", context)
                }
            }.execute()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trocar_voz)
        configurarFala()
        configurarInterface()
    }

    private fun configurarInterface() {
        configurarToolBarBranca(this)
        configurarToolbar()
        configurarBotaoFala()
    }

    private fun configurarFala() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech!!.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    meuToast(getString(R.string.lingua_nao_suportada), this)
                    Log.e("TTS", getString(R.string.lingua_nao_suportada))
                } else {
                    Log.e("TTS", "TTS")
                    carregarVozes(false, this)
                }
            }
        }
    }

    private fun configurarToolbar() {
        toolbar = findViewById(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        estiloDeVozPadrao = getPrefString(PREF_ESTILO_DE_VOZ_PADRAO, this)
        estiloDeVozPadrao = if (estiloDeVozPadrao.isEmpty()) getString(R.string.padrao) else estiloDeVozPadrao
        alterarTextoToolbar(this)
    }

    private fun configurarBotaoFala() {
        findViewById<View>(R.id.falarButton).setOnClickListener { falar(this) }
    }

    private fun gravarVozSelecionada() {
        setPrefString(estiloDeVozPadrao, PREF_ESTILO_DE_VOZ_PADRAO, this)
        meuToast(getString(R.string.estilo_voz_de_voz_alterado), this)
        estiloVozAlterado = true
    }

    fun reiniciarApp() {
        AlertDialog.Builder(this)
            .setTitle(getText(R.string.reiniciar).toString())
            .setMessage(getText(R.string.alteracao_realizada).toString())
            .setPositiveButton(getText(R.string.agora).toString()) { _, _ ->
                finishAffinity()
                startActivity(Intent(this@TrocarVozActivity, MainActivity::class.java))
            }
            .setNegativeButton(getText(R.string.reiniciar_depois).toString()) { _, _ ->
                meuToast(getString(R.string.mudancas_serao_aplicadas), applicationContext)
                finish()
            }
            .create()
            .show()
    }

    private fun carregarVozesRepetidas() {
        AlertDialog.Builder(this)
            .setTitle(getText(R.string.carregar_todas).toString())
            .setMessage(getString(R.string.aviso_vozes_repetidas))
            .setPositiveButton(getText(R.string.agora).toString()) { _, _ -> carregarVozes(true, this) }
            .setNegativeButton(getText(R.string.cancelar).toString()) { _, _ -> }
            .create()
            .show()
    }

    private fun voltarParaVozPadrao() {
        setPrefString("default", PREF_ESTILO_DE_VOZ_PADRAO, this)
        meuToast(getString(R.string.voz_padrao_selecionada), this)
        estiloVozAlterado = true
        estiloDeVozPadrao = getString(R.string.padrao)
        alterarTextoToolbar(this)
        estiloDeVozPadrao = "default"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_tipo_voz, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when (item.itemId) {
            R.id.salvar -> gravarVozSelecionada()
            R.id.vozPadrao -> voltarParaVozPadrao()
            R.id.carregarRepetidas -> carregarVozesRepetidas()
        }
        return true
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (estiloVozAlterado) {
            reiniciarApp()
        } else {
            super.onBackPressed()
        }
    }
}
