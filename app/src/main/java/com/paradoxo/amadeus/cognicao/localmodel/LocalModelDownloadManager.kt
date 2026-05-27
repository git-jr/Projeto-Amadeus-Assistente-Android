package com.paradoxo.amadeus.cognicao.localmodel

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.util.Log
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.entities.LocalModelEntity
import com.paradoxo.amadeus.dao.room.entities.ModelStatus
import com.paradoxo.amadeus.util.Preferencias
import java.io.File

private const val TAG = "ModelDownload"

data class DownloadProgress(val downloadedBytes: Long, val totalBytes: Long, val dmStatus: Int)

class LocalModelDownloadManager(private val context: Context) {

    private val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val dao = AmadeusDatabase.getInstance(context).localModelDAO()

    suspend fun enqueueDownload(descriptor: SupportedModels.ModelDescriptor): Long {
        val dest = modelFile(descriptor.modelId)
        dest.parentFile?.mkdirs()
        Log.d(TAG, "enqueue: modelId=${descriptor.modelId}")
        Log.d(TAG, "enqueue: url=${descriptor.downloadUrl}")
        Log.d(TAG, "enqueue: dest=${dest.absolutePath}")
        Log.d(TAG, "enqueue: requiresHfToken=${descriptor.requiresHfToken}")

        val request = DownloadManager.Request(Uri.parse(descriptor.downloadUrl))
            .setTitle(descriptor.displayName)
            .setDescription("Baixando modelo local...")
            .setDestinationUri(Uri.fromFile(dest))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        if (descriptor.requiresHfToken) {
            val token = Preferencias.getPrefString(Preferencias.PREF_HF_TOKEN, context)
            Log.d(TAG, "enqueue: token salvo length=${token.length}, blank=${token.isBlank()}")
            if (token.isNotBlank()) {
                request.addRequestHeader("Authorization", "Bearer $token")
                Log.d(TAG, "enqueue: Authorization header adicionado (Bearer hf_***${token.takeLast(4)})")
            } else {
                Log.w(TAG, "enqueue: requiresHfToken=true mas token está vazio — download provavelmente falhará com 401")
            }
        }

        val dmId = dm.enqueue(request)
        Log.d(TAG, "enqueue: DownloadManager dmId=$dmId")
        dao.updateProgress(descriptor.modelId, 0L, dmId)
        dao.updateStatusOnly(descriptor.modelId, ModelStatus.DOWNLOADING)
        return dmId
    }

    suspend fun checkProgress(modelId: String, dmId: Long): DownloadProgress {
        val cursor = dm.query(DownloadManager.Query().setFilterById(dmId))
        cursor.use {
            if (!it.moveToFirst()) {
                Log.w(TAG, "checkProgress[$modelId]: cursor vazio — dmId=$dmId não encontrado no DownloadManager")
                return DownloadProgress(0L, 0L, DownloadManager.STATUS_FAILED)
            }
            val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            val uri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_URI))

            val statusName = when (status) {
                DownloadManager.STATUS_PENDING    -> "PENDING"
                DownloadManager.STATUS_RUNNING    -> "RUNNING"
                DownloadManager.STATUS_PAUSED     -> "PAUSED"
                DownloadManager.STATUS_SUCCESSFUL -> "SUCCESSFUL"
                DownloadManager.STATUS_FAILED     -> "FAILED"
                else -> "UNKNOWN($status)"
            }
            Log.d(TAG, "checkProgress[$modelId]: status=$statusName downloaded=$downloaded total=$total")

            dao.updateProgress(modelId, downloaded, dmId)
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val path = modelFile(modelId).absolutePath
                    Log.i(TAG, "checkProgress[$modelId]: DOWNLOADED → $path")
                    dao.updateStatus(modelId, ModelStatus.DOWNLOADED, path)
                }
                DownloadManager.STATUS_FAILED -> {
                    val reasonName = when (reason) {
                        DownloadManager.ERROR_CANNOT_RESUME         -> "ERROR_CANNOT_RESUME"
                        DownloadManager.ERROR_DEVICE_NOT_FOUND      -> "ERROR_DEVICE_NOT_FOUND"
                        DownloadManager.ERROR_FILE_ALREADY_EXISTS   -> "ERROR_FILE_ALREADY_EXISTS"
                        DownloadManager.ERROR_FILE_ERROR            -> "ERROR_FILE_ERROR"
                        DownloadManager.ERROR_HTTP_DATA_ERROR       -> "ERROR_HTTP_DATA_ERROR"
                        DownloadManager.ERROR_INSUFFICIENT_SPACE    -> "ERROR_INSUFFICIENT_SPACE"
                        DownloadManager.ERROR_TOO_MANY_REDIRECTS    -> "ERROR_TOO_MANY_REDIRECTS"
                        DownloadManager.ERROR_UNHANDLED_HTTP_CODE   -> "ERROR_UNHANDLED_HTTP_CODE"
                        DownloadManager.ERROR_UNKNOWN               -> "ERROR_UNKNOWN"
                        400 -> "HTTP_400_BAD_REQUEST"
                        401 -> "HTTP_401_UNAUTHORIZED (token ausente ou inválido)"
                        403 -> "HTTP_403_FORBIDDEN (sem acesso ao repo — aceite licença no HuggingFace)"
                        404 -> "HTTP_404_NOT_FOUND (URL incorreta)"
                        else -> "reason=$reason"
                    }
                    Log.e(TAG, "checkProgress[$modelId]: FAILED reason=$reasonName uri=$uri")
                    dao.updateStatus(modelId, ModelStatus.ERROR, null)
                }
                DownloadManager.STATUS_PAUSED -> {
                    val pauseReason = when (reason) {
                        DownloadManager.PAUSED_QUEUED_FOR_WIFI      -> "PAUSED_QUEUED_FOR_WIFI (roaming desativado e sem WiFi)"
                        DownloadManager.PAUSED_WAITING_FOR_NETWORK  -> "PAUSED_WAITING_FOR_NETWORK"
                        DownloadManager.PAUSED_WAITING_TO_RETRY     -> "PAUSED_WAITING_TO_RETRY"
                        else -> "reason=$reason"
                    }
                    Log.w(TAG, "checkProgress[$modelId]: PAUSED $pauseReason")
                }
            }
            return DownloadProgress(downloaded, total, status)
        }
    }

    suspend fun cancelDownload(modelId: String, dmId: Long) {
        dm.remove(dmId)
        dao.updateStatus(modelId, ModelStatus.NOT_DOWNLOADED, null)
        dao.updateProgress(modelId, 0L, -1L)
    }

    suspend fun deleteModel(entity: LocalModelEntity) {
        entity.filePath?.let { File(it).delete() }
        dao.updateStatus(entity.modelId, ModelStatus.NOT_DOWNLOADED, null)
        dao.updateProgress(entity.modelId, 0L, -1L)
        dao.deactivate(entity.modelId)
    }

    fun modelFile(modelId: String): File {
        // DownloadManager roda como processo separado — só acessa external storage.
        // getExternalFilesDir é privado ao app (não requer permissão em Android 10+).
        val base = context.getExternalFilesDir(null)
            ?: context.filesDir  // fallback: nunca deve acontecer em devices normais
        return File(base, "litert_models/$modelId.litertlm")
    }
}
