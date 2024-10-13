package com.piashcse.controller

import com.piashcse.entities.Cart
import com.piashcse.entities.CartItemEntity
import com.piashcse.entities.CartItemTable
import com.piashcse.entities.product.Product
import com.piashcse.entities.product.ProductEntity
import com.piashcse.entities.product.ProductTable
import com.piashcse.repository.CartRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class CartController : CartRepo {
    override suspend fun addToCart(userId: String, productId: String, quantity: Int): Cart = query {
        val isProductExist =
            CartItemEntity.find { CartItemTable.userId eq userId and (CartItemTable.productId eq productId) }
                .toList().singleOrNull()
        isProductExist?.let {
            throw productId.alreadyExistException()
        } ?: CartItemEntity.new {
            this.userId = EntityID(userId, CartItemTable)
            this.productId = EntityID(productId, CartItemTable)
            this.quantity = quantity
        }.response()
    }

    override suspend fun getCartItems(userId: String, limit: Int, offset: Long): List<Cart> = query {
        CartItemEntity.find { CartItemTable.userId eq userId }.limit(limit, offset).map {
            it.response(ProductEntity.find { ProductTable.id eq it.productId }.first().response())
        }
    }

    override suspend fun updateCartQuantity(userId: String, productId: String, quantity: Int): Cart = query {
        val isProductExist =
            CartItemEntity.find { CartItemTable.userId eq userId and (CartItemTable.productId eq productId) }
                .toList().singleOrNull()
        isProductExist?.let {
            it.quantity = it.quantity + quantity
            it.response(ProductEntity.find { ProductTable.id eq it.productId }.first().response())
        } ?: throw productId.notFoundException()
    }

    override suspend fun deleteCartItem(userId: String, productId: String): Product = query {
        val isProductExist =
            CartItemEntity.find { CartItemTable.userId eq userId and (CartItemTable.productId eq productId) }
                .toList().singleOrNull()
        isProductExist?.let {
            it.delete()
            ProductEntity.find { ProductTable.id eq it.productId }.first().response()
        } ?: throw productId.notFoundException()

    }

    override suspend fun deleteAllItemsOfCart(userId: String): Boolean = query {
        val isCartExist = CartItemEntity.find { CartItemTable.userId eq userId }.toList()
        if (isCartExist.isEmpty()) {
            true
        } else {
            isCartExist.forEach {
                it.delete()
            }
            true
        }
    }
}