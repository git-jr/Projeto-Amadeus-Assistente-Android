package com.paradoxo.amadeus.ui.amadeuspro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paradoxo.amadeus.auth.AuthManager
import com.paradoxo.amadeus.payments.EntitlementManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AmadeusProViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager.getInstance(application)
    private val entitlementManager = EntitlementManager.getInstance(application)

    private val _uiState = MutableStateFlow(AmadeusProUiState())
    val uiState: StateFlow<AmadeusProUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authManager.state.collect { authState ->
                _uiState.update { it.copy(authState = authState) }
            }
        }

        viewModelScope.launch {
            entitlementManager.state.collect { entitlementState ->
                _uiState.update { it.copy(entitlementState = entitlementState) }
            }
        }

        viewModelScope.launch {
            entitlementManager.refreshEntitlements()
            val price = entitlementManager.fetchPremiumPriceLabel()
            _uiState.update { it.copy(priceLabel = price) }
        }
    }

    fun subscribe(activity: android.app.Activity) {
        entitlementManager.purchasePremium(activity)
    }
}
