package com.example.entities.shop

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import com.example.entities.user.UserTable
import org.jetbrains.exposed.dao.id.EntityID

object ShopTable : BaseIntIdTable("shop") {
    val userId = reference("user_id", UserTable.id)
    val shopCategoryId = reference("shop_category_id", ShopCategoryTable.id)
    val shopName = text("shop_name")
}

class ShopEntity(id: EntityID<String>) : BaseIntEntity(id, ShopTable) {
    companion object : BaseIntEntityClass<ShopEntity>(ShopTable)

    var userId by ShopTable.userId
    var shopCategoryId by ShopTable.shopCategoryId
    var shopName by ShopTable.shopName
    fun shopResponse() = Shop(id.value, shopName)
}

data class Shop(val id: String, val shopName: String)