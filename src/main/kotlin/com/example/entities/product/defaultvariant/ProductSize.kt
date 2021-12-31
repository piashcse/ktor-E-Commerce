package com.example.entities.product.defaultvariant

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProductSizeTable : IdTable<String>("product_size") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()
    val name = text("name")
    override val primaryKey = PrimaryKey(id)
}

class ProductSizeEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ProductSizeEntity>(ProductSizeTable)

    var name by ProductSizeTable.name
    fun response() = ProductSize(id.value, name)
}

data class ProductSize(val id: String, val size: String)