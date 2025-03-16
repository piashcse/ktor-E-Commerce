package com.piashcse.repository

import com.piashcse.entities.Shop

interface ShopRepo {
    suspend fun addShop(userId: String, shopCategoryId: String, shopName: String): Shop
    suspend fun getShop(userId: String, limit: Int): List<Shop>
    suspend fun updateShop(userId: String, shopId: String, shopName: String): Shop
    suspend fun deleteShop(userId: String, shopId: String): String
}