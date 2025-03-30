package com.piashcse.entities

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ShopCategoryTable : BaseIntIdTable("shop_category") {
    val name = text("name")
}

class ShopCategoryEntity(id: EntityID<String>) : BaseIntEntity(id, ShopCategoryTable) {
    companion object : BaseIntEntityClass<ShopCategoryEntity>(ShopCategoryTable)

    var name by ShopCategoryTable.name
    fun response() = ShopCategory(id.value, name)
}

data class ShopCategory(val id: String, val name: String)
