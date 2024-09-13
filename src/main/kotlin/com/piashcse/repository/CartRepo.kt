package com.piashcse.repository

import com.piashcse.entities.orders.Cart
import com.piashcse.entities.product.Product

interface CartRepo {
    suspend fun addToCart(userId: String, productId: String, quantity: Int): Cart
    suspend fun getCartItems(userId: String, limit: Int, offset: Long): List<Cart>
    suspend fun updateCartQuantity(userId: String, productId: String, quantity: Int): Cart
    suspend fun removeCartItem(userId: String, productId: String): Product
    suspend fun deleteAllItemsOfCart(userId: String): Boolean
}