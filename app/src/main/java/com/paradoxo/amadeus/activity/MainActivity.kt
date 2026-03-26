package com.paradoxo.amadeus.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import com.airbnb.lottie.LottieAnimationView
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.adapter.AdapterMaisDeUmItem
import com.paradoxo.amadeus.cognicao.Acionadora
import com.paradoxo.amadeus.cognicao.Processadora
import com.paradoxo.amadeus.cognicao.faisca.Inicia
import com.paradoxo.amadeus.dao.AcaoDAO
import com.paradoxo.amadeus.dao.SentencaDAO
import com.paradoxo.amadeus.enums.AcaoEnum
import com.paradoxo.amadeus.enums.ItemEnum
import com.paradoxo.amadeus.enums.ItemEnum.ITEM_LOAD
import com.paradoxo.amadeus.fragments.DialogSimples
import com.paradoxo.amadeus.modelo.Acao
import com.paradoxo.amadeus.modelo.EventoMensagem
import com.paradoxo.amadeus.modelo.Sentenca
import com.paradoxo.amadeus.service.GravaHistoricoService
import com.paradoxo.amadeus.util.Animacoes.animarComFade
import com.paradoxo.amadeus.util.Preferencias
import com.paradoxo.amadeus.util.Toasts.meuToast
import com.paradoxo.amadeus.util.Toasts.meuToastLong
import com.paradoxo.amadeus.util.Util.configurarToolBarBranca
import com.paradoxo.amadeus.util.Util.esconderTeclado
import com.paradoxo.amadeus.util.Validadora.validarTexto
import com.paradoxo.amadeus.util.voz.EscutadaoraService
import com.paradoxo.amadeus.util.voz.TextoParaVoz
import com.paradoxo.amadeus.util.voz.VozParaTexto
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.ArrayList
import android.content.pm.PackageManager
import com.paradoxo.amadeus.util.Permissao

class MainActivity : AppCompatActivity(), DialogSimples.FragmentDialogInterface {

    private lateinit var acionadora: Acionadora
    private lateinit var botoaEnviar: ImageView
    private lateinit var textoParaVoz: TextoParaVoz
    private lateinit var processadora: Processadora
    private lateinit var mensagemEditText: EditText
    private lateinit var vozParaTexto: VozParaTexto
    private lateinit var recyclerView: RecyclerView
    private lateinit var tagMic: String
    private lateinit var tagEnviar: String
    private lateinit var textoOuvidoTextView: TextView
    private var idUltimoHistoricoGravado: Long = 0
    private lateinit var escutadaoraServiceIntent: Intent
    private lateinit var menuSlidingPaneLayout: SlidingPaneLayout

