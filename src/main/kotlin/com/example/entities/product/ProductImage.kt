package com.example.entities.product

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProductImage : IdTable<String>("product_image") {
    override val id: Column<EntityID<String>> = text("id").uniqueIndex().entityId()
    val product_id = text("product_id").references(ProductTable.id).nullable()
    val image_url = text("image_url") // multiple image will be saved comma seperated string
    override val primaryKey = PrimaryKey(id)
}

class ProductImageEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ProductImageEntity>(ProductImage)

    var product_id by ProductImage.product_id
    var image_url by ProductImage.image_url
    fun response() = ImageUrl(id.value, image_url)
}

data class ImageUrl(
    val id: String,
    val imageUrl: String,
)