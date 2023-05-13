package com.piashcse.entities.product.defaultproductcategory

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ProductSubCategoryTable : BaseIntIdTable("product_sub_category") {
    val productSubCategoryName = text("product_category_name")
    val productCategoryId = reference("product_category_id", ProductCategoryTable.id)
}

class ProductSubCategoryEntity(id: EntityID<String>) : BaseIntEntity(id, ProductSubCategoryTable) {
    companion object : BaseIntEntityClass<ProductSubCategoryEntity>(ProductSubCategoryTable)

    var productSubCategoryName by ProductSubCategoryTable.productSubCategoryName
    var productCategoryId by ProductSubCategoryTable.productCategoryId
}