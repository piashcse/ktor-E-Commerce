package com.piashcse.controller

import com.piashcse.dbhelper.query
import com.piashcse.entities.shop.*
import com.piashcse.entities.user.UserTable
import com.piashcse.utils.CommonException
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction

class ShopController {
    suspend fun createShopCategory(shopCategoryName: String) = query {
        val categoryExist =
            ShopCategoryEntity.find { ShopCategoryTable.shopCategoryName eq shopCategoryName }.toList().singleOrNull()
        if (categoryExist == null) {
            ShopCategoryEntity.new {
                this.shopCategoryName = shopCategoryName
            }.shopCategoryResponse()
        } else {
            shopCategoryName.alreadyExistException()
        }
    }

    suspend fun getShopCategories(limit: Int, offset: Long) = query {
        val shopCategories = ShopCategoryEntity.all().limit(limit, offset)
        shopCategories.map {
            it.shopCategoryResponse()
        }
    }

    suspend fun updateShopCategory(shopCategoryId: String, shopCategoryName: String) = query {
        val shopCategoryExist =
            ShopCategoryEntity.find { ShopCategoryTable.id eq shopCategoryId }.toList().singleOrNull()
        shopCategoryExist?.apply {
            this.shopCategoryName = shopCategoryName
        }?.shopCategoryResponse() ?: shopCategoryId.isNotExistException()
    }

    suspend fun deleteShopCategory(shopCategoryId: String) = query {
        val shopCategoryExist =
            ShopCategoryEntity.find { ShopCategoryTable.id eq shopCategoryId }.toList().singleOrNull()
        shopCategoryExist?.let {
            shopCategoryExist.delete()
            shopCategoryId
        } ?: run {
            shopCategoryId.isNotExistException()
        }
    }

    suspend fun createShop(userId: String, shopCategoryId: String, shopName: String) = query {
        val shopNameExist = ShopEntity.find { ShopTable.shopName eq shopName }.toList().singleOrNull()
        if (shopNameExist == null) {
            ShopEntity.new {
                this.userId = EntityID(userId, UserTable)
                this.shopCategoryId = EntityID(shopCategoryId, ShopTable)
                this.shopName = shopName
            }.shopResponse()
        } else {
            shopName.alreadyExistException()
        }
    }
}