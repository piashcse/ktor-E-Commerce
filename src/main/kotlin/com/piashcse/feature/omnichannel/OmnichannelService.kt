package com.piashcse.feature.omnichannel

import com.piashcse.constants.FulfillmentType
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.PickupStatus
import com.piashcse.constants.PosSessionStatus
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
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.WarehouseDAO
import com.piashcse.database.entities.WarehouseTable
import com.piashcse.database.entities.addSales
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.database.entities.decrementStock
import com.piashcse.database.entities.decrementStockAt
import com.piashcse.database.entities.decrementStockFor
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.PaymentCompletedEvent
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.feature.tax.TaxService
import com.piashcse.mapper.toOrderResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class StoreResponse(
    val id: String,
    val shopId: String,
    val name: String,
    val address: String?,
    val city: String?,
    val country: String?,
    val latitude: String?,
    val longitude: String?,
    val phone: String?,
    val openingHours: String?,
    val supportsPickup: Boolean,
    val supportsPos: Boolean,
    val distanceKm: Double?,
)

@Serializable
data class PosSaleItem(
    val productId: String,
    val quantity: Int,
    val variantId: String? = null,
) {
    init {
        if (quantity <= 0) throw FieldValidationException(listOf(FieldViolation("quantity", "Quantity must be positive")))
    }
}

@Serializable
data class PosSaleRequest(
    val items: List<PosSaleItem>,
    val warehouseId: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.COD,
    val customerEmail: String? = null,
    val idempotencyKey: String? = null,
    /** Optional open cash-drawer session; COD sales are counted as cash. */
    val posSessionId: String? = null,
) {
    init {
        if (items.isEmpty()) throw FieldValidationException(listOf(FieldViolation("items", "At least one item is required")))
    }
}

@Serializable
data class PickupCompleteRequest(val pickupCode: String)

@Serializable
data class PosSessionOpenRequest(
    @Contextual val openingFloat: BigDecimal = BigDecimal.ZERO,
    val warehouseId: String? = null,
    val notes: String? = null,
) {
    init {
        if (openingFloat < BigDecimal.ZERO) {
            throw FieldValidationException(listOf(FieldViolation("openingFloat", "Opening float cannot be negative")))
        }
    }
}

@Serializable
data class PosSessionCloseRequest(
    @Contextual val countedCash: BigDecimal,
    val notes: String? = null,
) {
    init {
        if (countedCash < BigDecimal.ZERO) {
            throw FieldValidationException(listOf(FieldViolation("countedCash", "Counted cash cannot be negative")))
        }
    }
}

@Serializable
data class PosSessionResponse(
    val id: String,
    val shopId: String,
    val warehouseId: String?,
    val openedBy: String,
    val status: PosSessionStatus,
    val openingFloat: String,
    val cashSales: String,
    val cardSales: String,
    val expectedCash: String?,
    val countedCash: String?,
    val variance: String?,
    val openedAt: String,
    val closedAt: String?,
    val notes: String?,
    val closeNotes: String?,
)

