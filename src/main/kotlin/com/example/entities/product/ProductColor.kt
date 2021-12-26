package com.example.entities.product

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProductColorTable : IdTable<String>("product_color") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()
    val color_name = text("color_name")
    override val primaryKey = PrimaryKey(id)
}
class ProductColorEntity(id: EntityID<String>) : Entity<String>(id)  {
    companion object : EntityClass<String, ProductColorEntity>(ProductColorTable)
    var colorName by ProductColorTable.color_name
}