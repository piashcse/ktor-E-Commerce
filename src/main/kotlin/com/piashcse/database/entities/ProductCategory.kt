package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ProductCategoryTable : BaseIntIdTable("category") {
    val name = text("name")
    val image = text("image").nullable()
}

class ProductCategoryDAO(id: EntityID<String>) : BaseIntEntity(id, ProductCategoryTable) {
    companion object : BaseIntEntityClass<ProductCategoryDAO>(ProductCategoryTable)

    var name by ProductCategoryTable.name
    private val subCategories by ProductSubCategoryDAO referrersOn ProductSubCategoryTable.categoryId
    var image by ProductCategoryTable.image
    fun response() =
        ProductCategory(id.value, name, subCategories.map { it.response() }, image)
}

data class ProductCategory(
    val id: String,
    val name: String,
    val subCategories: List<ProductSubCategory>,
    val image: String?
)