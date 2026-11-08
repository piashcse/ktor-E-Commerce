package com.piashcse.mapper

import com.piashcse.database.entities.*
import com.piashcse.model.response.CartItemSummary
import com.piashcse.model.response.ProductResponse
import com.piashcse.model.response.ProductVariantResponse
import com.piashcse.utils.common.Money
import java.math.BigDecimal

fun CartItemDAO.toCartResponse(
    product: ProductResponse? = null,
    variant: ProductVariantResponse? = null,
) = Cart(productId.value, quantity, product, variantId?.value, variant)

fun CartItemDAO.toCartItemSummary(
    product: ProductDAO,
    unitPrice: BigDecimal,
    image: String?,
    stockQuantity: Int,
    shopName: String?,
    variant: ProductVariantDAO? = null,
) = CartItemSummary(
    productId = product.id.value,
    productName = product.name,
    price = Money.str(unitPrice),
    quantity = quantity,
    image = image,
    stockQuantity = stockQuantity,
    shopId = product.shopId?.value,
    shopName = shopName,
    variantId = variantId?.value,
    variantName = variant?.name,
)

fun WishListDAO.toWishListResponse(product: ProductResponse? = null) = WishList(product)
