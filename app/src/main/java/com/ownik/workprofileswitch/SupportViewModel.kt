package com.ownik.workprofileswitch

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.launch

internal enum class SupportTier(val productId: String) {
    SMALL("support_small"),
    MEDIUM("support_medium"),
    LARGE("support_large"),
}

internal data class SupportOption(val tier: SupportTier, val price: String)
internal enum class SupportError { UNAVAILABLE, PURCHASE, PROCESSING }

internal data class SupportUiState(
    val dialogVisible: Boolean = false,
    val automaticPrompt: Boolean = false,
    val loading: Boolean = false,
    val options: List<SupportOption> = emptyList(),
    val purchasesReady: Boolean = false,
    val purchasing: Boolean = false,
    val processing: Boolean = false,
    val pending: Boolean = false,
    val thankYou: Boolean = false,
    val error: SupportError? = null,
) {
    val busy: Boolean get() = purchasing || processing
    val canPurchase: Boolean
        get() = purchasesReady && !busy && !loading && !pending &&
            error != SupportError.PROCESSING
}

internal class SupportViewModel(application: Application) : AndroidViewModel(application) {
    var state by mutableStateOf(SupportUiState())
        private set

    private val preferences = SupportPreferences(application)
    private var promptState = preferences.read()
    private var connecting = false
    private var connectedOnce = false
    private var queryingPurchases = false
    private var queryingOptions = false
    private var closed = false
    private val consumingTokens = mutableSetOf<String>()
    private val completedTokens = mutableSetOf<String>()
    private val unfinishedTokens = mutableSetOf<String>()
    private val pendingTokens = mutableSetOf<String>()
    private var detailsById = emptyMap<String, ProductDetails>()

