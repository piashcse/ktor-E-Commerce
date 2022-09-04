package com.example.entities.product

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object ProductCategorySubCategoryTable : BaseIntIdTable("Product_category_sub_category") {
    val product_id = varchar("product_id", 50).references(ProductTable.id)
    val category_id = varchar("category_id", 50)
    val sub_category_id = varchar("sub_category_id", 50).nullable()
}

class ProductCategorySubCategoryEntity(id: EntityID<String>) : BaseIntEntity(id,ProductCategorySubCategoryTable ) {
    companion object : BaseIntEntityClass<ProductEntity>(ProductCategorySubCategoryTable)

    var product_id by ProductCategorySubCategoryTable.product_id
    var category_id by ProductCategorySubCategoryTable.category_id
    var sub_category_id by ProductCategorySubCategoryTable.sub_category_id
    fun response() = ProductCategorySubCategory(product_id, category_id, sub_category_id)
}

data class ProductCategorySubCategory(
    val productId: String, val categoryId: String, val subCategoryId: String?
)