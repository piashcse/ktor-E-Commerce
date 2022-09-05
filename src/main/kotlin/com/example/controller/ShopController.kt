package com.example.controller

import com.example.entities.shop.*
import com.example.entities.user.UserTable
import com.example.utils.CommonException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction

class ShopController {
    fun createShopCategory(shopCategoryName: String) = transaction {
        val categoryExist =
            ShopCategoryEntity.find { ShopCategoryTable.shopCategoryName eq shopCategoryName }.toList().singleOrNull()
        return@transaction if (categoryExist == null) {
            ShopCategoryEntity.new() {
                this.shopCategoryName = shopCategoryName
            }.shopCategoryResponse()
        } else {
            throw CommonException("Category name $shopCategoryName already exist")
        }
    }

    fun getShopCategories(offset: Int, limit: Int) = transaction {
        val shopCategories = ShopCategoryEntity.all().limit(limit, offset.toLong())
        return@transaction shopCategories.map {
            it.shopCategoryResponse()
        }
    }

    fun updateShopCategory(shopCategoryId: String, shopCategoryName: String) = transaction {
        val shopCategoryExist =
            ShopCategoryEntity.find { ShopCategoryTable.id eq shopCategoryId }.toList().singleOrNull()
        return@transaction shopCategoryExist?.apply {
            this.shopCategoryName = shopCategoryName
        }?.shopCategoryResponse() ?: throw CommonException("Category id $shopCategoryId is not exist")
    }

    fun deleteShopCategory(shopCategoryId: String) = transaction {
        val shopCategoryExist =
            ShopCategoryEntity.find { ShopCategoryTable.id eq shopCategoryId }.toList().singleOrNull()
        return@transaction if (shopCategoryExist != null) {
            shopCategoryExist.delete()
            shopCategoryId
        } else {
            throw CommonException("Category id $shopCategoryId is not exist")
        }
    }

    fun createShop(userId: String, shopCategoryId: String, shopName: String) = transaction {
        val shopNameExist = ShopEntity.find { ShopTable.shopName eq shopName }.toList().singleOrNull()
        return@transaction if (shopNameExist == null) {
            ShopEntity.new() {
                this.userId = EntityID(userId, UserTable)
                this.shopCategoryId = EntityID(shopCategoryId, ShopTable)
                this.shopName = shopName
            }.shopResponse()
        } else {
            throw CommonException("Shop name $shopName already exist")
        }
    }
}