    private val client = BillingClient.newBuilder(application)
        .setListener { result, purchases ->
            viewModelScope.launch {
                state = state.copy(purchasing = false)
                when (result.responseCode) {
                    BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty())
                    BillingClient.BillingResponseCode.USER_CANCELED -> Unit
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> queryPurchases()
                    else -> state = state.copy(error = SupportError.PURCHASE)
                }
            }
        }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun refresh() {
        if (closed || connecting) return
        state = state.copy(error = null)
        if (client.isReady || connectedOnce) {
            queryPurchases()
            loadOptions()
            return
        }
        connecting = true
        state = state.copy(loading = true, error = null)
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                viewModelScope.launch {
                    connecting = false
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        connectedOnce = true
                        queryPurchases()
                        loadOptions()
                    } else {
                        state = state.copy(loading = false, error = SupportError.UNAVAILABLE)
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Subsequent API calls reconnect automatically.
            }
        })
    }

    fun openDialog() {
        state = state.copy(
            dialogVisible = true, automaticPrompt = false, thankYou = false, error = null
        )
        refresh()
    }

    fun maybeShowPrompt(setupComplete: Boolean) {
        if (state.dialogVisible || !state.purchasesReady || unfinishedTokens.isNotEmpty() ||
            state.loading || state.busy || state.pending || state.options.isEmpty() ||
            state.error != null ||
            !promptState.shouldShow(System.currentTimeMillis(), setupComplete)
        ) return
        // Persist immediately: rotation or dismissal must not repeat the prompt.
        savePrompt(promptState.copy(promptShown = true))
        state = state.copy(dialogVisible = true, automaticPrompt = true, thankYou = false)
    }

    fun dismissDialog() {
        if (!state.busy) state = state.copy(dialogVisible = false)
    }

    fun remindLater() {
        savePrompt(promptState.remindLater(System.currentTimeMillis()))
        dismissDialog()
    }

    fun neverAsk() {
        savePrompt(promptState.copy(neverAsk = true))
        dismissDialog()
    }

    fun purchase(activity: Activity, tier: SupportTier) {
        if (!state.canPurchase || unfinishedTokens.isNotEmpty()) return
        val details = detailsById[tier.productId]
        val offer = details?.oneTimePurchaseOfferDetailsList?.firstOrNull()
        if (details == null || offer == null) {
            state = state.copy(error = SupportError.UNAVAILABLE)
            return
        }
        state = state.copy(purchasing = true, error = null, thankYou = false)
        val productBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
        offer.offerToken?.let { productBuilder.setOfferToken(it) }
        val product = productBuilder.build()
        val result = client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(product)).build()
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            state = state.copy(purchasing = false)
            when (result.responseCode) {
                BillingClient.BillingResponseCode.USER_CANCELED -> Unit
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> queryPurchases()
                else -> state = state.copy(error = SupportError.PURCHASE)
            }
        }
    }

    private fun loadOptions() {
        if (queryingOptions) return
        queryingOptions = true
        detailsById = emptyMap()
        state = state.copy(loading = true, options = emptyList())
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            SupportTier.entries.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it.productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }
        ).build()
        client.queryProductDetailsAsync(params) { result, products ->
            viewModelScope.launch {
                queryingOptions = false
                detailsById = if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    products.productDetailsList.associateBy { it.productId }
                } else emptyMap()
                val options = SupportTier.entries.mapNotNull { tier ->
                    detailsById[tier.productId]?.oneTimePurchaseOfferDetailsList
                        ?.firstOrNull()?.let { SupportOption(tier, it.formattedPrice) }
                }
                state = state.copy(
                    loading = false,
                    options = options,
                    error = if (result.responseCode != BillingClient.BillingResponseCode.OK ||
                        options.isEmpty()
                    ) SupportError.UNAVAILABLE else state.error,
                )
            }
        }
    }

    private fun queryPurchases() {
        if (queryingPurchases) return
        queryingPurchases = true
        state = state.copy(purchasesReady = false)
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            viewModelScope.launch {
                queryingPurchases = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    state = state.copy(purchasesReady = true)
                    val supportPurchases = purchases.filter { purchase ->
                        purchase.products.any { id -> SupportTier.entries.any { it.productId == id } }
                    }
                    unfinishedTokens.retainAll(supportPurchases.map { it.purchaseToken }.toSet())
                    pendingTokens.retainAll(supportPurchases.filter {
                        it.purchaseState == Purchase.PurchaseState.PENDING
                    }.map { it.purchaseToken }.toSet())
                    processPurchases(supportPurchases)
                } else {
                    state = state.copy(error = SupportError.UNAVAILABLE)
                }
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        purchases.filter { purchase ->
            purchase.products.any { id -> SupportTier.entries.any { it.productId == id } }
        }.forEach { purchase ->
            if (purchase.purchaseToken in completedTokens) return@forEach
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PENDING -> {
                    unfinishedTokens.add(purchase.purchaseToken)
                    pendingTokens.add(purchase.purchaseToken)
                }
                Purchase.PurchaseState.PURCHASED -> {
                    unfinishedTokens.add(purchase.purchaseToken)
                    pendingTokens.remove(purchase.purchaseToken)
                    savePrompt(promptState.copy(hasSupported = true))
                    consume(purchase)
                }
            }
        }
        state = state.copy(pending = pendingTokens.isNotEmpty())
    }

    private fun consume(purchase: Purchase) {
        if (!consumingTokens.add(purchase.purchaseToken)) return
        state = state.copy(processing = true)
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        client.consumeAsync(params) { result, token ->
            viewModelScope.launch {
                consumingTokens.remove(token)
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    completedTokens.add(token)
                    unfinishedTokens.remove(token)
                    state = state.copy(
                        processing = consumingTokens.isNotEmpty(),
                        pending = pendingTokens.isNotEmpty(),
                        dialogVisible = true,
                        thankYou = true,
                        automaticPrompt = false,
                        error = null,
                    )
                } else {
                    // Play retains the unconsumed purchase. Retry on the next foreground/Retry.
                    state = state.copy(
                        processing = consumingTokens.isNotEmpty(),
                        error = SupportError.PROCESSING,
                    )
                }
            }
        }
    }

    private fun savePrompt(value: SupportPromptState) {
        promptState = value
        preferences.write(value)
    }

    override fun onCleared() {
        closed = true
        client.endConnection()
    }
}
