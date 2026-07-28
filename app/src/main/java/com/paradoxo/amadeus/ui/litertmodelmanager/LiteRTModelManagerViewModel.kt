package com.paradoxo.amadeus.ui.litertmodelmanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paradoxo.amadeus.cognicao.localmodel.LocalModelDownloadManager
import com.paradoxo.amadeus.dao.room.AmadeusDatabase
import com.paradoxo.amadeus.dao.room.entities.LocalModelEntity
import com.paradoxo.amadeus.dao.room.entities.ModelStatus
import com.paradoxo.amadeus.util.Preferencias
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class LiteRTModelManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AmadeusDatabase.getInstance(application)
    private val dao = db.localModelDAO()
    private val downloadManager = LocalModelDownloadManager(application)

    private val _uiState = MutableStateFlow(LiteRTModelManagerUiState())
    val uiState: StateFlow<LiteRTModelManagerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dao.getAll()
                .collect { models ->
                    _uiState.update { it.copy(models = models) }
                }
        }

        _uiState.update { it.copy(hfToken = Preferencias.getPrefString(Preferencias.PREF_HF_TOKEN, application)) }

        startProgressPolling()
    }

    private fun startProgressPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val downloading = _uiState.value.models.filter { it.status == ModelStatus.DOWNLOADING }
                for (model in downloading) {
                    if (model.downloadManagerId >= 0) {
                        downloadManager.checkProgress(model.modelId, model.downloadManagerId)
                    }
                }
                delay(2_000)
            }
        }
    }

    fun downloadModel(descriptor: com.paradoxo.amadeus.cognicao.localmodel.SupportedModels.ModelDescriptor) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadManager.enqueueDownload(descriptor)
        }
    }

    fun cancelDownload(model: LocalModelEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadManager.cancelDownload(model.modelId, model.downloadManagerId)
        }
    }

    fun setActive(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setActive(modelId)
        }
    }

    fun deleteModel(entity: LocalModelEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadManager.deleteModel(entity)
        }
    }

    fun updateHfToken(token: String) {
        _uiState.update { it.copy(hfToken = token) }
        Preferencias.setPrefString(token, Preferencias.PREF_HF_TOKEN, getApplication())
    }
}
