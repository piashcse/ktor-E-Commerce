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
    ) = Content("Order $orderNumber placed", "Your order total is $total. We will notify you when it ships.")

    fun paymentCompleted(
        amount: String,
        orderId: String,
    ) = Content("Payment received", "We received $amount for order $orderId.")

    fun refundStatusChanged(
        refundId: String,
        from: String,
        to: String,
    ) = Content("Refund $to", "Refund $refundId changed from $from to $to.")

    fun rmaStatusChanged(
        rmaId: String,
        from: String,
        to: String,
    ) = Content("Return/exchange $to", "Request $rmaId changed from $from to $to.")

    fun welcome(userType: String) = Content("Welcome", "Your account has been created as $userType.")

    fun orderStatusChanged(
        orderNumber: String,
        status: String,
        trackingNumber: String? = null,
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
        return Content(title, body)
    }
}
