package com.piashcse.feature.seller

import com.piashcse.database.entities.CommissionRuleDAO
import com.piashcse.database.entities.CommissionRuleTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.ProductCategoryTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.math.RoundingMode

@Serializable
data class CommissionRuleRequest(
    val name: String,
    val sellerId: String? = null,
    val shopId: String? = null,
    val categoryId: String? = null,
    @Contextual val rate: BigDecimal,
    val priority: Int = 0,
    val isActive: Boolean = true,
) {
    init {
        if (name.isBlank()) throw FieldValidationException(listOf(FieldViolation("name", "Name is required")))
        if (rate < BigDecimal.ZERO || rate > BigDecimal("100")) {
            throw FieldValidationException(listOf(FieldViolation("rate", "Rate must be between 0 and 100")))
        }
    }
}

@Serializable
data class CommissionRuleResponse(
    val id: String,
    val name: String,
    val sellerId: String?,
    val shopId: String?,
    val categoryId: String?,
    val rate: String,
    val priority: Int,
    val isActive: Boolean,
)

@Serializable
data class EffectiveCommissionResponse(
    val rate: String,
    val source: String,
)

/**
 * Commission resolution: most specific active rule wins.
 * Specificity: seller (4) > shop (2) > category (1); ties break on priority.
 */
class CommissionService {
    fun resolveRateTx(
        seller: SellerDAO,
        categoryId: String? = null,
    ): BigDecimal {
        val candidates =
            CommissionRuleDAO.find { CommissionRuleTable.isActive eq true }
                .filter { rule ->
                    val sellerOk = rule.sellerId == null || rule.sellerId?.value == seller.id.value
                    val shopOk = rule.shopId == null || rule.shopId?.value == seller.shopId?.value
                    val categoryOk = rule.categoryId == null || rule.categoryId?.value == categoryId
                    sellerOk && shopOk && categoryOk
                }
        val best =
            candidates.maxWithOrNull(
                compareBy<CommissionRuleDAO> { specificity(it) }
                    .thenBy { it.priority }
                    .thenBy { it.createdAt },
            )
        return best?.rate ?: seller.commissionRate
    }

    /** Weighted commission across the order's lines, using each line's product category. */
    fun commissionForOrderTx(
        order: OrderDAO,
        seller: SellerDAO,
    ): BigDecimal {
        val items = OrderItemDAO.itemsForOrder(order.id)
        if (items.isEmpty()) return BigDecimal.ZERO.setScale(2)
        val netTotal = order.subTotal.subtract(order.discountAmount).max(BigDecimal.ZERO)
        val commission =
            items.fold(BigDecimal.ZERO) { acc, item ->
                val discountShare =
                    if (order.subTotal > BigDecimal.ZERO) {
                        Money.proportionalSplit(order.discountAmount, item.total, order.subTotal)
                    } else {
                        BigDecimal.ZERO
                    }
                val net = item.total.subtract(discountShare).max(BigDecimal.ZERO)
                val categoryId = ProductDAO.findById(item.productId.value)?.categoryId?.value
                acc.add(Money.commission(net, resolveRateTx(seller, categoryId)))
            }
        return Money.scale2(commission).max(BigDecimal.ZERO).min(netTotal)
    }

    fun commissionForItemTx(
        order: OrderDAO,
        orderItem: OrderItemDAO,
        seller: SellerDAO,
        amount: BigDecimal,
    ): BigDecimal {
        val categoryId = ProductDAO.findById(orderItem.productId.value)?.categoryId?.value
        return Money.commission(amount, resolveRateTx(seller, categoryId))
    }

    suspend fun effectiveRate(
        sellerUserId: String,
        categoryId: String? = null,
    ): EffectiveCommissionResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val category = categoryId?.takeIf { it.isNotBlank() }
            val rate = resolveRateTx(seller, category)
            val source =
                if (rate.compareTo(seller.commissionRate) == 0) {
                    "seller_default"
                } else {
                    "rule"
                }
            EffectiveCommissionResponse(rate.setScale(2, RoundingMode.HALF_UP).toPlainString(), source)
        }

    suspend fun list(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<CommissionRuleResponse> =
        query {
            CommissionRuleTable.selectAll()
                .toPaginatedResponse(limit, offset) { CommissionRuleDAO.wrapRow(it).toResponse() }
        }

    suspend fun create(request: CommissionRuleRequest): CommissionRuleResponse =
        query {
            validateRefs(request)
            CommissionRuleDAO.new {
                name = request.name
                sellerId = request.sellerId?.takeIf { it.isNotBlank() }?.entityID(SellerTable)
                shopId = request.shopId?.takeIf { it.isNotBlank() }?.entityID(ShopTable)
                categoryId = request.categoryId?.takeIf { it.isNotBlank() }?.entityID(ProductCategoryTable)
                rate = request.rate
                priority = request.priority
                isActive = request.isActive
            }.toResponse()
        }

    suspend fun update(
        id: String,
        request: CommissionRuleRequest,
    ): CommissionRuleResponse =
        query {
            validateRefs(request)
            val rule = CommissionRuleDAO.findById(id) ?: id.throwNotFound("Commission rule")
            rule.apply {
                name = request.name
                sellerId = request.sellerId?.takeIf { it.isNotBlank() }?.entityID(SellerTable)
                shopId = request.shopId?.takeIf { it.isNotBlank() }?.entityID(ShopTable)
                categoryId = request.categoryId?.takeIf { it.isNotBlank() }?.entityID(ProductCategoryTable)
                rate = request.rate
                priority = request.priority
                isActive = request.isActive
            }.toResponse()
        }

    suspend fun delete(id: String): Boolean =
        query {
            val rule = CommissionRuleDAO.findById(id) ?: return@query false
            rule.delete()
            true
        }

    private fun validateRefs(request: CommissionRuleRequest) {
        request.sellerId?.takeIf { it.isNotBlank() }?.let {
            if (SellerDAO.findById(it) == null) throw ValidationException("Seller not found")
        }
        request.shopId?.takeIf { it.isNotBlank() }?.let {
            if (com.piashcse.database.entities.ShopDAO.findById(it) == null) throw ValidationException("Shop not found")
        }
        request.categoryId?.takeIf { it.isNotBlank() }?.let {
            if (ProductCategoryTable.selectAll().where { ProductCategoryTable.id eq it.entityID(ProductCategoryTable) }.count() == 0L) {
                throw ValidationException("Category not found")
            }
        }
    }

    private fun specificity(rule: CommissionRuleDAO): Int =
        (if (rule.sellerId != null) 4 else 0) +
            (if (rule.shopId != null) 2 else 0) +
            (if (rule.categoryId != null) 1 else 0)

    private fun CommissionRuleDAO.toResponse() =
        CommissionRuleResponse(
            id = id.value,
            name = name,
            sellerId = sellerId?.value,
            shopId = shopId?.value,
            categoryId = categoryId?.value,
            rate = rate.toPlainString(),
            priority = priority,
            isActive = isActive,
        )
}
