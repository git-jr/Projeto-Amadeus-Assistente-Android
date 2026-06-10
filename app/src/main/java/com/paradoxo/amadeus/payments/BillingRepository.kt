package com.paradoxo.amadeus.payments

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BillingRepository(
    context: Context,
    private val onPurchasesUpdated: (List<Purchase>) -> Unit,
    private val onBillingError: (String) -> Unit
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private var cachedProductDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> onPurchasesUpdated(purchases.orEmpty())
            BillingResponseCode.USER_CANCELED -> Unit
            else -> onBillingError(billingResult.debugMessage.ifBlank { "Falha na compra" })
        }
    }

    suspend fun connectIfNeeded(): Boolean {
        if (billingClient.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingServiceDisconnected() {
                    if (continuation.isActive) continuation.resume(false)
                }

                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    continuation.resume(billingResult.responseCode == BillingResponseCode.OK)
                }
            })
        }
    }

    suspend fun queryPremiumProduct(): ProductDetails? {
        if (!connectIfNeeded()) return null
        cachedProductDetails?.let { return it }

        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_PREMIUM_MONTHLY)
            .setProductType(ProductType.SUBS)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        val details = suspendCancellableCoroutine { continuation ->
            billingClient.queryProductDetailsAsync(
                params,
                ProductDetailsResponseListener { billingResult, productDetailsResult ->
                    val product = if (billingResult.responseCode == BillingResponseCode.OK) {
                        productDetailsResult.productDetailsList.firstOrNull()
                    } else {
                        null
                    }
                    continuation.resume(product)
                }
            )
        }
        cachedProductDetails = details
        return details
    }

    suspend fun queryActiveSubscriptionPurchases(): List<Purchase> {
        if (!connectIfNeeded()) return emptyList()
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.SUBS)
            .build()
        return suspendCancellableCoroutine { continuation ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                val activePurchases = if (billingResult.responseCode == BillingResponseCode.OK) {
                    purchases.filter { purchase ->
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            PRODUCT_PREMIUM_MONTHLY in purchase.products
                    }
                } else {
                    emptyList()
                }
                continuation.resume(activePurchases)
            }
        }
    }

    suspend fun launchPremiumPurchase(activity: Activity): Boolean {
        val productDetails = queryPremiumProduct() ?: return false
        val offerToken = productDetails.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?: return false

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        return billingClient.launchBillingFlow(activity, params).responseCode == BillingResponseCode.OK
    }

    suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged || purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!connectIfNeeded()) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        suspendCancellableCoroutine { continuation ->
            billingClient.acknowledgePurchase(params) {
                continuation.resume(Unit)
            }
        }
    }

    fun close() {
        if (billingClient.isReady) billingClient.endConnection()
    }

    companion object {
        const val PRODUCT_PREMIUM_MONTHLY = "amadeus_premium_monthly"
    }
}
