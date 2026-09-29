package com.piashcse.feature.cart

import com.piashcse.constants.Message
import com.piashcse.database.entities.*
import com.piashcse.mapper.toCartItemSummary
import com.piashcse.mapper.toCartResponse
import com.piashcse.mapper.toProductResponse
import com.piashcse.model.response.CartSummaryResponse
import com.piashcse.model.response.ProductResponse
import com.piashcse.service.PricingService
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class CartRepositoryImpl : CartRepository {
    private fun requireCartParams(
        userId: String,
        productId: String,
        quantity: Int? = null,
    ) {
        userId.requireNotBlank("User ID")
        productId.requireNotBlank("Product ID")
        if (quantity != null && quantity <= 0) throw ValidationException(Message.Validation.notPositive("Quantity"))
    }

    override suspend fun createCart(
        userId: String,
        productId: String,
        quantity: Int,
    ): Cart =
        query {
            requireCartParams(userId, productId, quantity)

            // Advisory stock check only: read-then-write here is inherently racy (TOCTOU).
            // The authoritative check runs at order creation with a row-locked
            // effectiveStock(forUpdate = true) read, which re-validates before decrementing.
            val product = requireProductWithStock(productId, quantity)

            val existing =
                CartItemDAO.find {
                    CartItemTable.userId eq userId and (CartItemTable.productId eq productId)
                }.singleOrNull()
            existing?.let { throw productId.throwConflict("Product") }

            try {
                CartItemDAO.new {
                    this.userId = userId.entityID(UserTable)
                    this.productId = productId.entityID(ProductTable)
                    this.quantity = quantity
                }.toCartResponse()
            } catch (e: Exception) {
                // Unique constraint race: concurrent adds → 409 not 500.
                if ((e.message ?: "").contains("duplicate", ignoreCase = true)) throw productId.throwConflict("Product")
                throw e
            }
        }

    override suspend fun getCartItems(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<Cart> =
        query {
            val base = CartItemTable.selectAll().andWhere { CartItemTable.userId eq userId }
            base.paginateWithPreload(limit, offset, rowMapper = { it }) { rows ->
                val productIds = rows.map { it[CartItemTable.productId] }
                val products =
                    if (productIds.isNotEmpty()) {
                        ProductDAO.find { ProductTable.id inList productIds }.associateBy { it.id.value }
                    } else {
                        emptyMap()
                    }
                val imagesMap =
                    if (products.isNotEmpty()) {
                        ProductImageDAO.imagesForProducts(products.keys.map { it.entityID(ProductTable) })
                    } else {
                        emptyMap()
                    }
                rows.map { row ->
                    val product =
                        products[row[CartItemTable.productId].value]
                            ?: row[CartItemTable.productId].value.throwNotFound("Product")
                    CartItemDAO.wrapRow(row).toCartResponse(product.toProductResponse(imagesMap[product.id.value]))
                }
            }
        }

    override suspend fun updateCartQuantity(
        userId: String,
        productId: String,
        quantity: Int,
    ): Cart? =
        query {
            userId.requireNotBlank("User ID")
            productId.requireNotBlank("Product ID")

            val cartItem = requireCartItem(userId, productId)

            // Single non-positive path: quantity <= 0 deletes the line item (no throw branch).
            if (quantity <= 0) {
                cartItem.delete()
                return@query null
            }

            // Advisory stock check only: read-then-write here is inherently racy (TOCTOU) —
            // concurrent checkouts can oversell between this read and order placement.
            // The authoritative check runs at order creation with a row-locked
            // effectiveStock(forUpdate = true) read, which re-validates before decrementing.
            val product = requireProductWithStock(cartItem.productId.value, quantity)
            cartItem.quantity = quantity

            cartItem.toCartResponse(product.toProductResponse())
        }

    override suspend fun removeCartItem(
        userId: String,
        productId: String,
    ): ProductResponse =
        query {
            requireCartParams(userId, productId)

            val cartItem = requireCartItem(userId, productId)

            val product = requireProduct(cartItem.productId.value)
            cartItem.delete()
            product.toProductResponse()
        }

    override suspend fun clearCart(userId: String): Boolean =
        query {
            userId.requireNotBlank("User ID")
            CartItemTable.deleteWhere { CartItemTable.userId eq userId }
            true
        }

    override suspend fun getCartSummary(userId: String): CartSummaryResponse =
        query {
            val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.toList()
            val products =
                ProductDAO.find {
                    ProductTable.id inList cartItems.map { it.productId.value }.distinct()
                }.associateBy { it.id.value }
            val productEntityIds = products.keys.map { it.entityID(ProductTable) }
            val imagesMap =
                if (products.isNotEmpty()) {
                    ProductImageDAO.imagesForProducts(productEntityIds)
                } else {
                    emptyMap()
                }
            val inventoryMap =
                if (products.isNotEmpty()) {
                    InventoryDAO.find { InventoryTable.productId inList productEntityIds }
                        .associate { it.productId.value to it.stockQuantity }
                } else {
                    emptyMap()
                }
            val shopIds = products.values.mapNotNull { it.shopId?.value }.distinct()
            val shops =
                if (shopIds.isNotEmpty()) {
                    ShopDAO.find { ShopTable.id inList shopIds }.associateBy { it.id.value }
                } else {
                    emptyMap()
                }

            val items =
                cartItems.mapNotNull { cartItem ->
                    val product = products[cartItem.productId.value] ?: return@mapNotNull null
                    val unitPrice = product.discountPrice ?: product.price
                    cartItem.toCartItemSummary(
                        product = product,
                        unitPrice = unitPrice,
                        image = imagesMap[product.id.value]?.firstOrNull(),
                        stockQuantity = inventoryMap[product.id.value] ?: 0,
                        shopName = product.shopId?.value?.let { shops[it]?.name },
                    )
                }

            val lines = items.map { Money.of(it.price) to it.quantity }
            val subtotal = PricingService.subtotal(lines)
            val tax = PricingService.tax(subtotal)
            CartSummaryResponse(items, subtotal.toPlainString(), tax.toPlainString(), items.size)
        }
}
