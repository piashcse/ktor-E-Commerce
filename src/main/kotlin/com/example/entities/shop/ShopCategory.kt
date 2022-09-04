package com.example.entities.shop

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ShopCategoryTable : BaseIntIdTable("shop_category") {
    val shop_category_name = text("shop_category_name")
}

class ShopCategoryEntity(id: EntityID<String>) : BaseIntEntity(id,ShopCategoryTable) {
    companion object : BaseIntEntityClass< ShopCategoryEntity>(ShopCategoryTable)

    var shop_category_name by ShopCategoryTable.shop_category_name
    fun shopCategoryResponse() = ShopCategory(id.value, shop_category_name)
}

data class ShopCategory(val id: String, val shopName: String)
