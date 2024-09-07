package com.piashcse.controller

import com.piashcse.entities.shop.*
import com.piashcse.entities.user.UserTable
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class ShopController {
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

    suspend fun getShop(userId: String, limit: Int, offset: Long) = query {
        val isExist = ShopEntity.find { ShopTable.userId eq userId }.limit(limit, offset).toList()
        isExist.map {
            it.shopResponse()
        }
    }

    suspend fun updateShop(userId: String, shopId: String, shopName: String) = query {
        val shopNameExist =
            ShopEntity.find { ShopTable.userId eq userId and (ShopTable.id eq shopId) }.toList().singleOrNull()
        shopNameExist?.let {
            it.shopName = shopName
            it.shopResponse()
        } ?: shopName.isNotExistException()
    }

    suspend fun deleteShop(userId: String, shopId: String) = query {
        val shopNameExist =
            ShopEntity.find { ShopTable.userId eq userId and (ShopTable.id eq shopId) }.toList().singleOrNull()
        shopNameExist?.let {
            it.delete()
            shopId
        } ?: shopId.isNotExistException()
    }
}