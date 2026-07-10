package com.piashcse.mapper

import com.piashcse.database.entities.*
import com.piashcse.model.response.CartItemSummary
import com.piashcse.model.response.ProductResponse
import java.math.BigDecimal

fun CartItemDAO.toCartResponse(product: ProductResponse? = null) = Cart(productId.value, quantity, product)

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

fun WishListDAO.toWishListResponse(product: ProductResponse? = null) = WishList(product)
