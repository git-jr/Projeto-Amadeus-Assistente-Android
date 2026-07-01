package com.paradoxo.amadeus.payments

enum class EntitlementFeature(val key: String) {
    ADVANCED_MODELS("ADVANCED_MODELS")
}

enum class EntitlementSource {
    GOOGLE_PLAY,
    STRIPE,
    MANUAL,
    UNKNOWN
}

enum class EntitlementStatus {
    ACTIVE,
    EXPIRED,
    CANCELED,
    PENDING,
    INACTIVE
}

data class Entitlement(
    val planId: String = "",
    val features: Set<EntitlementFeature> = emptySet(),
    val source: EntitlementSource = EntitlementSource.UNKNOWN,
    val status: EntitlementStatus = EntitlementStatus.INACTIVE,
    val expiresAtMillis: Long? = null
) {
    fun hasFeature(feature: EntitlementFeature): Boolean {
        val notExpired = expiresAtMillis == null || expiresAtMillis > System.currentTimeMillis()
        return status == EntitlementStatus.ACTIVE && notExpired && feature in features
    }
}

data class EntitlementUiState(
    val entitlement: Entitlement = Entitlement(),
    val loading: Boolean = true,
    val purchasing: Boolean = false,
    val errorMessage: String? = null
) {
    val isPremium: Boolean
        get() = entitlement.status == EntitlementStatus.ACTIVE &&
                (entitlement.expiresAtMillis == null || entitlement.expiresAtMillis > System.currentTimeMillis())

    val canUseAdvancedModels: Boolean
        get() = entitlement.hasFeature(EntitlementFeature.ADVANCED_MODELS)
}
