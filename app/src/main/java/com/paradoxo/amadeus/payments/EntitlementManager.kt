package com.paradoxo.amadeus.payments

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.Purchase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.paradoxo.amadeus.auth.AuthManager
import com.paradoxo.amadeus.util.Preferencias
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class EntitlementManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val authManager = AuthManager.getInstance(context)
    private val database = FirebaseDatabase.getInstance().reference
    private val billingRepository = BillingRepository(
        context = context,
        onPurchasesUpdated = { purchases -> scope.launch { processPurchases(purchases) } },
        onBillingError = { message ->
            _state.update { it.copy(purchasing = false, errorMessage = message) }
        }
    )

    private val _state = MutableStateFlow(
        EntitlementUiState(entitlement = readCachedEntitlement(), loading = true)
    )
    val state: StateFlow<EntitlementUiState> = _state

    fun refreshEntitlements() {
        scope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }
            val backendEntitlement = runCatching { fetchBackendEntitlement() }.getOrNull()
            val playPurchases = runCatching {
                withContext(Dispatchers.IO) { billingRepository.queryActiveSubscriptionPurchases() }
            }.getOrElse {
                _state.update { current -> current.copy(errorMessage = it.message) }
                emptyList()
            }

            if (playPurchases.isNotEmpty()) {
                processPurchases(playPurchases)
                return@launch
            }

            val entitlement = backendEntitlement ?: Entitlement()
            cacheEntitlement(entitlement)
            _state.update {
                it.copy(entitlement = entitlement, loading = false, purchasing = false)
            }
        }
    }

    fun purchasePremium(activity: Activity) {
        scope.launch {
            _state.update { it.copy(purchasing = true, errorMessage = null) }
            val hasGoogleAccount = authManager.requireGoogleAccountForPurchase(activity)
            if (!hasGoogleAccount) {
                _state.update {
                    it.copy(
                        purchasing = false,
                        errorMessage = "Entre com sua Conta Google para assinar."
                    )
                }
                return@launch
            }

            val launched = withContext(Dispatchers.IO) {
                billingRepository.launchPremiumPurchase(activity)
            }
            if (!launched) {
                _state.update {
                    it.copy(
                        purchasing = false,
                        errorMessage = "Assinatura indisponível no Google Play."
                    )
                }
            }
        }
    }

    private suspend fun processPurchases(purchases: List<Purchase>) {
        val activePurchase = purchases.firstOrNull {
            it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                BillingRepository.PRODUCT_PREMIUM_MONTHLY in it.products
        }
        val pendingPurchase = purchases.firstOrNull {
            it.purchaseState == Purchase.PurchaseState.PENDING &&
                BillingRepository.PRODUCT_PREMIUM_MONTHLY in it.products
        }

        when {
            activePurchase != null -> {
                withContext(Dispatchers.IO) {
                    runCatching { billingRepository.acknowledgeIfNeeded(activePurchase) }
                    runCatching { submitGooglePlayPurchaseForVerification(activePurchase) }
                }
                val entitlement = Entitlement(
                    planId = BillingRepository.PRODUCT_PREMIUM_MONTHLY,
                    features = setOf(EntitlementFeature.ADVANCED_MODELS),
                    source = EntitlementSource.GOOGLE_PLAY,
                    status = EntitlementStatus.ACTIVE
                )
                cacheEntitlement(entitlement)
                _state.update {
                    it.copy(entitlement = entitlement, loading = false, purchasing = false)
                }
            }

            pendingPurchase != null -> {
                val entitlement = Entitlement(
                    planId = BillingRepository.PRODUCT_PREMIUM_MONTHLY,
                    features = emptySet(),
                    source = EntitlementSource.GOOGLE_PLAY,
                    status = EntitlementStatus.PENDING
                )
                cacheEntitlement(entitlement)
                _state.update {
                    it.copy(entitlement = entitlement, loading = false, purchasing = false)
                }
            }

            else -> {
                cacheEntitlement(Entitlement())
                _state.update {
                    it.copy(entitlement = Entitlement(), loading = false, purchasing = false)
                }
            }
        }
    }

    private suspend fun fetchBackendEntitlement(): Entitlement? {
        val uid = ensureUid()
        val snapshot = database.child("entitlements").child(uid).get().await()
        if (!snapshot.exists()) return null
        return snapshot.toEntitlement()
    }

    private suspend fun submitGooglePlayPurchaseForVerification(purchase: Purchase) {
        val uid = ensureUid()
        val payload = mapOf(
            "uid" to uid,
            "source" to EntitlementSource.GOOGLE_PLAY.name,
            "productId" to BillingRepository.PRODUCT_PREMIUM_MONTHLY,
            "purchaseToken" to purchase.purchaseToken,
            "packageName" to context.packageName,
            "submittedAt" to System.currentTimeMillis()
        )
        database.child("pending_purchase_verifications")
            .child(uid)
            .child(purchase.orderId ?: purchase.purchaseToken.hashCode().toString())
            .setValue(payload)
            .await()
    }

    private suspend fun ensureUid(): String {
        return authManager.ensureAnonymousUser().uid
    }

    private fun readCachedEntitlement(): Entitlement {
        val status = runCatching {
            EntitlementStatus.valueOf(Preferencias.getPrefString(PREF_STATUS, context))
        }.getOrDefault(EntitlementStatus.INACTIVE)
        val source = runCatching {
            EntitlementSource.valueOf(Preferencias.getPrefString(PREF_SOURCE, context))
        }.getOrDefault(EntitlementSource.UNKNOWN)
        val features = Preferencias.getPrefString(PREF_FEATURES, context)
            .split(',')
            .mapNotNull { key ->
                EntitlementFeature.entries.firstOrNull { it.key == key }
            }
            .toSet()
        val expiresAt = Preferencias.getPrefLong(PREF_EXPIRES_AT, context)
            .takeIf { it > 0L }
        return Entitlement(
            planId = Preferencias.getPrefString(PREF_PLAN_ID, context),
            features = features,
            source = source,
            status = status,
            expiresAtMillis = expiresAt
        )
    }

    private fun cacheEntitlement(entitlement: Entitlement) {
        Preferencias.setPrefString(entitlement.planId, PREF_PLAN_ID, context)
        Preferencias.setPrefString(entitlement.status.name, PREF_STATUS, context)
        Preferencias.setPrefString(entitlement.source.name, PREF_SOURCE, context)
        Preferencias.setPrefString(
            entitlement.features.joinToString(",") { it.key },
            PREF_FEATURES,
            context
        )
        Preferencias.setPrefLong(PREF_EXPIRES_AT, entitlement.expiresAtMillis ?: 0L, context)
    }

    fun close() {
        billingRepository.close()
    }

    companion object {
        private const val PREF_PLAN_ID = "premium_plan_id_cached"
        private const val PREF_STATUS = "premium_status_cached"
        private const val PREF_SOURCE = "premium_source_cached"
        private const val PREF_FEATURES = "premium_features_cached"
        private const val PREF_EXPIRES_AT = "premium_expires_at_cached"

        @Volatile private var instance: EntitlementManager? = null

        fun getInstance(context: Context): EntitlementManager =
            instance ?: synchronized(this) {
                instance ?: EntitlementManager(context.applicationContext).also { instance = it }
            }
    }
}

private fun DataSnapshot.toEntitlement(): Entitlement {
    val planId = child("planId").getValue(String::class.java).orEmpty()
    val source = runCatching {
        EntitlementSource.valueOf(child("source").getValue(String::class.java).orEmpty())
    }.getOrDefault(EntitlementSource.UNKNOWN)
    val status = runCatching {
        EntitlementStatus.valueOf(child("status").getValue(String::class.java).orEmpty())
    }.getOrDefault(EntitlementStatus.INACTIVE)
    val expiresAt = child("expiresAtMillis").getValue(Long::class.java)
    val features = child("features").children.mapNotNull { item ->
        val key = item.getValue(String::class.java) ?: item.key
        EntitlementFeature.entries.firstOrNull { it.key == key }
    }.toSet()

    return Entitlement(
        planId = planId,
        features = features,
        source = source,
        status = status,
        expiresAtMillis = expiresAt
    )
}
