package com.example.entities.product.defaultvariant

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProductColorTable : IdTable<String>("product_color") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()
    val name = text("name")
    override val primaryKey = PrimaryKey(id)
}

class ProductColorEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ProductColorEntity>(ProductColorTable)

    var name by ProductColorTable.name
    fun response() = ProductColor(id.value, name)
}

data class ProductColor(val id: String, val color: String)