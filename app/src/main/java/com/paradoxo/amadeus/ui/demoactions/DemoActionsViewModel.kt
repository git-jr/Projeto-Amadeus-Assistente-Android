package com.paradoxo.amadeus.ui.demoactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.service.AmadeusAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class DemoScenario(
    val id: String,
    val title: String,
    val description: String,
    val actions: List<ActionIntent>,
    val isReadScreen: Boolean = false,
)

internal class DemoActionsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DemoActionsUiState())
    val uiState: StateFlow<DemoActionsUiState> = _uiState.asStateFlow()

    init {
        refreshServiceStatus()
    }

    fun refreshServiceStatus() {
        val context = getApplication<Application>()
        _uiState.update { it.copy(serviceActive = AmadeusAccessibilityService.isActive(context)) }
    }

    fun runDemo(demo: DemoScenario) {
        if (_uiState.value.runningId != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(runningId = demo.id, statusText = "", readResult = "") }

            // 3s countdown
            for (i in 3 downTo 1) {
                _uiState.update { it.copy(countdown = i) }
                delay(1000)
            }
            _uiState.update { it.copy(countdown = 0) }

            if (demo.isReadScreen) {
                val elements = AmadeusAccessibilityService.getVisibleElements()

                val sorted = elements.sortedByDescending { e ->
                    when {
                        !e.text.isNullOrBlank() -> 3
                        !e.contentDescription.isNullOrBlank() -> 2
                        !e.labelLegivel.isNullOrBlank() -> 1
                        else -> 0
                    }
                }

                val preview = sorted.take(5).joinToString("\n") { e ->
                    "• ${e.text?.take(40) ?: e.contentDescription?.take(40) ?: e.labelLegivel.take(40) ?: "(sem texto)"}"
                }
                _uiState.update { it.copy(readResult = "${elements.size} elementos encontrados:\n$preview") }
                delay(4000)
                _uiState.update { it.copy(runningId = null, statusText = "Concluído") }
                return@launch
            }

            val sent = AmadeusAccessibilityService.executeActions(
                demo.actions,
                onFeedback = { msg -> _uiState.update { it.copy(statusText = msg) } },
                onDone = {
                    _uiState.update { it.copy(statusText = "Concluído") }
                    viewModelScope.launch {
                        delay(2000)
                        _uiState.update { it.copy(runningId = null) }
                    }
                }
            )
            if (!sent) {
                _uiState.update { it.copy(statusText = "Serviço de acessibilidade não está ativo", runningId = null) }
                delay(2000)
            }
        }
    }
}
