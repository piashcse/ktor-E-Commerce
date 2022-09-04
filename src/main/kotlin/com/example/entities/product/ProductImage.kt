package com.example.entities.product

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object ProductImage : BaseIntIdTable("product_image") {
    val product_id = text("product_id").references(ProductTable.id).nullable()
    val image_url = text("image_url") // multiple image will be saved comma seperated string
}

class ProductImageEntity(id: EntityID<String>) : BaseIntEntity(id,ProductImage ) {
    companion object : BaseIntEntityClass<ProductImageEntity>(ProductImage)

    var product_id by ProductImage.product_id
    var image_url by ProductImage.image_url
    fun response() = ImageUrl(id.value, image_url)
}

data class ImageUrl(
    val id: String,
    val imageUrl: String,
)