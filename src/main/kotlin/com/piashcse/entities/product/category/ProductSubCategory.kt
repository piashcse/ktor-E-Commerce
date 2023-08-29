package com.piashcse.entities.product.category

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ProductSubCategoryTable : BaseIntIdTable("sub_category") {
    val categoryId = reference("category_id", ProductCategoryTable.id)
    val subCategoryName = text("sub_category_name")
    val image = text("image").nullable()
}

class ProductSubCategoryEntity(id: EntityID<String>) : BaseIntEntity(id, ProductSubCategoryTable) {
    companion object : BaseIntEntityClass<ProductSubCategoryEntity>(ProductSubCategoryTable)

    var categoryId by ProductSubCategoryTable.categoryId
    var subCategoryName by ProductSubCategoryTable.subCategoryName
    var image by ProductSubCategoryTable.image
    fun response() = ProductSubCategoryResponse(id.value, categoryId.value, subCategoryName, image)
}

data class ProductSubCategoryResponse(val id: String, val categoryId: String, val subCategoryName: String, val image: String?)