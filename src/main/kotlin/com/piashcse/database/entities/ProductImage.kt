package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ProductImageTable : BaseIdTable("product_image") {
    val productId = reference("product_id", ProductTable.id)
    val imageUrl = varchar("image_url", 500)
    val sortOrder = integer("sort_order").default(0)
}

class ProductImageDAO(id: EntityID<String>) : BaseEntity(id, ProductImageTable) {
    companion object : BaseEntityClass<ProductImageDAO>(ProductImageTable, ProductImageDAO::class.java)

    var productId by ProductImageTable.productId
    var imageUrl by ProductImageTable.imageUrl
    var sortOrder by ProductImageTable.sortOrder

    fun toUrl() = imageUrl
}
