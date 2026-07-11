package com.piashcse.database.entities

import com.piashcse.constants.ProductStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import java.math.BigDecimal

object ProductTable : BaseIdTable("product") {
    val userId = reference("user_id", UserTable.id).index()
    val shopId = reference("shop_id", ShopTable.id).nullable().index()
    val name = text("name")
    val description = text("description")
    val categoryId = reference("category_id", ProductCategoryTable.id).index()
    val subCategoryId = reference("sub_category_id", ProductSubCategoryTable.id).nullable()
    val brandId = reference("brand_id", BrandTable.id).nullable()
    val sku = varchar("sku", 100).uniqueIndex()
    val barcode = varchar("barcode", 100).nullable().index()
    val weight = decimal("weight", 10, 3).nullable()
    val dimensions = varchar("dimensions", 100).nullable()
    val minOrderQuantity = integer("min_order_quantity").default(1)
    val price = decimal("price", 10, 2)
    val discountPrice = decimal("discount_price", 10, 2).nullable()
    val discountPercentage = decimal("discount_percentage", 5, 2).nullable()
    val videoLink = text("video_link").nullable()
    val hotDeal = bool("hot_deal").default(false).index()
    val featured = bool("featured").default(false).index()
    val bestSeller = bool("best_seller").default(false).index()
    val newProduct = bool("new_product").default(false)
    val freeShipping = bool("free_shipping").default(false)
    val status = enumerationByName<ProductStatus>("status", 50).default(ProductStatus.ACTIVE).index()
    val viewCount = integer("view_count").default(0)
    val rating = decimal("rating", 3, 2).default(BigDecimal("0.00"))
    val totalReviews = integer("total_reviews").default(0)
    val totalSales = integer("total_sales").default(0)
}

class ProductDAO(id: EntityID<String>) : BaseEntity(id, ProductTable) {
    companion object : BaseEntityClass<ProductDAO>(ProductTable, ProductDAO::class.java)

    var userId by ProductTable.userId
    var shopId by ProductTable.shopId
    var categoryId by ProductTable.categoryId
    var subCategoryId by ProductTable.subCategoryId
    var brandId by ProductTable.brandId
    var name by ProductTable.name
    var description by ProductTable.description
    var sku by ProductTable.sku
    var barcode by ProductTable.barcode
    var weight by ProductTable.weight
    var dimensions by ProductTable.dimensions
    var minOrderQuantity by ProductTable.minOrderQuantity
    var price by ProductTable.price
    var discountPrice by ProductTable.discountPrice
    var discountPercentage by ProductTable.discountPercentage
    var videoLink by ProductTable.videoLink
    var hotDeal by ProductTable.hotDeal
    var featured by ProductTable.featured
    var bestSeller by ProductTable.bestSeller
    var newProduct by ProductTable.newProduct
    var freeShipping by ProductTable.freeShipping
    var status by ProductTable.status
    var viewCount by ProductTable.viewCount
    var rating by ProductTable.rating
    var totalReviews by ProductTable.totalReviews
    var totalSales by ProductTable.totalSales

    val imageUrls: List<String>
        get() = ProductImageDAO.find { ProductImageTable.productId eq id }
            .orderBy(ProductImageTable.sortOrder to SortOrder.ASC)
            .map { it.imageUrl }

    fun setImages(urls: List<String>) {
        ProductImageTable.deleteWhere { ProductImageTable.productId eq id }
        urls.forEachIndexed { index, url ->
            ProductImageDAO.new {
                productId = this@ProductDAO.id
                imageUrl = url
                sortOrder = index
            }
        }
    }

}
