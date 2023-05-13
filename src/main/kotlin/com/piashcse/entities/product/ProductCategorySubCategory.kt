package com.piashcse.entities.product

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ProductCategorySubCategoryTable : BaseIntIdTable("Product_category_sub_category") {
    val productId = varchar("product_id", 50).references(ProductTable.id)
    val categoryId = varchar("category_id", 50)
    val subCategoryId = varchar("sub_category_id", 50).nullable()
}

class ProductCategorySubCategoryEntity(id: EntityID<String>) : BaseIntEntity(id, ProductCategorySubCategoryTable) {
    companion object : BaseIntEntityClass<ProductEntity>(ProductCategorySubCategoryTable)

    var productId by ProductCategorySubCategoryTable.productId
    var categoryId by ProductCategorySubCategoryTable.categoryId
    var subCategoryId by ProductCategorySubCategoryTable.subCategoryId
    fun response() = ProductCategorySubCategory(productId, categoryId, subCategoryId)
}

data class ProductCategorySubCategory(
    val productId: String, val categoryId: String, val subCategoryId: String?
)