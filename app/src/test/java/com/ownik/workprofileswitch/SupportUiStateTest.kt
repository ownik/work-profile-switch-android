package com.ownik.workprofileswitch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportUiStateTest {
    private val ready = SupportUiState(purchasesReady = true)

    @Test
    fun purchasesRequireSuccessfulOwnedPurchaseCheck() {
        assertFalse(SupportUiState().canPurchase)
        assertTrue(ready.canPurchase)
    }

    @Test
    fun blocksNewPaymentsWhilePurchasingOrConsuming() {
        assertFalse(ready.copy(purchasing = true).canPurchase)
        assertFalse(ready.copy(processing = true).canPurchase)
    }

    @Test
    fun pendingOrUnprocessedPaymentBlocksAnotherPayment() {
        assertFalse(ready.copy(pending = true).canPurchase)
        assertFalse(ready.copy(error = SupportError.PROCESSING).canPurchase)
    }

    @Test
    fun productRefreshBlocksPaymentUntilPricesArrive() {
        assertFalse(ready.copy(loading = true).canPurchase)
    }
}
