package com.piashcse.repository

import com.piashcse.entities.WishList
import com.piashcse.entities.product.Product

interface WishListRepo {
    suspend fun addToWishList(userId: String, productId: String): WishList
    suspend fun getWishList(userId: String): List<Product>
    suspend fun deleteWishList(userId: String, productId: String): Product
}