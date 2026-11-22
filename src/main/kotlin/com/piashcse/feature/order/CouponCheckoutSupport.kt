package com.piashcse.feature.order

import com.piashcse.constants.Message
import com.piashcse.database.entities.CartItemDAO
import com.piashcse.database.entities.CouponDAO
import com.piashcse.database.entities.CouponTable
import com.piashcse.database.entities.CouponUsageDAO
import com.piashcse.database.entities.CouponUsageTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductVariantDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.feature.coupon.CouponLine
import com.piashcse.feature.coupon.CouponRules
import com.piashcse.feature.coupon.toSnapshot
import com.piashcse.service.PricingService
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

/** Coupon lookup and line construction shared by placement and checkout preview. */
internal object CouponCheckoutSupport {
    /**
     * Loads and validates a coupon for the current pricing path. User/usage
     * checks live in [CouponRules] so every checkout entry point agrees.
     */
    fun load(
        code: String,
        forUpdate: Boolean = false,
        userId: String? = null,
    ): CouponDAO {
        val query = CouponDAO.find { (CouponTable.code eq CouponRules.normalize(code)) and (CouponTable.isActive eq true) }
        val coupon =
            (if (forUpdate) query.forUpdate() else query).firstOrNull()
                ?: throw ValidationException(Message.Orders.INVALID_COUPON)
        val usedByUser =
            userId?.let {
                CouponUsageDAO.find {
                    (CouponUsageTable.couponId eq coupon.id) and
                        (CouponUsageTable.userId eq it.entityID(UserTable))
                }.toList()
                    .map { row -> row.checkoutReference ?: row.id.value }
                    .distinct()
                    .size
            } ?: 0
        CouponRules.rejection(coupon.toSnapshot(), currentUtc(), usedByUser)?.let { throw ValidationException(it) }
        return coupon
    }

    fun lines(
        items: List<CartItemDAO>,
        products: Map<String, ProductDAO>,
        variants: Map<String, ProductVariantDAO>,
    ): List<CouponLine> =
        items.mapNotNull { item ->
            products[item.productId.value]?.let { product ->
                CouponLine(
                    productShopId = product.shopId?.value,
                    productCategoryId = product.categoryId.value,
                    unitPriceUsd = PricingService.unitPriceUsd(product, item.variantId?.value?.let(variants::get)),
                    quantity = item.quantity,
                )
            }
        }
}
