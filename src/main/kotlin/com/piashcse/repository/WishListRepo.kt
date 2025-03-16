package com.piashcse.repository

import com.piashcse.entities.Product
import com.piashcse.entities.WishList

interface WishListRepo {
    suspend fun addToWishList(userId: String, productId: String): WishList
    suspend fun getWishList(userId: String, limit: Int): List<Product>
    suspend fun deleteWishList(userId: String, productId: String): Product
}