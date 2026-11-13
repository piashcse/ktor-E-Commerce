package com.piashcse.feature.invoice

import com.piashcse.constants.UserType
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireOrderAccess
import com.piashcse.utils.extension.throwNotFound

/**
 * Printable order documents. The response is self-contained HTML so the client can
 * print it or convert it to PDF without a server-side PDF dependency.
 */
class InvoiceService {
    suspend fun invoice(
        orderId: String,
        callerId: String,
        userType: UserType,
    ): String =
        query {
            val order = loadOrder(orderId, callerId, userType)
            val items = OrderItemDAO.itemsForOrder(order.id)
            val hasInclusiveTax = items.any { it.taxInclusive }
            val shopName = order.shopId?.value?.let { ShopDAO.findById(it)?.name }
            val email = order.contactEmail ?: UserDAO.findById(order.userId.value)?.email

            page(
                title = "Invoice ${order.orderNumber}",
                body =
                    """
                    <header>
                      <h1>Invoice</h1>
                      <p class="muted">${escape(order.orderNumber)} · ${escape(order.createdAt.toUtcString())}</p>
                    </header>
                    <section class="grid">
                      <div>
                        <h2>Bill to</h2>
                        <p>${escape(order.billingAddress ?: order.shippingAddress ?: "—")}</p>
                        <p>${escape(email ?: "")}</p>
                      </div>
                      <div>
                        <h2>Ship to</h2>
                        <p>${escape(order.shippingAddress ?: "—")}</p>
                      </div>
                      <div>
                        <h2>Details</h2>
                        <p>Shop: ${escape(shopName ?: "—")}</p>
                        <p>Payment: ${escape(order.paymentMethod?.name ?: "—")} (${escape(order.paymentStatus.name)})</p>
                        <p>Status: ${escape(order.status.name)}</p>
                        ${order.trackingNumber?.let { "<p>Tracking: ${escape(it)}</p>" } ?: ""}
                      </div>
                    </section>
                    <table>
                      <thead>
                        <tr><th>Item</th><th>SKU</th><th class="num">Qty</th><th class="num">Unit</th><th class="num">Total</th></tr>
                      </thead>
                      <tbody>
                        ${items.joinToString("") { item ->
                        """
                        <tr>
                          <td>${escape(item.productName ?: item.productId.value)}${item.variantName?.let { " (${escape(it)})" } ?: ""}</td>
                          <td>${escape(item.sku ?: "—")}</td>
                          <td class="num">${item.quantity}</td>
                          <td class="num">${escape(Money.str(item.price))}</td>
                          <td class="num">${escape(Money.str(item.total))}</td>
                        </tr>
                        """.trimIndent()
                    }}
                      </tbody>
                    </table>
                    <section class="totals">
                      <p><span>Subtotal${if (hasInclusiveTax) " (incl. tax)" else ""}</span><span>${escape(
                        Money.str(order.subTotal),
                    )} ${escape(order.currency)}</span></p>
                      <p><span>Discount</span><span>-${escape(Money.str(order.discountAmount))} ${escape(order.currency)}</span></p>
                      <p><span>Shipping</span><span>${escape(Money.str(order.shippingCost))} ${escape(order.currency)}</span></p>
                      <p><span>${if (hasInclusiveTax) "Included tax" else "Tax"}</span><span>${escape(
                        Money.str(order.taxAmount),
                    )} ${escape(order.currency)}</span></p>
                      <p class="grand"><span>Total</span><span>${escape(Money.str(order.total))} ${escape(order.currency)}</span></p>
                    </section>
                    """.trimIndent(),
            )
        }

    suspend fun packingSlip(
        orderId: String,
        callerId: String,
        userType: UserType,
    ): String =
        query {
            val order = loadOrder(orderId, callerId, userType)
            val items = OrderItemDAO.itemsForOrder(order.id)
            val shopName = order.shopId?.value?.let { ShopDAO.findById(it)?.name }

            page(
                title = "Packing slip ${order.orderNumber}",
                body =
                    """
                    <header>
                      <h1>Packing slip</h1>
                      <p class="muted">${escape(order.orderNumber)} · ${escape(order.createdAt.toUtcString())}</p>
                    </header>
                    <section class="grid">
                      <div>
                        <h2>Ship to</h2>
                        <p>${escape(order.shippingAddress ?: "—")}</p>
                      </div>
                      <div>
                        <h2>Details</h2>
                        <p>Shop: ${escape(shopName ?: "—")}</p>
                        <p>Shipping: ${escape(order.shippingMethod ?: "—")}</p>
                        ${order.trackingNumber?.let { "<p>Tracking: ${escape(it)}</p>" } ?: ""}
                      </div>
                    </section>
                    <table>
                      <thead><tr><th>Item</th><th>SKU</th><th class="num">Qty</th></tr></thead>
                      <tbody>
                        ${items.joinToString("") { item ->
                        """
                        <tr>
                          <td>${escape(item.productName ?: item.productId.value)}${item.variantName?.let { " (${escape(it)})" } ?: ""}</td>
                          <td>${escape(item.sku ?: "—")}</td>
                          <td class="num">${item.quantity}</td>
                        </tr>
                        """.trimIndent()
                    }}
                      </tbody>
                    </table>
                    """.trimIndent(),
            )
        }

    private fun loadOrder(
        orderId: String,
        callerId: String,
        userType: UserType,
    ): OrderDAO {
        val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
        requireOrderAccess(order.userId.value, order.shopId?.value, callerId, userType)
        return order
    }

    private fun page(
        title: String,
        body: String,
    ) = """
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>${escape(title)}</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; color: #111; margin: 40px; }
            header { border-bottom: 2px solid #111; margin-bottom: 24px; }
            h1 { margin: 0 0 6px; font-size: 28px; }
            h2 { font-size: 14px; text-transform: uppercase; letter-spacing: .08em; margin: 0 0 6px; }
            p { margin: 3px 0; line-height: 1.45; white-space: pre-line; }
            .muted { color: #555; }
            .grid { display: flex; gap: 32px; margin-bottom: 28px; }
            .grid > div { flex: 1; }
            table { width: 100%; border-collapse: collapse; margin: 20px 0; }
            th, td { border-bottom: 1px solid #ddd; padding: 10px 8px; text-align: left; font-size: 14px; }
            th { background: #f5f5f5; }
            .num { text-align: right; }
            .totals { margin-left: auto; width: 320px; }
            .totals p, .totals .grand { display: flex; justify-content: space-between; margin: 6px 0; }
            .grand { font-weight: 700; border-top: 2px solid #111; padding-top: 8px; }
            @media print { body { margin: 16mm; } }
          </style>
        </head>
        <body>
        $body
        </body>
        </html>
        """.trimIndent()

    private fun escape(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
}
