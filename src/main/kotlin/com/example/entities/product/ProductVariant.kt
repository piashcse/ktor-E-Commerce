package com.example.entities.product

import com.example.entities.product.defaultvariant.ProductColorTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProductVariantTable : IdTable<String>("product_variant") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()
    val product_id = ProductCategoryTable.reference("product_id", ProductTable.id)
    val name = text("name")
    override val primaryKey = PrimaryKey(id)
}

class ProductVariantEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ProductVariantEntity>(ProductColorTable)

    var product_id by ProductVariantTable.product_id
    var name by ProductVariantTable.name
    fun response() = ProductVariant(id.value, name)
}

data class ProductVariant(val id: String, val variantName: String)