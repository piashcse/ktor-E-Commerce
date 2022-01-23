package com.example.entities.product.defaultproductcategory

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProductSubCategoryTable : IdTable<String>("product_sub_category") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()
    val product_sub_category_name = text("product_category_name")
    val product_category_id = reference("product_category_id", ProductCategoryTable.id)
    override val primaryKey = PrimaryKey(id)
}

class ProductSubCategoryEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ProductSubCategoryEntity>(ProductSubCategoryTable)

    var productSubCategoryName by ProductSubCategoryTable.product_sub_category_name
    var product_category_id by ProductSubCategoryTable.product_category_id
}