class OmnichannelService(
    private val digitalDelivery: DigitalDeliveryService = DigitalDeliveryService(),
    private val taxService: TaxService = TaxService(),
    private val loyaltyService: LoyaltyService = LoyaltyService(),
) {
    suspend fun stores(
        latitude: Double?,
        longitude: Double?,
        radiusKm: Double?,
        limit: Int,
    ): List<StoreResponse> =
        query {
            val approvedShops =
                com.piashcse.database.entities.ShopDAO.find {
                    ShopTable.status eq com.piashcse.constants.ShopStatus.APPROVED
                }.map { it.id }.toSet()
            val rows =
                WarehouseDAO.find {
                    (WarehouseTable.isActive eq true) and
                        (WarehouseTable.supportsPickup eq true) and
                        (WarehouseTable.shopId inList approvedShops)
                }.map { it.toStoreResponse(latitude, longitude) }
            rows.filter { warehouse ->
                radiusKm == null || warehouse.distanceKm == null || warehouse.distanceKm <= radiusKm
            }.sortedWith(compareBy<StoreResponse> { it.distanceKm ?: Double.MAX_VALUE }).take(limit.coerceIn(1, 100))
        }

    suspend fun pickupOrders(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse> =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId?.value ?: throw ValidationException("Seller has no shop")
            val rows =
                OrderDAO.find {
                    (OrderTable.shopId eq shopId.entityID(com.piashcse.database.entities.ShopTable)) and
                        (OrderTable.fulfillmentType eq FulfillmentType.PICKUP)
                }.sortedByDescending { it.createdAt }
            val page = rows.drop(offset).take(limit).map { it.toOrderResponse() }
            PaginatedResponse(
                data = page,
                metadata = PaginationMetadata(totalCount = rows.size.toLong(), limit = limit, offset = offset),
            )
        }

    suspend fun markReady(
        sellerUserId: String,
        orderId: String,
    ): OrderResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
            if (order.shopId?.value != seller.shopId?.value) throw ForbiddenException("Order belongs to another shop")
            if (order.fulfillmentType != FulfillmentType.PICKUP) throw ValidationException("Order is not a pickup order")
            if (order.paymentStatus != PaymentStatus.COMPLETED) throw ValidationException("Order is not paid")
            if (order.status == OrderStatus.CANCELED) throw ValidationException("Order is canceled")
            if (order.pickupStatus != null && order.pickupStatus != PickupStatus.PENDING) {
                throw ValidationException("Pickup order cannot be marked ready")
            }
            order.pickupStatus = PickupStatus.READY
            order.toOrderResponse()
        }

    suspend fun completePickup(
        sellerUserId: String,
        orderId: String,
        code: String,
    ): OrderResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
            if (order.shopId?.value != seller.shopId?.value) throw ForbiddenException("Order belongs to another shop")
            if (order.fulfillmentType != FulfillmentType.PICKUP) throw ValidationException("Order is not a pickup order")
            if (order.paymentStatus != PaymentStatus.COMPLETED) throw ValidationException("Order is not paid")
            if (order.status == OrderStatus.CANCELED) throw ValidationException("Order is canceled")
            if (order.pickupStatus != PickupStatus.READY) throw ValidationException("Pickup order is not ready")
            if (order.pickupCode == null || order.pickupCode != code.trim().uppercase()) {
                throw ValidationException("Invalid pickup code")
            }
            order.pickupStatus = PickupStatus.PICKED_UP
            order.pickedUpAt = currentUtc()
            order.status = OrderStatus.RECEIVED
            order.completedDate = currentUtc()
            order.toOrderResponse()
        }

    suspend fun posSale(
        sellerUserId: String,
        request: PosSaleRequest,
    ): OrderResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId ?: throw ValidationException("Seller has no shop")
            if (request.paymentMethod in listOf(PaymentMethod.CREDIT_CARD, PaymentMethod.PAYPAL)) {
                throw ValidationException("POS card/PayPal processing is not configured; use COD or bank transfer")
            }
            findIdempotentPosSale(shopId.value, request.idempotencyKey)?.let { return@query it.toOrderResponse() }
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

    private fun findIdempotentPosSale(
        shopId: String,
        key: String?,
    ): OrderDAO? =
        key?.takeIf { it.isNotBlank() }?.let {
            OrderDAO.find {
                (OrderTable.shopId eq shopId.entityID(com.piashcse.database.entities.ShopTable)) and
                    (OrderTable.fulfillmentType eq FulfillmentType.POS) and
                    (OrderTable.idempotencyKey eq it)
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
                        (PosSessionTable.shopId eq shopId.entityID(com.piashcse.database.entities.ShopTable))
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
            }
        }
        return PosCatalog(products, variants)
    }

    private fun resolvePosCustomer(customerEmail: String?): UserDAO {
        val email = customerEmail?.takeIf { it.isNotBlank() }?.trim()?.lowercase()
        return email?.let {
            UserDAO.find {
                (UserTable.email eq it) and (UserTable.userType eq com.piashcse.constants.UserType.CUSTOMER)
            }.firstOrNull()
        } ?: UserDAO.new {
            this.email = email ?: "pos-${UUID.randomUUID()}@local"
            userType = com.piashcse.constants.UserType.CUSTOMER
            password = "!"
            isVerified = email != null
            isGuest = email == null
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
            "POS-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}-${UUID.randomUUID().toString().take(6).uppercase()}"
        var subtotal = BigDecimal.ZERO
        var totalTax = BigDecimal.ZERO
        var exclusiveTax = BigDecimal.ZERO
        val order =
            OrderDAO.new {
                userId = customer.id
                this.shopId = shopId.entityID(com.piashcse.database.entities.ShopTable)
                this.orderNumber = orderNumber
                idempotencyKey = request.idempotencyKey?.takeIf { it.isNotBlank() }
                status = OrderStatus.PAID
                paymentStatus = PaymentStatus.COMPLETED
                subTotal = BigDecimal.ZERO
                shippingCost = BigDecimal.ZERO
                taxAmount = BigDecimal.ZERO
                this.total = BigDecimal.ZERO
                currency = "USD"
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
            val unit = Money.scale2(variant?.effectivePrice ?: product.discountPrice ?: product.price)
            val lineTotal = Money.scale2(unit.multiply(BigDecimal(item.quantity)))
            if (warehouse != null) {
                product.decrementStockAt(warehouse.id.value, variant?.id?.value, item.quantity)
            } else if (variant == null) {
                product.decrementStock(item.quantity)
            } else {
                product.decrementStockFor(variant.id.value, item.quantity)
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
                it[taxClassId] = quote.taxClassId?.entityID(com.piashcse.database.entities.TaxClassTable)
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
                currency = "USD"
                status = PaymentStatus.COMPLETED
                paymentMethod = request.paymentMethod
                transactionId = "POS-${order.id.value}"
                provider = com.piashcse.constants.PaymentProvider.MANUAL
                providerStatus = "pos"
                scaStatus = com.piashcse.constants.ScaStatus.NOT_REQUIRED
            }
        session?.let {
            if (request.paymentMethod == PaymentMethod.COD) {
                it.cashSales = Money.scale2(it.cashSales.add(order.total))
            } else {
                it.cardSales = Money.scale2(it.cardSales.add(order.total))
            }
        }
        loyaltyService.earnForPaidOrderTx(customer.id.value, order.id.value, order.total)
        OutboxPublisher.enqueueTx(
            "PAYMENT",
            payment.id.value,
            PaymentCompletedEvent(
                paymentId = payment.id.value,
                orderId = order.id.value,
                userId = customer.id.value,
                email = customer.email,
                amount = payment.amount,
            ),
        )
        digitalDelivery.grantForOrderTx(order)
        return order.toOrderResponse()
    }

    suspend fun openPosSession(
        sellerUserId: String,
        request: PosSessionOpenRequest,
    ): PosSessionResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId ?: throw ValidationException("Seller has no shop")
            val warehouse =
                request.warehouseId?.takeIf { it.isNotBlank() }?.let { warehouseId ->
                    val row =
                        WarehouseDAO.findById(warehouseId)
                            ?: throw ValidationException("Warehouse not found")
                    if (row.shopId.value != shopId.value) throw ForbiddenException("Warehouse belongs to another shop")
                    if (!row.supportsPos) throw ValidationException("Warehouse does not support POS")
                    row
                }
            val existing =
                PosSessionDAO.find {
                    (PosSessionTable.shopId eq shopId) and (PosSessionTable.status eq PosSessionStatus.OPEN)
                }.firstOrNull()
            if (existing != null) throw ValidationException("A POS session is already open for this shop")
            PosSessionDAO.new {
                this.shopId = shopId
                this.warehouseId = warehouse?.id
                this.openedBy = sellerUserId.entityID(UserTable)
                status = PosSessionStatus.OPEN
                openingFloat = Money.scale2(request.openingFloat)
                notes = request.notes
            }.toResponse()
        }

    suspend fun closePosSession(
        sellerUserId: String,
        sessionId: String,
        request: PosSessionCloseRequest,
    ): PosSessionResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId ?: throw ValidationException("Seller has no shop")
            val session =
                PosSessionDAO.find {
                    (PosSessionTable.id eq sessionId.entityID(PosSessionTable)) and
                        (PosSessionTable.shopId eq shopId)
                }.forUpdate().firstOrNull() ?: throw ValidationException("POS session not found")
            if (session.status != PosSessionStatus.OPEN) throw ValidationException("POS session is already closed")
            val expected = Money.scale2(session.openingFloat.add(session.cashSales))
            val counted = Money.scale2(request.countedCash)
            session.expectedCash = expected
            session.countedCash = counted
            session.variance = Money.scale2(counted.subtract(expected))
            session.status = PosSessionStatus.CLOSED
            session.closedAt = currentUtc()
            session.closeNotes = request.notes
            session.toResponse()
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

    suspend fun posSessionReport(
        sellerUserId: String,
        sessionId: String,
    ): String =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId ?: throw ValidationException("Seller has no shop")
            val session =
                PosSessionDAO.find {
                    (PosSessionTable.id eq sessionId.entityID(PosSessionTable)) and
                        (PosSessionTable.shopId eq shopId)
                }.firstOrNull() ?: sessionId.throwNotFound("POS session")
            val sales =
                OrderDAO.find {
                    (OrderTable.posSessionId eq session.id) and
                        (OrderTable.fulfillmentType eq FulfillmentType.POS)
                }.count()
            val expected = session.expectedCash ?: Money.scale2(session.openingFloat.add(session.cashSales))
            """
            <!doctype html>
            <html><head><meta charset="utf-8"><title>POS Session ${escapeHtml(session.id.value)}</title>
            <style>
              body{font-family:system-ui,sans-serif;max-width:520px;margin:24px auto;color:#111}
              h1{font-size:20px;text-align:center;margin:0 0 4px}.muted{color:#666;text-align:center;font-size:12px}
              table{width:100%;border-collapse:collapse;margin:18px 0;font-size:14px}
              td{padding:8px 4px;border-bottom:1px dashed #ddd}.num{text-align:right;font-variant-numeric:tabular-nums}
              .total{font-weight:700}.foot{text-align:center;font-size:11px;color:#666;margin-top:20px}
              @media print{body{margin:0}}
            </style></head><body>
            <h1>POS Session Report</h1>
            <div class="muted">${escapeHtml(session.id.value)} · ${escapeHtml(session.status.name)}</div>
            <table>
              <tr><td>Opened</td><td class="num">${escapeHtml(session.openedAt.toString())}</td></tr>
              <tr><td>Closed</td><td class="num">${session.closedAt?.toString() ?: "-"}</td></tr>
              <tr><td>Sales</td><td class="num">$sales</td></tr>
              <tr><td>Opening float</td><td class="num">${session.openingFloat.toPlainString()}</td></tr>
              <tr><td>Cash sales</td><td class="num">${session.cashSales.toPlainString()}</td></tr>
              <tr><td>Card sales</td><td class="num">${session.cardSales.toPlainString()}</td></tr>
              <tr class="total"><td>Expected cash</td><td class="num">${expected.toPlainString()}</td></tr>
              <tr><td>Counted cash</td><td class="num">${session.countedCash?.toPlainString() ?: "-"}</td></tr>
              <tr class="total"><td>Variance</td><td class="num">${session.variance?.toPlainString() ?: "-"}</td></tr>
            </table>
            <div class="foot">Generated from the POS ledger.</div>
            </body></html>
            """.trimIndent()
        }

    private fun escapeHtml(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;")

    suspend fun posSessions(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PosSessionResponse> =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId ?: throw ValidationException("Seller has no shop")
            PosSessionTable.selectAll()
                .where { PosSessionTable.shopId eq shopId }
                .orderBy(PosSessionTable.openedAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { PosSessionDAO.wrapRow(it).toResponse() }
        }

    suspend fun posSales(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse> =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId?.value ?: throw ValidationException("Seller has no shop")
            val rows =
                OrderDAO.find {
                    (OrderTable.shopId eq shopId.entityID(com.piashcse.database.entities.ShopTable)) and
                        (OrderTable.fulfillmentType eq FulfillmentType.POS)
                }.sortedByDescending { it.createdAt }
            PaginatedResponse(
                data = rows.drop(offset).take(limit).map { it.toOrderResponse() },
                metadata = PaginationMetadata(totalCount = rows.size.toLong(), limit = limit, offset = offset),
            )
        }

    private fun PosSessionDAO.toResponse() =
        PosSessionResponse(
            id = id.value,
            shopId = shopId.value,
            warehouseId = warehouseId?.value,
            openedBy = openedBy.value,
            status = status,
            openingFloat = Money.str(openingFloat),
            cashSales = Money.str(cashSales),
            cardSales = Money.str(cardSales),
            expectedCash = expectedCash?.let(Money::str),
            countedCash = countedCash?.let(Money::str),
            variance = variance?.let(Money::str),
            openedAt = openedAt.toUtcString(),
            closedAt = closedAt?.toUtcString(),
            notes = notes,
            closeNotes = closeNotes,
        )

    private fun WarehouseDAO.toStoreResponse(
        latitude: Double?,
        longitude: Double?,
    ) = StoreResponse(
        id = id.value,
        shopId = shopId.value,
        name = name,
        address = address,
        city = city,
        country = country,
        latitude = this.latitude?.toPlainString(),
        longitude = this.longitude?.toPlainString(),
        phone = phone,
        openingHours = openingHours,
        supportsPickup = supportsPickup,
        supportsPos = supportsPos,
        distanceKm =
            run {
                val storeLat = this.latitude
                val storeLon = this.longitude
                if (latitude != null && longitude != null && storeLat != null && storeLon != null) {
                    haversineKm(latitude, longitude, storeLat.toDouble(), storeLon.toDouble())
                } else {
                    null
                }
            },
    )

    private fun haversineKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val radius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return radius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
