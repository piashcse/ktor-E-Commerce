package com.piashcse.repository

import com.piashcse.entities.shop.ShopCategory

interface ShopCategoryRepo {
    suspend fun addShopCategory(shopCategoryName: String): ShopCategory
    suspend fun getShopCategories(limit: Int, offset: Long): List<ShopCategory>
    suspend fun updateShopCategory(shopCategoryId: String, shopCategoryName: String): ShopCategory
    suspend fun deleteShopCategory(shopCategoryId: String) :String
}