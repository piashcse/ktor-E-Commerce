package com.piashcse.feature.omnichannel

import com.piashcse.constants.AppConstants
import com.piashcse.constants.FulfillmentType
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.PosSessionStatus
import com.piashcse.constants.ProductStatus
import com.piashcse.constants.ScaStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.OrderItemTable
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.PosSessionDAO
import com.piashcse.database.entities.PosSessionTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.ProductVariantDAO
import com.piashcse.database.entities.ProductVariantTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.TaxClassTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.WarehouseDAO
import com.piashcse.database.entities.addSales
import com.piashcse.database.entities.decrementStock
import com.piashcse.database.entities.decrementStockAt
import com.piashcse.database.entities.decrementStockFor
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.order.OrderCompletion
import com.piashcse.feature.seller.SellerLedger
import com.piashcse.feature.tax.TaxService
import com.piashcse.mapper.toOrderResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.service.DigitalDeliveryService
import com.piashcse.service.PricingService
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.readQuery
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/** POS sales, receipts and sale listing. */
class PosSaleService(
    private val digitalDelivery: DigitalDeliveryService,
    private val taxService: TaxService,
    private val loyaltyService: LoyaltyService,
) {
    suspend fun posSale(
        sellerUserId: String,
        request: PosSaleRequest,
    ): OrderResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId ?: throw ValidationException("Seller has no shop")
            requireOperationalShop(shopId.value)
            if (request.paymentMethod in listOf(PaymentMethod.CREDIT_CARD, PaymentMethod.PAYPAL)) {
                throw ValidationException("POS card/PayPal processing is not configured; use COD or bank transfer")
            }
            findIdempotentPosSale(shopId.value, request.idempotencyKey)?.let { return@query it.toOrderResponse(includeSensitive = false) }
            val session = resolvePosSession(shopId.value, request.posSessionId)
            val warehouse = resolvePosWarehouse(shopId.value, request.warehouseId, session)
            val catalog = loadPosCatalog(shopId.value, request.items)
            val customer = resolvePosCustomer(request.customerEmail)
            completePosSale(shopId.value, request, session, warehouse, catalog, customer)
        }

    private data class PosCatalog(
        val products: Map<String, ProductDAO>,
        val variants: Map<String, ProductVariantDAO>,
    )

    private fun posIdempotencyKey(
        shopId: String,
        key: String?,
    ): String? = key?.takeIf { it.isNotBlank() }?.let { "pos:$shopId:$it" }

    private fun findIdempotentPosSale(
        shopId: String,
        key: String?,
    ): OrderDAO? =
        posIdempotencyKey(shopId, key)?.let { namespaced ->
            OrderDAO.find {
                (OrderTable.shopId eq shopId.entityID(ShopTable)) and
                    (OrderTable.fulfillmentType eq FulfillmentType.POS) and
                    (OrderTable.idempotencyKey eq namespaced)
            }.firstOrNull()
        }

    private fun resolvePosSession(
        shopId: String,
        sessionId: String?,
    ): PosSessionDAO? =
        sessionId?.takeIf { it.isNotBlank() }?.let {
            val row =
                PosSessionDAO.find {
                    (PosSessionTable.id eq it.entityID(PosSessionTable)) and
                        (PosSessionTable.shopId eq shopId.entityID(ShopTable))
                }.forUpdate().firstOrNull() ?: throw ValidationException("POS session not found")
            if (row.status != PosSessionStatus.OPEN) throw ValidationException("POS session is closed")
            row
        }

    private fun resolvePosWarehouse(
        shopId: String,
        warehouseId: String?,
        session: PosSessionDAO?,
    ): WarehouseDAO? {
        val resolvedId = warehouseId?.takeIf { it.isNotBlank() } ?: session?.warehouseId?.value
        return resolvedId?.let { id ->
            val row = WarehouseDAO.findById(id) ?: throw ValidationException("Warehouse not found")
            if (row.shopId.value != shopId) throw ForbiddenException("Warehouse belongs to another shop")
            if (!row.supportsPos) throw ValidationException("Warehouse does not support POS")
            row
        }
    }

    private fun loadPosCatalog(
        shopId: String,
        items: List<PosSaleItem>,
    ): PosCatalog {
        val products =
            ProductDAO.find {
                ProductTable.id.inList(items.map { it.productId.entityID(ProductTable) })
            }.associateBy { it.id.value }
        if (products.size != items.map { it.productId }.distinct().size) {
            throw ValidationException("One or more products were not found")
        }
        if (products.values.any { it.shopId?.value != shopId }) {
            throw ValidationException("All POS items must belong to the seller's shop")
        }
        if (products.values.any {
                it.deletedAt != null || it.status != ProductStatus.ACTIVE
            }
        ) {
            throw ValidationException("Cannot sell an inactive product in POS")
        }
        val variantIds = items.mapNotNull { it.variantId }.distinct()
        val variants =
            if (variantIds.isEmpty()) {
                emptyMap()
            } else {
                ProductVariantDAO.find {
                    ProductVariantTable.id inList variantIds.map { it.entityID(ProductVariantTable) }
                }.associateBy { it.id.value }
            }
        items.forEach { item ->
            item.variantId?.let { id ->
                val variant = variants[id] ?: throw ValidationException("Variant not found: $id")
                if (variant.productId.value != item.productId) {
                    throw ValidationException("Variant does not belong to product ${item.productId}")
                }
                if (variant.status != ProductStatus.ACTIVE) {
                    throw ValidationException("Cannot sell an inactive variant in POS")
                }
            }
        }
        return PosCatalog(products, variants)
    }

    private fun resolvePosCustomer(customerEmail: String?): UserDAO {
        val email = customerEmail?.takeIf { it.isNotBlank() }?.trim()?.lowercase()
        return email?.let {
            UserDAO.find {
                (UserTable.email eq it) and (UserTable.userType eq UserType.CUSTOMER)
            }.firstOrNull()
        } ?: UserDAO.new {
            this.email = email ?: "pos-${UUID.randomUUID()}@local"
            userType = UserType.CUSTOMER
            // Never auto-verify an email supplied at the counter: doing so
            // would block the real owner from registering. The row is a guest
            // account that can later be claimed through registration/OTP.
            password =
                at.favre.lib.crypto.bcrypt.BCrypt.withDefaults().hashToString(
                    AppConstants.BCRYPT_COST,
                    UUID.randomUUID().toString().toCharArray(),
                )
            isVerified = false
            isGuest = true
        }
    }

    private fun completePosSale(
        shopId: String,
        request: PosSaleRequest,
        session: PosSessionDAO?,
        warehouse: WarehouseDAO?,
        catalog: PosCatalog,
        customer: UserDAO,
    ): OrderResponse {
        val orderNumber =
            "POS-${LocalDate.now(
                ZoneOffset.UTC,
            ).format(DateTimeFormatter.BASIC_ISO_DATE)}-${UUID.randomUUID().toString().take(6).uppercase()}"
        var subtotal = BigDecimal.ZERO
        var totalTax = BigDecimal.ZERO
        var exclusiveTax = BigDecimal.ZERO
        val order =
            OrderDAO.new {
                userId = customer.id
                this.shopId = shopId.entityID(ShopTable)
                this.orderNumber = orderNumber
                idempotencyKey = posIdempotencyKey(shopId, request.idempotencyKey)
                status = OrderStatus.PAID
                paymentStatus = PaymentStatus.COMPLETED
                subTotal = BigDecimal.ZERO
                shippingCost = BigDecimal.ZERO
                taxAmount = BigDecimal.ZERO
                this.total = BigDecimal.ZERO
                currency = AppConstants.DEFAULT_CURRENCY
                fxRate = BigDecimal.ONE
                shippingMethod = "POS"
                fulfillmentType = FulfillmentType.POS
                paymentMethod = request.paymentMethod
                this.posSessionId = session?.id
                notes = "In-store sale"
            }

        request.items.forEach { item ->
            val product = catalog.products.getValue(item.productId)
            val variant = item.variantId?.let { catalog.variants[it] }
            val unit = Money.scale2(PricingService.unitPriceUsd(product, variant))
            val lineTotal = Money.scale2(unit.multiply(BigDecimal(item.quantity)))
            if (!product.isDigital) {
                if (warehouse != null) {
                    product.decrementStockAt(warehouse.id.value, variant?.id?.value, item.quantity)
                } else if (variant == null) {
                    product.decrementStock(item.quantity)
                } else {
                    product.decrementStockFor(variant.id.value, item.quantity)
                }
            }
            product.addSales(item.quantity)
            val quote =
                taxService.resolve(
                    country = warehouse?.country ?: "US",
                    region = null,
                    categoryId = product.categoryId.value,
                    taxClassId = product.taxClassId?.value,
                    userId = customer.id.value,
                    businessTaxId = null,
                )
            val amounts = taxService.apply(lineTotal, quote)
            subtotal = subtotal.add(lineTotal)
            totalTax = totalTax.add(amounts.tax)
            if (!quote.inclusive) exclusiveTax = exclusiveTax.add(amounts.tax)
            OrderItemTable.insert {
                it[orderId] = order.id
                it[productId] = product.id
                it[variantId] = variant?.id
                it[variantName] = variant?.name
                it[OrderItemTable.shopId] = shopId.entityID(ShopTable)
                it[quantity] = item.quantity
                it[price] = unit
                it[OrderItemTable.total] = lineTotal
                it[productName] = product.name
                it[sku] = product.sku
                it[isDigital] = product.isDigital
                it[discountAmount] = BigDecimal.ZERO
                it[taxAmount] = amounts.tax
                it[taxRate] = quote.rate
                it[taxInclusive] = quote.inclusive
                it[taxClassId] = quote.taxClassId?.entityID(TaxClassTable)
            }
        }
        order.subTotal = Money.scale2(subtotal)
        order.taxAmount = Money.scale2(totalTax)
        order.total = Money.scale2(subtotal.add(exclusiveTax))

        val payment =
            PaymentDAO.new {
                this.orderId = order.id
                this.userId = customer.id
                amount = order.total
                currency = AppConstants.DEFAULT_CURRENCY
                status = PaymentStatus.COMPLETED
                paymentMethod = request.paymentMethod
                transactionId = "POS-${order.id.value}"
                provider = PaymentProvider.MANUAL
                providerStatus = "pos"
                scaStatus = ScaStatus.NOT_REQUIRED
            }
        session?.let {
            if (request.paymentMethod == PaymentMethod.COD) {
                it.cashSales = Money.scale2(it.cashSales.add(order.total))
            } else {
                it.cardSales = Money.scale2(it.cardSales.add(order.total))
            }
        }
        SellerLedger.applyOrder(order)
        // Shared completion path: loyalty, seller payout ledger, digital delivery
        // and reservation finalization must match online/offline payments.
        OrderCompletion.completeTx(order, payment, loyaltyService, digitalDelivery)
        return order.toOrderResponse(includeSensitive = false)
    }

    suspend fun posReceipt(
        sellerUserId: String,
        orderId: String,
    ): String =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId ?: throw ValidationException("Seller has no shop")
            val order =
                OrderDAO.find {
                    (OrderTable.id eq orderId.entityID(OrderTable)) and
                        (OrderTable.shopId eq shopId)
                }.firstOrNull() ?: orderId.throwNotFound("POS order")
            if (order.fulfillmentType != FulfillmentType.POS) throw ValidationException("Order is not a POS sale")
            val items = OrderItemDAO.find { OrderItemTable.orderId eq order.id }.toList()
            val sessionId = order.posSessionId?.value
            val title = "POS Receipt ${order.orderNumber}"
            val rows =
                items.joinToString("") { item ->
                    """
                    <tr>
                      <td>${escapeHtml(item.productName.orEmpty())}</td>
                      <td class="num">${item.quantity}</td>
                      <td class="num">${item.price.toPlainString()}</td>
                      <td class="num">${item.total.toPlainString()}</td>
                    </tr>
                    """.trimIndent()
                }
            """
            <!doctype html>
            <html><head><meta charset="utf-8"><title>$title</title>
            <style>
              body{font-family:system-ui,sans-serif;max-width:420px;margin:24px auto;color:#111}
              h1{font-size:20px;text-align:center;margin:0 0 4px}
              .muted{color:#666;text-align:center;font-size:12px}
              table{width:100%;border-collapse:collapse;margin:16px 0;font-size:13px}
              th,td{padding:6px 4px;border-bottom:1px dashed #ddd;text-align:left}
              .num{text-align:right}.totals td{border:0;padding:2px 4px}
              .total{font-weight:700;font-size:16px}.foot{text-align:center;font-size:11px;color:#666;margin-top:20px}
              @media print{body{margin:0}}
            </style></head><body>
            <h1>${escapeHtml(order.orderNumber)}</h1>
            <div class="muted">${escapeHtml(order.createdAt.toString())}</div>
            <table><thead><tr><th>Item</th><th class="num">Qty</th><th class="num">Price</th><th class="num">Total</th></tr></thead>
            <tbody>$rows</tbody></table>
            <table class="totals">
              <tr><td>Subtotal</td><td class="num">${order.subTotal.toPlainString()}</td></tr>
              <tr><td>Tax</td><td class="num">${order.taxAmount.toPlainString()}</td></tr>
              <tr class="total"><td>Total</td><td class="num">${order.total.toPlainString()} ${escapeHtml(order.currency)}</td></tr>
              <tr><td>Payment</td><td class="num">${order.paymentMethod?.name ?: "POS"}</td></tr>
              <tr><td>Session</td><td class="num">${escapeHtml(sessionId ?: "-")}</td></tr>
            </table>
            <div class="foot">Thank you for shopping with us.</div>
            </body></html>
            """.trimIndent()
        }

    suspend fun posSales(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse> =
        readQuery {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId?.value ?: throw ValidationException("Seller has no shop")
            OrderTable.selectAll()
                .andWhere {
                    (OrderTable.shopId eq shopId.entityID(ShopTable)) and
                        (OrderTable.fulfillmentType eq FulfillmentType.POS)
                }
                .orderBy(OrderTable.createdAt to SortOrder.DESC)
                .toPosOrdersPaginated(limit, offset)
        }
}
