package com.piashcse.entities

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ProductSubCategoryTable : BaseIntIdTable("sub_category") {
    val categoryId = reference("category_id", ProductCategoryTable.id)
    val name = text("name")
    val image = text("image").nullable()
}

class ProductSubCategoryEntity(id: EntityID<String>) : BaseIntEntity(id, ProductSubCategoryTable) {
    companion object : BaseIntEntityClass<ProductSubCategoryEntity>(ProductSubCategoryTable)

    var categoryId by ProductSubCategoryTable.categoryId
    var name by ProductSubCategoryTable.name
    var image by ProductSubCategoryTable.image
    fun response() = ProductSubCategory(id.value, categoryId.value, name, image)
}

data class ProductSubCategory(val id: String, val categoryId: String, val name: String, val image: String?)