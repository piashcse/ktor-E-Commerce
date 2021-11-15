package com.example.entities

import com.example.entities.ProductCategoryTable.entityId
import com.example.entities.ProductCategoryTable.uniqueIndex
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProductSizeTable : IdTable<String>("product_size") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()
    val size_name = text("size_name")
    override val primaryKey = PrimaryKey(id)
}
class ProductSizeEntity(id: EntityID<String>) : Entity<String>(id)  {
    companion object : EntityClass<String, ProductSizeEntity>(ProductSizeTable)
    var sizeName by ProductSizeTable.size_name
}