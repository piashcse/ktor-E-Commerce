package com.example.controller

import com.example.entities.shop.*
import com.example.utils.AlreadyExist
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ShopController {
    fun createShopCategory(shopCategoryName: String) = transaction {
        val categoryExist =
            ShopCategoryEntity.find { ShopCategoryTable.shop_category_name eq shopCategoryName }.toList().singleOrNull()
        return@transaction if (categoryExist == null) {
            ShopCategoryEntity.new(UUID.randomUUID().toString()) {
                this.shopCategoryName = shopCategoryName
            }.shopCategoryResponse()
        } else {
            throw AlreadyExist("Category name $shopCategoryName already exist")
        }
    }

    fun createShop(userId: String, shopCategoryId: String, shopName: String) = transaction {
        val shopNameExist = ShopEntity.find { ShopTable.shop_name eq shopName }.toList().singleOrNull()
        return@transaction if (shopNameExist == null) {
            ShopEntity.new(UUID.randomUUID().toString()) {
                user_id = EntityID(userId, ShopTable)
                shop_category_id = EntityID(shopCategoryId, ShopTable)
                shop_name = shopName
            }.shopResponse()
        } else {
            throw AlreadyExist("Shop name $shopName already exist")
        }
    }
}