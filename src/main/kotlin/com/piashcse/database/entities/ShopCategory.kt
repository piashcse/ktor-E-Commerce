package com.piashcse.database.entities


import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ShopCategoryTable : BaseIntIdTable("shop_category") {
    val name = text("name")
}

class ShopCategoryDAO(id: EntityID<String>) : BaseIntEntity(id, ShopCategoryTable) {
    companion object : BaseIntEntityClass<ShopCategoryDAO>(ShopCategoryTable)

    var name by ShopCategoryTable.name
    fun response() = ShopCategory(id.value, name)
}

data class ShopCategory(val id: String, val name: String)
