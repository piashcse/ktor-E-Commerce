package com.piashcse.feature.cart

import com.piashcse.constants.Message
import com.piashcse.constants.ProductStatus
import com.piashcse.database.entities.CartItemDAO
import com.piashcse.database.entities.CartItemTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductVariantDAO
import com.piashcse.database.entities.ProductVariantTable
import com.piashcse.database.entities.WishlistDAO
import com.piashcse.database.entities.WishlistTable
import com.piashcse.database.entities.effectiveStock
import com.piashcse.database.entities.effectiveStockFor
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull

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
    variantId: String? = null,
): ProductDAO = requireCartSelection(productId, quantity, resourceName, variantId).first

/** Resolves the product/variant pair and validates stock in one pass. */
fun requireCartSelection(
    productId: String,
    quantity: Int,
    resourceName: String = "Product",
    variantId: String? = null,
): Pair<ProductDAO, ProductVariantDAO?> {
    val product = requireProduct(productId, resourceName)
    val variant = resolveVariant(product, variantId)
    if (variant == null && hasVariants(product)) {
        throw ValidationException("Select a product variant before adding it to the cart")
    }
    val stock =
        if (variant == null) product.effectiveStock() else product.effectiveStockFor(variant.id.value)
    if (quantity > stock) throw ValidationException(Message.Validation.insufficientStock(product.name, stock))
    if (quantity < product.minOrderQuantity) {
        throw ValidationException("Minimum order quantity for ${product.name} is ${product.minOrderQuantity}")
    }
    return product to variant
}

fun resolveVariant(
    product: ProductDAO,
    variantId: String?,
): ProductVariantDAO? {
    if (variantId == null) return null
    val variant = ProductVariantDAO.findById(variantId) ?: variantId.throwNotFound("Variant")
    if (variant.productId.value != product.id.value) {
        throw ValidationException("Variant does not belong to this product")
    }
    if (variant.status != ProductStatus.ACTIVE) {
        throw ValidationException(Message.Products.OUT_OF_STOCK)
    }
    return variant
}

fun hasVariants(product: ProductDAO): Boolean = ProductVariantDAO.find { ProductVariantTable.productId eq product.id }.firstOrNull() != null

fun requireCartItem(
    userId: String,
    productId: String,
    variantId: String? = null,
): CartItemDAO =
    CartItemDAO.find {
        (CartItemTable.userId eq userId) and
            (CartItemTable.productId eq productId) and
            (
                if (variantId == null) {
                    CartItemTable.variantId.isNull()
                } else {
                    CartItemTable.variantId eq variantId.entityID(ProductVariantTable)
                }
            )
    }.singleOrNull() ?: productId.throwNotFound("Product")

fun requireWishlistItem(
    userId: String,
    productId: String,
): WishlistDAO =
    WishlistDAO.find {
        WishlistTable.userId eq userId and (WishlistTable.productId eq productId)
    }.firstOrNull() ?: productId.throwNotFound("ProductResponse")
