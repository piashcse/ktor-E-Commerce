package com.piashcse.feature.cart

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.database.entities.*
import com.piashcse.mapper.toProductResponse
import com.piashcse.model.response.CartItemSummary
import com.piashcse.model.response.CartSummaryResponse
import com.piashcse.model.response.ProductResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
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
import java.math.BigDecimal
import java.math.RoundingMode

class CartService : CartRepository {

    override suspend fun createCart(
        userId: String,
        productId: String,
        quantity: Int,
    ): Cart = query {
        userId.requireNotBlank("User ID")
        productId.requireNotBlank("Product ID")
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
        val query = CartItemTable.selectAll().andWhere { CartItemTable.userId eq userId }
        val (totalCount, rows) = query.toPaginatedList(limit, offset) { it }
        val productIds = rows.map { it[CartItemTable.productId] }
        val products = if (productIds.isNotEmpty()) {
            ProductDAO.find { ProductTable.id inList productIds }.associateBy { it.id.value }
        } else {
            emptyMap()
        }
        val imagesMap = if (products.isNotEmpty()) {
            ProductImageDAO.imagesForProducts(products.keys.map { EntityID(it, ProductTable) })
        } else {
            emptyMap()
        }
        val data = rows.map { row ->
            val product = products[row[CartItemTable.productId].value]
                ?: row[CartItemTable.productId].value.throwNotFound("Product")
            CartItemDAO.wrapRow(row).response(product.toProductResponse(imagesMap[product.id.value]))
        }
        PaginatedResponse(data, PaginationMetadata(totalCount, limit, offset))
    }

    override suspend fun updateCartQuantity(
        userId: String,
        productId: String,
        quantity: Int,
    ): Cart? = query {
        userId.requireNotBlank("User ID")
        productId.requireNotBlank("Product ID")

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
        userId.requireNotBlank("User ID")
        productId.requireNotBlank("Product ID")

        val cartItem = CartItemDAO.find {
            CartItemTable.userId eq userId and (CartItemTable.productId eq productId)
        }.singleOrNull() ?: productId.throwNotFound("Product")

        val product = ProductDAO.findById(cartItem.productId)
            ?: throw NotFoundException(Message.Cart.PRODUCT_NOT_FOUND)
        cartItem.delete()
        product.response()
    }

    override suspend fun clearCart(userId: String): Boolean = query {
        userId.requireNotBlank("User ID")
        CartItemTable.deleteWhere { CartItemTable.userId eq userId }
        true
    }

    override suspend fun getCartSummary(userId: String): CartSummaryResponse = query {
        userId.requireNotBlank("User ID")

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
            val unitPrice = product.discountPrice ?: product.price
            CartItemSummary(
                productId = product.id.value,
                productName = product.name,
                price = unitPrice.toPlainString(),
                quantity = cartItem.quantity,
                image = product.imageUrls.firstOrNull(),
                stockQuantity = product.effectiveStock(),
                shopId = product.shopId?.value,
                shopName = product.shopId?.value?.let { shops[it]?.name },
            )
        }

        val subtotal = items.sumOf { BigDecimal(it.price) * BigDecimal(it.quantity) }
        val tax = subtotal.multiply(BigDecimal(AppConstants.DEFAULT_TAX_PERCENTAGE.toString())).setScale(2, RoundingMode.HALF_UP)
        CartSummaryResponse(items, subtotal.toPlainString(), tax.toPlainString(), items.size)
    }
}
