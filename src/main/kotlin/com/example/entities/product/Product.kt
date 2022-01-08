package com.example.entities.product

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProductTable : IdTable<String>("product") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()
    val category_id = text("category_id").references(ProductSubCategoryTable.id)
    val title = text("title")
    val description = text("description")
    val price = text("price")
    val discount_price = text("discount_price").nullable()
    val discount_percent = text("discount_percent").nullable()
    override val primaryKey = PrimaryKey(id)
}

class ProductEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ProductEntity>(ProductTable)

    var category_id by ProductTable.category_id
    var title by ProductTable.title
    var description by ProductTable.description
    var price by ProductTable.price
    var discountPrice by ProductTable.discount_price
    var discountPercentage by ProductTable.discount_percent
    fun response() = Product(category_id, title, description, price, discountPrice, discountPercentage)
}

data class Product(
    val categoryId: String,
    val title: String,
    val description: String,
    val price: String,
    val discountPrice: String?,
    val discountPercentage: String?
)