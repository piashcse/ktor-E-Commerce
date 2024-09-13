package com.piashcse.controller

import com.piashcse.entities.shop.*
import com.piashcse.repository.ShopCategoryRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query

class ShopCategoryController : ShopCategoryRepo {
    override suspend fun addShopCategory(shopCategoryName: String): ShopCategory = query {
        val isExistShopCategory =
            ShopCategoryEntity.find { ShopCategoryTable.shopCategoryName eq shopCategoryName }.toList().singleOrNull()
        isExistShopCategory?.let {
            throw shopCategoryName.alreadyExistException()
        } ?: ShopCategoryEntity.new {
            this.shopCategoryName = shopCategoryName
        }.response()
    }

    override suspend fun getShopCategories(limit: Int, offset: Long): List<ShopCategory> = query {
        val shopCategories = ShopCategoryEntity.all().limit(limit, offset)
        shopCategories.map {
            it.response()
        }
    }

    override suspend fun updateShopCategory(shopCategoryId: String, shopCategoryName: String): ShopCategory = query {
        val isShopCategoryExist =
            ShopCategoryEntity.find { ShopCategoryTable.id eq shopCategoryId }.toList().singleOrNull()
        isShopCategoryExist?.let {
            it.shopCategoryName = shopCategoryName
            it.response()
        } ?: throw shopCategoryId.notFoundException()
    }

    override suspend fun deleteShopCategory(shopCategoryId: String): String = query {
        val shopCategoryExist =
            ShopCategoryEntity.find { ShopCategoryTable.id eq shopCategoryId }.toList().singleOrNull()
        shopCategoryExist?.let {
            it.delete()
            shopCategoryId
        } ?: throw shopCategoryId.notFoundException()
    }
}