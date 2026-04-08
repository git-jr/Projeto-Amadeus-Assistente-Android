package com.paradoxo.amadeus.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Escuta continuamente em background pelo microfone e dispara [onWakeWordDetected]
 * quando a palavra "amadeus" for reconhecida pelo modelo Vosk local.
 *
 * Uso:
 * 1. Coloque o zip do modelo em app/src/main/assets/ com o nome [MODEL_ASSET].zip
 *    (baixe em: https://alphacephei.com/vosk/models — use vosk-model-small-pt-0.3)
 * 2. Chame [initialize] uma vez. O modelo é descompactado na primeira execução.
 * 3. Chame [startDetection] / [stopDetection] para controlar o loop de escuta.
 * 4. Chame [release] ao destruir o serviço.
 */
class WakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {
    private var model: Model? = null
    private var detectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Descompacta o modelo (apenas na 1ª execução) e inicia a detecção. */
    fun initialize() {
        scope.launch {
            try {
                val modelDir = File(context.filesDir, MODEL_DIR)
                if (!isModelReady(modelDir)) {
                    Log.d(TAG, "Descompactando modelo Vosk...")
                    extractFromAssets(modelDir)
                }
                val loadedModel = Model(modelDir.absolutePath)
                withContext(Dispatchers.Main) {
                    model = loadedModel
                    startDetection()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao carregar modelo Vosk: ${e.message}")
            }
        }
    }

    /** Verifica se o modelo já foi extraído e está íntegro. */
    private fun isModelReady(modelDir: File): Boolean {
        if (!modelDir.exists()) return false
        // Arquivo obrigatório presente em todos os modelos Vosk
        return File(modelDir, "am/final.mdl").exists()
    }

    /**
     * Extrai o zip do modelo de assets para filesDir.
     * O zip pode ter um diretório raiz (ex: vosk-model-small-pt-0.3/) — ele é ignorado,
     * e o conteúdo é extraído direto em [targetDir].
     */
    private fun extractFromAssets(targetDir: File) {
        targetDir.mkdirs()
        context.assets.open("$MODEL_ASSET.zip").use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    // Remove o diretório raiz do zip (ex: "vosk-model-small-pt-0.3/conf/...")
                    // tornando o caminho relativo ao targetDir
                    val relativePath = entry.name.substringAfter('/')
                    if (relativePath.isNotEmpty()) {
                        val dest = File(targetDir, relativePath)
                        if (entry.isDirectory) {
                            dest.mkdirs()
                        } else {
                            dest.parentFile?.mkdirs()
                            dest.outputStream().use { out -> zip.copyTo(out) }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        Log.d(TAG, "Modelo extraído para: ${targetDir.absolutePath}")
    }

    /** Inicia o loop de escuta. Não faz nada se o modelo ainda não carregou. */
    fun startDetection() {
        if (model == null || detectionJob?.isActive == true) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) return

        detectionJob = scope.launch {
            val recognizer = Recognizer(model, SAMPLE_RATE.toFloat()).apply {
                setGrammar("""["$WAKE_WORD", "[unk]"]""")
            }
            val minBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, 4096) * 2
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord não inicializou")
                recognizer.close()
                return@launch
            }
            recorder.startRecording()
            val buffer = ShortArray(maxOf(minBuffer, 4096))
            try {
                while (isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0 && recognizer.acceptWaveForm(buffer, read)) {
                        val text = try {
                            JSONObject(recognizer.result).optString("text")
                        } catch (_: JSONException) {
                            ""
                        }
                        Log.d(TAG, "Palavra detectada: \"$text\"")
                        if (text.contains(WAKE_WORD, ignoreCase = true)) {
                            Log.d(TAG, "Wake word detectada : \"$text\"")
                            withContext(Dispatchers.Main) { onWakeWordDetected() }
                            break // para após detectar; startDetection() é chamado novamente depois
                        }
                    }
                }
            } finally {
                recorder.stop()
                recorder.release()
                recognizer.close()
            }
        }
    }

    /** Para o loop de escuta sem liberar o modelo. */
    fun stopDetection() {
        detectionJob?.cancel()
        detectionJob = null
    }

    /** Para tudo e libera o modelo. Chamar no onDestroy do serviço. */
    fun release() {
        stopDetection()
        scope.cancel()
        model?.close()
        model = null
    }

    companion object {
        private const val TAG = "WakeWordManager"
        private const val SAMPLE_RATE = 16000
        const val WAKE_WORD = "amadeus"

        // Nome do zip em app/src/main/assets/ (sem extensão .zip)
        // Baixe vosk-model-small-pt-0.3.zip e renomeie para vosk-model-small-pt.zip
        private const val MODEL_ASSET = "vosk-model-small-pt"
        private const val MODEL_DIR = "vosk-model-small-pt"
    }
}
