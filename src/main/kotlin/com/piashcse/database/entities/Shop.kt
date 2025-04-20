package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ShopTable : BaseIntIdTable("shop") {
    val userId = reference("user_id", UserTable.id)
    val categoryId = reference("category_id", ShopCategoryTable.id)
    val name = text("name")
}

class ShopDAO(id: EntityID<String>) : BaseIntEntity(id, ShopTable) {
    companion object : BaseIntEntityClass<ShopDAO>(ShopTable)

    var userId by ShopTable.userId
    var categoryId by ShopTable.categoryId
    var name by ShopTable.name
    fun shopResponse() = Shop(id.value, name, categoryId.value)
}

data class Shop(val id: String, val name: String, val categoryId: String)