package com.example.entities.product

import com.example.entities.product.defaultproductcategory.ProductCategoryTable
import com.example.entities.product.defaultvariant.ProductColorTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProductVariantOptionTable : IdTable<String>("product_variant_option") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()
    val product_variant_id = ProductCategoryTable.reference("product_variant_id", ProductVariantTable.id)
    val name = text("name")
    override val primaryKey = PrimaryKey(id)
}

class ProductVariantOptionEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ProductVariantOptionEntity>(ProductColorTable)

    var product_variant_id by ProductVariantOptionTable.product_variant_id
    var name by ProductVariantTable.name
}