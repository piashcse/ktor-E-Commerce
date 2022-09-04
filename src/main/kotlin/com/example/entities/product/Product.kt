package com.example.entities.product

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import com.example.entities.product.defaultproductcategory.ProductCategoryTable
import org.jetbrains.exposed.dao.id.EntityID

object ProductTable : BaseIntIdTable("product") {
    val category_id = text("category_id").references(ProductCategoryTable.id)
    val title = text("title")
    val description = text("description")
    val price = double("price")
    val discount_price = text("discount_price").nullable()
}

class ProductEntity(id: EntityID<String>) : BaseIntEntity(id, ProductTable) {
    companion object : BaseIntEntityClass<ProductEntity>(ProductTable)

    var category_id by ProductTable.category_id
    var title by ProductTable.title
    var description by ProductTable.description
    var price by ProductTable.price
    var discountPrice by ProductTable.discount_price
    fun response() = Product(category_id, title, description, price, discountPrice)
}

data class Product(
    val categoryId: String,
    val title: String,
    val description: String,
    val price: Double,
    val discountPrice: String?,
)