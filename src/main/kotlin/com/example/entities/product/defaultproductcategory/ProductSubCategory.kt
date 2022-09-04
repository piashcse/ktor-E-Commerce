package com.example.entities.product.defaultproductcategory

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProductSubCategoryTable : BaseIntIdTable("product_sub_category") {
    val product_sub_category_name = text("product_category_name")
    val product_category_id = reference("product_category_id", ProductCategoryTable.id)
}

class ProductSubCategoryEntity(id: EntityID<String>) : BaseIntEntity(id, ProductSubCategoryTable) {
    companion object : BaseIntEntityClass<ProductSubCategoryEntity>(ProductSubCategoryTable)

    var productSubCategoryName by ProductSubCategoryTable.product_sub_category_name
    var product_category_id by ProductSubCategoryTable.product_category_id
}