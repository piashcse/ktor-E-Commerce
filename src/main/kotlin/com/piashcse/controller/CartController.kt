package com.piashcse.controller

import com.piashcse.entities.orders.CartItemEntity
import com.piashcse.entities.orders.CartItemTable
import com.piashcse.entities.product.ProductEntity
import com.piashcse.entities.product.ProductTable
import com.piashcse.entities.user.UserTable
import com.piashcse.models.PagingData
import com.piashcse.models.cart.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class CartController {
    fun addToCart(userId: String, addCart: AddCart) = transaction {
        val isProductExist =
            CartItemEntity.find { CartItemTable.userId eq userId and (CartItemTable.productId eq addCart.productId) }
                .toList().singleOrNull()
        return@transaction isProductExist?.apply {
            this.quantity = this.quantity + addCart.quantity
        }?.cartResponse() ?: CartItemEntity.new {
            this.userId = EntityID(userId, CartItemTable)
            productId = EntityID(addCart.productId, CartItemTable)
            quantity = addCart.quantity
        }.cartResponse()
    }

    fun getCartItems(userId: String, pagingData: PagingData) = transaction {
        return@transaction CartItemEntity.find { CartItemTable.userId eq userId }
            .limit(pagingData.limit, pagingData.offset).map {
                it.cartResponse(ProductEntity.find { ProductTable.id eq it.productId }.first().response())
            }
    }

    fun updateCartQuantity(userId: String, updateCart: UpdateCart) = transaction {
        val productExist =
            CartItemEntity.find { CartItemTable.userId eq userId and (CartItemTable.productId eq updateCart.productId) }
                .toList().singleOrNull()

        productExist?.let {
            it.quantity = it.quantity + updateCart.quantity
            it.cartResponse(ProductEntity.find { ProductTable.id eq it.productId }.first().response())
        }
    }

    fun removeCartItem(userId: String, deleteProduct: DeleteProduct) = transaction {
        val productExist =
            CartItemEntity.find { CartItemTable.userId eq userId and (CartItemTable.productId eq deleteProduct.productId) }
                .toList().singleOrNull()
        productExist?.let {
            it.delete()
            ProductEntity.find { ProductTable.id eq it.productId }.first().response()
        }

    }

    fun deleteAllFromCart(userId: String) = transaction {
        return@transaction CartItemEntity.find { CartItemTable.userId eq userId }.toList().forEach {
            it.delete()
        }
    }
}