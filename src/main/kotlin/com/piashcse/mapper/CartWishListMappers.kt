package com.piashcse.mapper

import com.piashcse.database.entities.*
import com.piashcse.model.response.CartItemSummary
import com.piashcse.model.response.CartResponse
import com.piashcse.model.response.ProductResponse
import com.piashcse.model.response.WishListResponse
import java.math.BigDecimal

fun CartItemDAO.toCartResponse(product: ProductResponse? = null) = CartResponse(productId.value, quantity, product)

fun CartItemDAO.toCartItemSummary(
    product: ProductDAO,
    unitPrice: BigDecimal,
    image: String?,
    stockQuantity: Int,
    shopName: String?,
) = CartItemSummary(
    productId = product.id.value,
    productName = product.name,
    price = unitPrice.toPlainString(),
    quantity = quantity,
    image = image,
    stockQuantity = stockQuantity,
    shopId = product.shopId?.value,
    shopName = shopName,
)

fun WishListDAO.toWishListResponse(product: ProductResponse? = null) = WishListResponse(product)
