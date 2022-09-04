package com.example.entities.shop

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import com.example.entities.user.UserTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ShopTable : BaseIntIdTable("shop") {
    val user_id = reference("user_id", UserTable.id)
    val shop_category_id = reference("shop_category_id", ShopCategoryTable.id)
    val shop_name = text("shop_name")
}

class ShopEntity(id: EntityID<String>) : BaseIntEntity(id, ShopTable) {
    companion object : BaseIntEntityClass<ShopEntity>(ShopTable)

    var user_id by ShopTable.user_id
    var shop_category_id by ShopTable.shop_category_id
    var shop_name by ShopTable.shop_name
    fun shopResponse() = Shop(id.value, shop_name)
}

data class Shop(val id: String, val shopName: String)