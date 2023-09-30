package com.piashcse.controller

import com.piashcse.dbhelper.query
import com.piashcse.entities.orders.CartItemEntity
import com.piashcse.entities.orders.CartItemTable
import com.piashcse.entities.product.ProductEntity
import com.piashcse.entities.product.ProductTable
import com.piashcse.entities.user.UserTable
import com.piashcse.models.PagingData
import com.piashcse.models.cart.*
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class CartController {
   suspend fun addToCart(userId: String, addCart: AddCart) = query {
        val isProductExist =
            CartItemEntity.find { CartItemTable.userId eq userId and (CartItemTable.productId eq addCart.productId) }
                .toList().singleOrNull()
        isProductExist?.let {
            addCart.productId.alreadyExistException()
        } ?: CartItemEntity.new {
            this.userId = EntityID(userId, CartItemTable)
            productId = EntityID(addCart.productId, CartItemTable)
            quantity = addCart.quantity
        }.cartResponse()
    }

    suspend fun getCartItems(userId: String, pagingData: PagingData) = query {
        CartItemEntity.find { CartItemTable.userId eq userId }.limit(pagingData.limit, pagingData.offset).map {
            it.cartResponse(ProductEntity.find { ProductTable.id eq it.productId }.first().response())
        }
    }

    suspend fun updateCartQuantity(userId: String, updateCart: UpdateCart) = query {
        val productExist =
            CartItemEntity.find { CartItemTable.userId eq userId and (CartItemTable.productId eq updateCart.productId) }
                .toList().singleOrNull()

        productExist?.let {
            it.quantity = it.quantity + updateCart.quantity
            it.cartResponse(ProductEntity.find { ProductTable.id eq it.productId }.first().response())
        }
    }

    suspend fun removeCartItem(userId: String, deleteProduct: DeleteProduct) = query {
        val productExist =
            CartItemEntity.find { CartItemTable.userId eq userId and (CartItemTable.productId eq deleteProduct.productId) }
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