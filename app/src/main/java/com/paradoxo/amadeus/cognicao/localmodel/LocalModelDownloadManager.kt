package com.paradoxo.amadeus.cognicao.localmodel

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.entities.LocalModelEntity
import com.paradoxo.amadeus.dao.room.entities.ModelStatus
import com.paradoxo.amadeus.util.Preferencias
import java.io.File

data class DownloadProgress(val downloadedBytes: Long, val totalBytes: Long, val dmStatus: Int)

class LocalModelDownloadManager(private val context: Context) {

    private val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val dao = AmadeusDatabase.getInstance(context).localModelDAO()

    suspend fun enqueueDownload(descriptor: SupportedModels.ModelDescriptor): Long {
        val dest = modelFile(descriptor.modelId)
        dest.parentFile?.mkdirs()
        val request = DownloadManager.Request(Uri.parse(descriptor.downloadUrl))
            .setTitle(descriptor.displayName)
            .setDescription("Baixando modelo local...")
            .setDestinationUri(Uri.fromFile(dest))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        if (descriptor.requiresHfToken) {
            val token = Preferencias.getPrefString(Preferencias.PREF_HF_TOKEN, context)
            if (token.isNotBlank()) {
                request.addRequestHeader("Authorization", "Bearer $token")
            }
        }

        val dmId = dm.enqueue(request)
        dao.updateProgress(descriptor.modelId, 0L, dmId)
        dao.updateStatusOnly(descriptor.modelId, ModelStatus.DOWNLOADING)
        return dmId
    }

    suspend fun checkProgress(modelId: String, dmId: Long): DownloadProgress {
        val cursor = dm.query(DownloadManager.Query().setFilterById(dmId))
        cursor.use {
            if (!it.moveToFirst()) return DownloadProgress(0L, 0L, DownloadManager.STATUS_FAILED)
            val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            dao.updateProgress(modelId, downloaded, dmId)
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL ->
                    dao.updateStatus(modelId, ModelStatus.DOWNLOADED, modelFile(modelId).absolutePath)
                DownloadManager.STATUS_FAILED ->
                    dao.updateStatus(modelId, ModelStatus.ERROR, null)
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
