package com.piashcse.entities.product

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import com.piashcse.entities.user.UserTable
import org.jetbrains.exposed.dao.id.EntityID

object ProductImageTable : BaseIntIdTable("product_image") {
    val userId = reference("user_id", UserTable.id)
    val productId = reference("product_id", ProductTable.id)
    val imageUrl = text("image_url") // multiple image will be saved comma seperated string
}

class ProductImageEntity(id: EntityID<String>) : BaseIntEntity(id, ProductImageTable) {
    companion object : BaseIntEntityClass<ProductImageEntity>(ProductImageTable)

    var userId by ProductImageTable.userId
    var productId by ProductImageTable.productId
    var imageUrl by ProductImageTable.imageUrl
    fun response() = ImageUrl(id.value, imageUrl)
}

data class ImageUrl(
    val id: String,
    val imageUrl: String,
)