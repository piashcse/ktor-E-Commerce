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
}
