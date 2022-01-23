package com.example.entities.product

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProductCategorySubCategoryTable : IdTable<String>("Product_category_sub_category") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()
    val product_id = text("product_id").references(ProductTable.id)
    val category_id = text("category_id")
    val sub_category_id = text("sub_category_id").nullable()
    override val primaryKey = PrimaryKey(id)
}

class ProductCategorySubCategoryEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ProductEntity>(ProductCategorySubCategoryTable)

    var product_id by ProductCategorySubCategoryTable.product_id
    var category_id by ProductCategorySubCategoryTable.category_id
    var sub_category_id by ProductCategorySubCategoryTable.sub_category_id
    fun response() = ProductCategorySubCategory(product_id, category_id, sub_category_id)
}

data class ProductCategorySubCategory(
    val productId: String, val categoryId: String, val subCategoryId: String?
)