package com.example.entities.product

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProductCategoryTable : IdTable<String>("product_category") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()
    val user_id = text("")
    val product_category_name = text("product_category_name")
    override val primaryKey = PrimaryKey(id)
}

class ProductCategoryEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ProductCategoryEntity>(ProductCategoryTable)
    var product_category_name by ProductCategoryTable.product_category_name
    fun productCategoryResponse() = ProductCategoryResponse(id.value, product_category_name)
}

data class ProductCategoryResponse(val id: String, val productCategoryName: String)