package com.piashcse.repository

import com.piashcse.entities.WishList
import com.piashcse.entities.Product

interface WishListRepo {
    suspend fun addToWishList(userId: String, productId: String): WishList
    suspend fun getWishList(userId: String, limit: Int, offset: Long): List<Product>
    suspend fun deleteWishList(userId: String, productId: String): Product
}