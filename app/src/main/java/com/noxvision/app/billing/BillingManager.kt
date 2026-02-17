package com.noxvision.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.noxvision.app.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingManager(
    private val context: Context,
    private val onPurchaseConsumed: (String) -> Unit // Callback when a consumable is bought and consumed
) : PurchasesUpdatedListener {

    private val _billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails = _productDetails.asStateFlow()

    // Consumable product IDs
    private val productIds = listOf(
        "credits_50",
        "credits_120",
        "credits_250",
        "credits_700",
        "credits_1500"
    )

    fun startConnection() {
        _billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    AppLogger.log("Billing setup finished", AppLogger.LogType.INFO)
                    queryProductDetails()
                } else {
                    AppLogger.log("Billing setup failed: ${billingResult.debugMessage}", AppLogger.LogType.ERROR)
                }
            }

            override fun onBillingServiceDisconnected() {
                AppLogger.log("Billing service disconnected", AppLogger.LogType.ERROR)
                // TODO: Retry connection logic
            }
        })
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()

        _billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = productDetailsList
            } else {
                AppLogger.log("Error querying products: ${billingResult.debugMessage}", AppLogger.LogType.ERROR)
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            )
            .build()
        _billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            AppLogger.log("User canceled purchase", AppLogger.LogType.INFO)
        } else {
            AppLogger.log("Purchase failed: ${billingResult.debugMessage}", AppLogger.LogType.ERROR)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Confirm purchase (Consume it since it's credits)
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            CoroutineScope(Dispatchers.IO).launch {
                val result = _billingClient.consumePurchase(consumeParams)
                if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    withContext(Dispatchers.Main) {
                        // Extract amount from product ID for simplicity in this demo
                        // Real implementation might map ID to amount safely
                        val productId = purchase.products.firstOrNull() ?: ""
                        onPurchaseConsumed(productId)
                    }
                }
            }
        }
    }
}
