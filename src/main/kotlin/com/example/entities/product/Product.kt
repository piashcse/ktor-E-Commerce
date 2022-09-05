package com.example.entities.product

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import com.example.entities.product.defaultproductcategory.ProductCategoryTable
import org.jetbrains.exposed.dao.id.EntityID

object ProductTable : BaseIntIdTable("product") {
    val categoryId = text("category_id").references(ProductCategoryTable.id)
    val title = text("title")
    val description = text("description")
    val price = double("price")
    val discountPrice = text("discount_price").nullable()
}

class ProductEntity(id: EntityID<String>) : BaseIntEntity(id, ProductTable) {
    companion object : BaseIntEntityClass<ProductEntity>(ProductTable)

    var categoryId by ProductTable.categoryId
    var title by ProductTable.title
    var description by ProductTable.description
    var price by ProductTable.price
    var discountPrice by ProductTable.discountPrice
    fun response() = Product(categoryId, title, description, price, discountPrice)
}

data class Product(
    val categoryId: String,
    val title: String,
    val description: String,
    val price: Double,
    val discountPrice: String?,
)