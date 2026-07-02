package com.piashcse.feature.cart

import com.piashcse.constants.Message
import com.piashcse.database.entities.*
import com.piashcse.model.response.CartItemSummary
import com.piashcse.model.response.CartSummaryResponse
import com.piashcse.model.response.ProductResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class CartService : CartRepository {

    override suspend fun createCart(
        userId: String,
        productId: String,
        quantity: Int,
    ): Cart = query {
        if (userId.isBlank()) throw ValidationException(Message.Validation.blankField("User ID"))
        if (productId.isBlank()) throw ValidationException(Message.Validation.blankField("Product ID"))
        if (quantity <= 0) throw ValidationException(Message.Validation.notPositive("Quantity"))

        val existing = CartItemDAO.find {
            CartItemTable.userId eq userId and (CartItemTable.productId eq productId)
        }.singleOrNull()
        existing?.let { throw productId.throwConflict("Product") }

        CartItemDAO.new {
            this.userId = EntityID(userId, UserTable)
            this.productId = EntityID(productId, ProductTable)
            this.quantity = quantity
        }.response()
    }

    override suspend fun getCartItems(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<Cart> = query {
        CartItemTable.selectAll().andWhere { CartItemTable.userId eq userId }
            .toPaginatedResponse(limit, offset) {
                val product = ProductDAO.findById(it[CartItemTable.productId].value)
                    ?: it[CartItemTable.productId].value.throwNotFound("Product")
                CartItemDAO.wrapRow(it).response(product.response())
            }
    }

    override suspend fun updateCartQuantity(
        userId: String,
        productId: String,
        quantity: Int,
    ): Cart? = query {
        if (userId.isBlank()) throw ValidationException(Message.Validation.blankField("User ID"))
        if (productId.isBlank()) throw ValidationException(Message.Validation.blankField("Product ID"))

        val cartItem = CartItemDAO.find {
            CartItemTable.userId eq userId and (CartItemTable.productId eq productId)
        }.singleOrNull() ?: productId.throwNotFound("Product")

        if (quantity == 0) { cartItem.delete(); return@query null }
        cartItem.quantity = quantity

        val product = ProductDAO.findById(cartItem.productId)
            ?: throw NotFoundException(Message.Cart.PRODUCT_NOT_FOUND)
        cartItem.response(product.response())
    }

    override suspend fun removeCartItem(
        userId: String,
        productId: String,
    ): ProductResponse = query {
        if (userId.isBlank()) throw ValidationException(Message.Validation.blankField("User ID"))
        if (productId.isBlank()) throw ValidationException(Message.Validation.blankField("Product ID"))

        val cartItem = CartItemDAO.find {
            CartItemTable.userId eq userId and (CartItemTable.productId eq productId)
        }.singleOrNull() ?: productId.throwNotFound("Product")

        val product = ProductDAO.findById(cartItem.productId)
            ?: throw NotFoundException(Message.Cart.PRODUCT_NOT_FOUND)
        cartItem.delete()
        product.response()
    }

    override suspend fun clearCart(userId: String): Boolean = query {
        if (userId.isBlank()) throw ValidationException(Message.Validation.blankField("User ID"))
        CartItemTable.deleteWhere { CartItemTable.userId eq userId }
        true
    }

    override suspend fun getCartSummary(userId: String): CartSummaryResponse = query {
        if (userId.isBlank()) throw ValidationException(Message.Validation.blankField("User ID"))

        val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.toList()
        val products = ProductDAO.find {
            ProductTable.id inList cartItems.map { it.productId.value }.distinct()
        }.associateBy { it.id.value }
        val shopIds = products.values.mapNotNull { it.shopId?.value }.distinct()
        val shops = if (shopIds.isNotEmpty()) {
            ShopDAO.find { ShopTable.id inList shopIds }.associateBy { it.id.value }
        } else {
            emptyMap()
        }

        val items = cartItems.mapNotNull { cartItem ->
            val product = products[cartItem.productId.value] ?: return@mapNotNull null
            CartItemSummary(
                productId = product.id.value,
                productName = product.name,
                price = (product.discountPrice ?: product.price).toDouble(),
                quantity = cartItem.quantity,
                image = product.images.split(",").firstOrNull()?.takeIf { it.isNotBlank() },
                stockQuantity = product.effectiveStock(),
                shopId = product.shopId?.value,
                shopName = product.shopId?.value?.let { shops[it]?.name },
            )
        }

        val subtotal = items.sumOf { it.price * it.quantity }
        CartSummaryResponse(items, subtotal, subtotal * 0.1, items.size)
    }
}
