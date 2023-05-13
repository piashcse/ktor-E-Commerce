package com.piashcse.entities.shop

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ShopCategoryTable : BaseIntIdTable("shop_category") {
    val shopCategoryName = text("shop_category_name")
}

class ShopCategoryEntity(id: EntityID<String>) : BaseIntEntity(id,ShopCategoryTable) {
    companion object : BaseIntEntityClass< ShopCategoryEntity>(ShopCategoryTable)

    var shopCategoryName by ShopCategoryTable.shopCategoryName
    fun shopCategoryResponse() = ShopCategory(id.value, shopCategoryName)
}

data class ShopCategory(val id: String, val shopName: String)
