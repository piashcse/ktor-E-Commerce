package com.piashcse.controller

import com.piashcse.entities.shop.Shop
import com.piashcse.entities.shop.ShopEntity
import com.piashcse.entities.shop.ShopTable
import com.piashcse.entities.user.UserTable
import com.piashcse.repository.ShopRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class ShopController : ShopRepo {
    override suspend fun addShop(userId: String, shopCategoryId: String, shopName: String): Shop = query {
        val shopNameExist = ShopEntity.find { ShopTable.shopName eq shopName }.toList().singleOrNull()
        if (shopNameExist == null) {
            ShopEntity.new {
                this.userId = EntityID(userId, UserTable)
                this.shopCategoryId = EntityID(shopCategoryId, ShopTable)
                this.shopName = shopName
            }.shopResponse()
        } else {
            throw shopName.alreadyExistException()
        }
    }

    override suspend fun getShop(userId: String, limit: Int, offset: Long): List<Shop> = query {
        val isExist = ShopEntity.find { ShopTable.userId eq userId }.limit(limit, offset).toList()
        isExist.map {
            it.shopResponse()
        }
    }

    override suspend fun updateShop(userId: String, shopId: String, shopName: String): Shop = query {
        val shopNameExist =
            ShopEntity.find { ShopTable.userId eq userId and (ShopTable.id eq shopId) }.toList().singleOrNull()
        shopNameExist?.let {
            it.shopName = shopName
            it.shopResponse()
        } ?: throw shopName.notFoundException()
    }

    override suspend fun deleteShop(userId: String, shopId: String): String = query {
        val shopNameExist =
            ShopEntity.find { ShopTable.userId eq userId and (ShopTable.id eq shopId) }.toList().singleOrNull()
        shopNameExist?.let {
            it.delete()
            shopId
        } ?: throw shopId.notFoundException()
    }
}