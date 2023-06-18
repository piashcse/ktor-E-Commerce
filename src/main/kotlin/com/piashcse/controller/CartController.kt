package com.piashcse.controller

import com.piashcse.entities.orders.CartItemEntity
import com.piashcse.entities.orders.CartItemTable
import com.piashcse.entities.product.ProductTable
import com.piashcse.entities.user.UserTable
import com.piashcse.models.PagingData
import com.piashcse.models.cart.AddCart
import com.piashcse.models.cart.DeleteProduct
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class CartController {
    fun addToCart(userId: String, addCart: AddCart) = transaction {
        val isProductExist =
            CartItemEntity.find { CartItemTable.id eq userId and (CartItemTable.productId eq addCart.productId) }
                .toList().singleOrNull()
        return@transaction isProductExist?.apply {
            this.totalPrice = addCart.totalPrice
            this.quantity = this.quantity + addCart.quantity
            this.singlePrice = singlePrice
        } ?: CartItemEntity.new {
            this.userId = EntityID(userId, UserTable)
            productId = EntityID(addCart.productId, ProductTable)
            totalPrice = addCart.singlePrice * addCart.quantity
            singlePrice = addCart.singlePrice
            quantity = addCart.quantity
        }
    }

    fun removeCartItem(userId: String, deleteProduct: DeleteProduct) = transaction {
        val productExist =
            CartItemEntity.find { CartItemTable.id eq userId and (CartItemTable.productId eq deleteProduct.productId) }
                .toList().singleOrNull()
        productExist?.delete()
    }

    fun deleteCart(userId: String) = transaction {
        return@transaction CartItemEntity.find { CartItemTable.id eq userId }.toList().forEach {
            it.delete()
        }
    }

    fun getCartItems(userId: String, pagingData: PagingData) = transaction {
        return@transaction CartItemEntity.find { CartItemTable.id eq userId }.limit(pagingData.limit, pagingData.offset)
            .map {
                it.cartResponse()
            }
    }
}