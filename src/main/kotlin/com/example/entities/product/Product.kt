package com.example.entities.product

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProductTable : IdTable<String>("product") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()
    val subcategory_id = text("subcategory_id").references(ProductSubCategoryTable.id)
    val title = text("title")
    val description = text("description")
    val color_id = text("color_id").references(ProductColorTable.id)
    val size_id = text("size_id").references(ProductSizeTable.id)
    val price = text("price")
    val discount_price = text("discount_price")
    val discount_percent = text("discount_percent")
    override val primaryKey = PrimaryKey(id)
}
class ProductTableEntity(id: EntityID<String>) : Entity<String>(id)  {
    companion object : EntityClass<String, ProductTableEntity>(ProductTable)
    var subCategoryId by ProductTable.subcategory_id
    var title by ProductTable.title
    var description by ProductTable.description
    var colorId by ProductTable.color_id
    var sizeId by ProductTable.size_id
    var price by ProductTable.price
    var discountPrice by ProductTable.discount_price
    var discountPercentage by ProductTable.discount_percent
}