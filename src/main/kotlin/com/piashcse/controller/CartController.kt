package com.piashcse.controller

import com.piashcse.entities.orders.CartItemEntity
import com.piashcse.entities.orders.CartItemTable
import com.piashcse.entities.product.ProductEntity
import com.piashcse.entities.product.ProductTable
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class CartController {
   suspend fun addToCart(userId: String, productId: String, quantity:Int) = query {
        val isProductExist =
            CartItemEntity.find { CartItemTable.userId eq userId and (CartItemTable.productId eq productId) }
                .toList().singleOrNull()
        isProductExist?.let {
             productId.alreadyExistException()
        } ?: CartItemEntity.new {
            this.userId = EntityID(userId, CartItemTable)
            this.productId = EntityID(productId, CartItemTable)
            this.quantity = quantity
        }.cartResponse()
    }

    suspend fun getCartItems(userId: String, limit:Int, offset:Long) = query {
        CartItemEntity.find { CartItemTable.userId eq userId }.limit(limit, offset).map {
            it.cartResponse(ProductEntity.find { ProductTable.id eq it.productId }.first().response())
        }
    }

    suspend fun updateCartQuantity(userId: String, productId:String, quantity:Int) = query {
        val productExist =
            CartItemEntity.find { CartItemTable.userId eq userId and (CartItemTable.productId eq productId) }
                .toList().singleOrNull()

        productExist?.let {
            it.quantity = it.quantity + quantity
            it.cartResponse(ProductEntity.find { ProductTable.id eq it.productId }.first().response())
        }
    }

    suspend fun removeCartItem(userId: String, productId: String) = query {
        val productExist =
            CartItemEntity.find { CartItemTable.userId eq userId and (CartItemTable.productId eq productId) }
                .toList().singleOrNull()
        productExist?.let {
            it.delete()
            ProductEntity.find { ProductTable.id eq it.productId }.first().response()
        }

    }

    suspend fun deleteAllFromCart(userId: String) = query {
        val isEmpty = CartItemEntity.find { CartItemTable.userId eq userId }.toList()
        if (isEmpty.isEmpty()) {
            "".isNotExistException()
        } else {
            isEmpty.forEach {
                it.delete()
            }
            true
        }
    }
}