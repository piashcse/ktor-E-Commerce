package com.example.entities.shop

import com.example.entities.user.UsersTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ShopTable : IdTable<String>("shop") {
    override val id: Column<EntityID<String>> = ShopTable.text("id").uniqueIndex().entityId()
    val user_id = ShopTable.reference("user_id", UsersTable.id)
    val shop_category_id = ShopTable.reference("shop_category_id", ShopCategoryTable.id)
    val shop_name = text("shop_name")
    override val primaryKey = PrimaryKey(id)
}

class ShopEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ShopEntity>(ShopTable)
    var user_id by ShopTable.user_id
    var shop_category_id by ShopTable.shop_category_id
    var shop_name by ShopTable.shop_name
    fun shopResponse() = Shop(id.value, shop_name)
}

data class Shop(val id: String, val shopName: String)