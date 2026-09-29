package com.piashcse.feature.cart

import com.piashcse.constants.Message
import com.piashcse.database.entities.CartItemDAO
import com.piashcse.database.entities.CartItemTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.WishListDAO
import com.piashcse.database.entities.WishListTable
import com.piashcse.database.entities.effectiveStock
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

/**
 * Shared cart/wishlist guards. Thin wrappers around DAO lookups —
 * stock + conflict semantics are identical to the inlined originals.
 */
fun requireProduct(
    productId: String,
    resourceName: String = "Product",
): ProductDAO = ProductDAO.findById(productId) ?: productId.throwNotFound(resourceName)

fun requireProductWithStock(
    productId: String,
    quantity: Int,
    resourceName: String = "Product",
): ProductDAO {
    val product = requireProduct(productId, resourceName)
    val stock = product.effectiveStock()
    if (quantity > stock) throw ValidationException(Message.Validation.insufficientStock(product.name, stock))
    return product
}

fun requireCartItem(
    userId: String,
    productId: String,
): CartItemDAO =
    CartItemDAO.find {
        CartItemTable.userId eq userId and (CartItemTable.productId eq productId)
    }.singleOrNull() ?: productId.throwNotFound("Product")

fun requireWishlistItem(
    userId: String,
    productId: String,
): WishListDAO =
    WishListDAO.find {
        WishListTable.userId eq userId and (WishListTable.productId eq productId)
    }.firstOrNull() ?: productId.throwNotFound("ProductResponse")