    private var emTeste = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        configurarServicosSegundoPlano()
        configurarIterface()
        // Solicita permissão de armazenamento se ainda não concedida
        if (!Permissao.armazenamentoAcessivel(this)) {
            Permissao.solicitarAcessoArmazenamento(this)
        }
    }

    private fun configurarServicosSegundoPlano() {
        escutadaoraServiceIntent = Intent(this, EscutadaoraService::class.java)
        vozParaTexto = VozParaTexto(applicationContext)
        textoParaVoz = TextoParaVoz(applicationContext)
        processadora = Processadora(this)
        acionadora = Acionadora(this)
    }

    private fun configurarIterface() {
        menuSlidingPaneLayout = findViewById(R.id.menuSlidingPaneLayout)
        textoOuvidoTextView = findViewById(R.id.textoOuvidoTextView)
        mensagemEditText = findViewById(R.id.mensagemEditText)
        tagMic = resources.getString(R.string.tag_mic)
        tagEnviar = resources.getString(R.string.tag_enviar)
        botoaEnviar = findViewById(R.id.enviarButton)
        configurarNomesIaUsu()
        configurarToolBarBranca(this)
        configurarToolbar()
        configurarRecycler()
        configurarEditeText()
        configurarItensMenu()
        configurarBotaoEnviar()
        configurarBotoaEnviarTeclado()
        val lottieAnimationView = findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        lottieAnimationView.speed = 0.5f
    }

    private fun configurarNomesIaUsu() {
        findViewById<TextView>(R.id.nomeIaTextView).text = Preferencias.getPrefString("nomeIA", this)
        findViewById<TextView>(R.id.nomeUsuarioTextView).text = Preferencias.getPrefString("nomeUsu", this)
    }

    private fun configurarBotoaEnviarTeclado() {
        mensagemEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val entrada = findViewById<EditText>(R.id.mensagemEditText).text.toString()
                validarEntrada(entrada)
                true
            } else {
                false
            }
        }
    }

    private fun validarEntrada(entrada: String) {
        if (validarTexto(entrada)) {
            adicionarResposta(Sentenca(entrada, ItemEnum.USUARIO.ordinal))
            esconderTeclado(this)
            processadora.processarEntrada(entrada)
            mensagemEditText.text.clear()
            textoOuvidoTextView.setText(R.string.ouvindo)
        }
    }

    @Subscribe
    fun adicionarResposta(sentenca: Sentenca) {
        if (adapter.itens.size != 0 && adapter.itens[adapter.itemCount - 1].tipo_item == ITEM_LOAD.ordinal) {
            adapter.remove(adapter.itemCount - 1)
            recyclerView.smoothScrollToPosition(adapter.itemCount)
        }
        val sentencaDAOHistorico = SentencaDAO(this, true)
        idUltimoHistoricoGravado = sentencaDAOHistorico.inserir(sentenca)
        sentenca.id = idUltimoHistoricoGravado.toString()
        adapter.add(sentenca)
        if (sentenca.tipo_item == ItemEnum.USUARIO.ordinal) {
            adapter.add(Sentenca(ITEM_LOAD.ordinal))
        } else {
            textoParaVoz.configurarFalaIA(sentenca.respostas[0])
        }
        Handler(mainLooper).postDelayed({ recyclerView.smoothScrollToPosition(adapter.itemCount) }, 250)
    }

    @Subscribe
    fun lidarTextoOuvido(eventoMensagem: EventoMensagem) {
        textoOuvidoTextView.text = eventoMensagem.mensagem
        if (eventoMensagem.jaTerminou) {
            trocarLayoutBottom(true)
            validarEntrada(eventoMensagem.mensagem)
        }
        val codErro = eventoMensagem.codErro
        if (codErro != 0) {
            trocarLayoutBottom(true)
            if (codErro != SpeechRecognizer.ERROR_SPEECH_TIMEOUT && codErro != SpeechRecognizer.ERROR_NO_MATCH) {
                meuToast(eventoMensagem.nomeErro ?: "", applicationContext)
            }
        }
    }

    fun iniciarEscutadoraService() {
        startService(escutadaoraServiceIntent)
    }

    fun pararEscutadoraService() {
        stopService(escutadaoraServiceIntent)
    }

    private fun configurarEditeText() {
        mensagemEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable) {
                trocarIconeBotaoEnviar(editable.toString())
            }
        })
    }

    private fun configurarBotaoEnviar() {
        botoaEnviar.setOnClickListener {
            val tag = botoaEnviar.tag.toString()
            if (tag == tagEnviar) {
                val entrada = findViewById<EditText>(R.id.mensagemEditText).text.toString()
                validarEntrada(entrada)
            } else {
                trocarLayoutBottom(false)
            }
        }
    }

    private fun configurarItensMenu() {
        configurarClickItem(R.id.configPrimariaLayout, ConfigPrimariaActivity())
        configurarClickItem(R.id.aprendizadoLayout, AprendizActivity())
        configurarClickItem(R.id.vozLayout, VozConfigActivity())
        configurarClickItem(R.id.sobreLayout, SobreActivity())
    }

    private fun configurarClickItem(view: Int, activity: Activity) {
        findViewById<View>(view).setOnClickListener {
            startActivity(Intent(this@MainActivity, activity.javaClass))
        }
    }

    private fun trocarLayoutBottom(tipoTexto: Boolean) {
        Permissao.solicitarPermissaoMicrofone(this)
        val layoutEscrevendo = findViewById<LinearLayout>(R.id.layoutEscrevendo)
        val layoutOuvindo = findViewById<LinearLayout>(R.id.layoutOuvindo)
        if (tipoTexto) {
            pararEscutadoraService()
            animarComFade(layoutOuvindo, false)
            animarComFade(layoutEscrevendo, true)
        } else {
            iniciarEscutadoraService()
            esconderTeclado(this@MainActivity)
            animarComFade(layoutEscrevendo, false)
            animarComFade(layoutOuvindo, true)
            findViewById<View>(R.id.layoutOuvindo).setOnClickListener { trocarLayoutBottom(true) }
        }
    }

    private fun trocarIconeBotaoEnviar(textoDaBusca: String) {
        val tag = botoaEnviar.tag.toString()
        if (textoDaBusca.trim().isEmpty()) {
            if (tag == tagEnviar) {
                botoaEnviar.setImageResource(R.drawable.ic_mic)
                botoaEnviar.tag = tagMic
            }
        } else {
            if (tag == tagMic) {
                botoaEnviar.setImageResource(R.drawable.ic_seta_mais)
                botoaEnviar.tag = tagEnviar
            }
        }
    }

    private fun configurarToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            if (menuSlidingPaneLayout.isOpen) {
                menuSlidingPaneLayout.closePane()
            } else {
                menuSlidingPaneLayout.openPane()
            }
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add -> startActivity(Intent(applicationContext, EditarSentencaActivity::class.java))
                R.id.action_aprendizado -> startActivity(Intent(applicationContext, AprendizActivity::class.java))
                R.id.action_sobre -> startActivity(Intent(applicationContext, SobreActivity::class.java))
            }
            false
        }
    }

    private fun configurarRecycler() {
        recyclerView = findViewById(R.id.recyclerView)
        val sentencas = ArrayList<Sentenca>()
        adapter = AdapterMaisDeUmItem(sentencas)
        recyclerView.adapter = adapter
        adapter.setOnItemClickListener { _, item, _ ->
            val itemTipoIa = item.tipo_item != ItemEnum.IA.ordinal
            val itemAcao = item.acao
            if (itemTipoIa && itemAcao != null && itemAcao != AcaoEnum.SEM_ACAO) {
                acionadora.tratarAcao(Acao(itemAcao), item.respostas[0])
            } else {
                val itemTipoUsuario = item.tipo_item == ItemEnum.USUARIO.ordinal
                if (!itemTipoIa || itemTipoUsuario) {
                    mensagemEditText.setText(item.respostas[0])
                    mensagemEditText.setSelection(mensagemEditText.length())
                }
            }
        }
        adapter.setOnLongClickListener { _, position, _ ->
            vibrar()
            abrirDialogSimples(position)
        }
        carregaSentencaBanco(this)
    }

    @Suppress("unused")
    private fun gerarListaTeste(context: Activity): List<Sentenca> {
        val acaoDAO = AcaoDAO(context)
        val acoes = acaoDAO.getAcoes() ?: emptyList()
        val itens = ArrayList<Sentenca>()
        for (acao in acoes) {
            val sentenca = Sentenca("Gatilhos: " + acao.gatilhos.toString(), AcaoEnum.SEM_ACAO)
            sentenca.tipo_item = ItemEnum.ITEM_CARD.ordinal
            val s = acao.acaoEnum.toString()
            sentenca.addResposta("Comando: $s")
            itens.add(sentenca)
        }
        itens.add(Sentenca(5))
        itens.add(Sentenca("Olá " + Preferencias.getPrefString("nomeUsu", context)))
        itens.add(Sentenca("Essa é uma versão beta experimental"))
        itens.add(Sentenca("Aperte o botão de voltar, para abrir o menu"))
        itens.add(Sentenca("Acima estão alguns comandos e seu gatilhos já dsiponíveis no app"))
        return itens
    }

    @Suppress("DEPRECATION")
    private fun vibrar() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrate(100)
    }

    fun abrirDialogSimples(posi: Int) {
        val dialog = DialogSimples.newDialog(
            getString(R.string.confirmar_exclusao),
            getString(R.string.tem_certeza_que_deseja_exluir_esse_item),
            posi,
            intArrayOf(android.R.string.ok, android.R.string.cancel)
        )
        dialog.openDialog(supportFragmentManager)
    }

    override fun onClick(posi: Int, which: Int) {
        when (which) {
            -1 -> {
                meuToast("Ok posi= $posi", applicationContext)
                val sentencaDAO = SentencaDAO(applicationContext, true)
                sentencaDAO.excluir(adapter.itens[posi])
                adapter.remove(posi)
            }
            -2 -> {}
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (menuSlidingPaneLayout.isOpen) {
            menuSlidingPaneLayout.closePane()
        } else {
            super.onBackPressed()
        }
    }

    override fun onStart() {
        super.onStart()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
        textoParaVoz.interromperFalaIA()
        if (emTeste) {
            meuToastLong("Versão de teste, Firebase desativado", applicationContext)
            return
        }
        try {
            val uploadDadosAutorizado = Preferencias.getPrefBool(PREF_UPLOAD_DADOS_AUTORIZADO, this, false)
            if (!uploadDadosAutorizado) return
            val idUltimoUpload = Preferencias.getPrefLong(PREF_ID_ULTIMO_UPLOAD, this)
            if (idUltimoUpload < idUltimoHistoricoGravado) {
                val intentService = Intent(this, GravaHistoricoService::class.java)
                intentService.putExtra(PREF_ID_ULTIMO_UPLOAD, idUltimoUpload)
                startService(intentService)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> { // microfone
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    meuToast("Permissão de microfone concedida", applicationContext)
                } else {
                    meuToast("Permissão de microfone negada", applicationContext)
                }
            }
            2 -> { // armazenamento
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    meuToast("Permissão de armazenamento concedida", applicationContext)
                } else {
                    meuToast("Permissão de armazenamento negada", applicationContext)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        processadora.destruir()
    }

    companion object {
        lateinit var adapter: AdapterMaisDeUmItem

        const val PREF_ID_ULTIMO_UPLOAD = "idUltimoUpload"
        const val PREF_UPLOAD_DADOS_AUTORIZADO = "upload_dados_autorizado"

        private fun atualizarRecycler(sentencas: List<Sentenca>, context: Activity) {
            adapter.addAll(sentencas)
            val inicia = Inicia(context)
            inicia.despertar()
        }

        @Suppress("DEPRECATION")
        private fun carregaSentencaBanco(context: Activity) {
            object : AsyncTask<Void?, Void?, List<Sentenca>>() {
                override fun doInBackground(vararg params: Void?): List<Sentenca> {
                    val sentencaDAO = SentencaDAO(context, true)
                    return sentencaDAO.listar()
                }

                override fun onPostExecute(sentencas: List<Sentenca>) {
                    super.onPostExecute(sentencas)
                    atualizarRecycler(sentencas, context)
                }
            }.execute()
        }
    }
}
