package com.piashcse.feature.notification

/** Centralized, testable notification copy. */
object NotificationTemplates {
    data class Content(
        val title: String,
        val body: String,
    )

    fun orderPlaced(
        orderNumber: String,
        total: String,
        trackingUrl: String? = null,
    ) = Content(
        "Order $orderNumber placed",
        "Your order total is $total. We will notify you when it ships." +
            (trackingUrl?.let { "\n\nTrack your order: $it" } ?: ""),
    )

    fun paymentCompleted(
        amount: String,
        orderId: String,
    ) = Content("Payment received", "We received $amount for order $orderId.")

    fun refundStatusChanged(
        refundId: String,
        from: String,
        to: String,
        reason: String? = null,
    ) = Content("Refund $to", "Refund $refundId changed from $from to $to." + (reason?.let { " Reason: $it" } ?: ""))

    fun rmaStatusChanged(
        rmaId: String,
        from: String,
        to: String,
        note: String? = null,
    ) = Content("Return/exchange $to", "Request $rmaId changed from $from to $to." + (note?.let { " Note: $it" } ?: ""))

    fun welcome(userType: String) = Content("Welcome", "Your account has been created as $userType.")

    fun orderStatusChanged(
        orderNumber: String,
        status: String,
        trackingNumber: String? = null,
        trackingUrl: String? = null,
    ): Content {
        val title =
            when (status) {
                "SHIPPED" -> "Order $orderNumber shipped"
                "DELIVERED" -> "Order $orderNumber delivered"
                "CANCELED" -> "Order $orderNumber canceled"
                else -> "Order $orderNumber $status"
            }
        val body =
            when (status) {
                "SHIPPED" ->
                    "Your order $orderNumber has shipped." +
                        (trackingNumber?.takeIf { it.isNotBlank() }?.let { " Tracking: $it." } ?: "")
                "DELIVERED" -> "Your order $orderNumber was delivered."
                "CANCELED" -> "Your order $orderNumber was canceled."
                else -> "Your order $orderNumber is now $status."
            }
        return Content(
            title,
            body + (trackingUrl?.let { "\n\nView your order: $it" } ?: ""),
        )
    }

    fun backInStock(
        productName: String,
        productUrl: String? = null,
    ) = Content(
        "$productName is back in stock",
        "$productName is available again. Order it before it sells out." +
            (productUrl?.let { "\n\nBuy it now: $it" } ?: ""),
    )

    fun sellerNewOrder(
        orderNumber: String,
        total: String,
    ) = Content("New order $orderNumber", "You have a new order worth $total. Confirm and fulfill it from your seller dashboard.")

    fun sellerOrderPaid(orderNumber: String) = Content("Order $orderNumber paid", "The order is paid and ready to fulfill.")

    fun sellerOrderCanceled(orderNumber: String) = Content("Order $orderNumber canceled", "The customer canceled this order.")

    fun sellerRefundUpdate(
        orderId: String,
        status: String,
        reason: String? = null,
    ) = Content("Refund $status", "A refund for order $orderId is now $status." + (reason?.let { " Reason: $it" } ?: ""))

    fun sellerRmaUpdate(
        orderId: String,
        status: String,
        note: String? = null,
    ) = Content("Return $status", "A return/exchange for order $orderId is now $status." + (note?.let { " Note: $it" } ?: ""))

    fun sellerReview(
        rating: Int,
        comment: String?,
    ) = Content("New $rating-star review", comment?.takeIf { it.isNotBlank() } ?: "A customer left a new review.")

    fun disputeCreated(
        disputeId: String,
        subject: String,
    ) = Content("New dispute $disputeId", "A dispute was opened: $subject")

    fun kycStatus(
        status: String,
        reason: String?,
    ) = Content("KYC $status", "Your KYC submission is $status." + (reason?.let { " Reason: $it" } ?: ""))

    fun shopStatus(
        status: String,
        reason: String?,
    ) = Content("Shop $status", "Your shop is now $status." + (reason?.let { " Reason: $it" } ?: ""))

    fun payoutStatus(
        status: String,
        amount: String,
        currency: String,
        reason: String? = null,
    ) = Content("Payout $status", "$currency $amount payout is $status." + (reason?.let { " Reason: $it" } ?: ""))

    fun adminQueue(
        title: String,
        detail: String,
    ) = Content(title, detail)
}
