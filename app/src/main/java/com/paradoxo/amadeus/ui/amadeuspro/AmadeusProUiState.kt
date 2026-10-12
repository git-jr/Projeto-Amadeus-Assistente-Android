package com.paradoxo.amadeus.ui.amadeuspro

import com.paradoxo.amadeus.auth.AuthUiState
import com.paradoxo.amadeus.payments.EntitlementUiState

data class AmadeusProUiState(
    val authState: AuthUiState = AuthUiState(),
    val entitlementState: EntitlementUiState = EntitlementUiState(),
    val priceLabel: String? = null
)
