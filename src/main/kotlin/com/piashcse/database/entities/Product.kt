package com.piashcse.database.entities

import com.piashcse.constants.ProductStatus
import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import com.piashcse.model.response.Product
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal

object ProductTable : BaseIntIdTable("product") {
    val userId = reference("user_id", UserTable.id) // Original creator/seller
    val shopId = reference("shop_id", ShopTable.id).nullable() // Shop that currently sells this product
    val name = text("name")
    val description = text("description")
    val categoryId = reference("category_id", ProductCategoryTable.id)
    val subCategoryId = reference("sub_category_id", ProductSubCategoryTable.id).nullable()
    val brandId = reference("brand_id", BrandTable.id).nullable()
    val sku = varchar("sku", 100).uniqueIndex() // Stock Keeping Unit - unique identifier
    val barcode = varchar("barcode", 100).nullable().index() // Product barcode for tracking
    val weight = decimal("weight", 10, 3).nullable() // Weight in kg
    val dimensions = varchar("dimensions", 100).nullable() // Length x Width x Height in cm
    val minOrderQuantity = integer("min_order_quantity").default(1) // Minimum quantity required for purchase
    val price = decimal("price", 10, 2) // Using decimal for accurate monetary calculations
    val discountPrice = decimal("discount_price", 10, 2).nullable()
    val discountPercentage = decimal("discount_percentage", 5, 2).nullable() // Discount percentage
    val videoLink = text("video_link").nullable()
    val hotDeal = bool("hot_deal").default(false) // Whether it's a hot deal or not
    val featured = bool("featured").default(false) // Whether the product is featured or not
    val bestSeller = bool("best_seller").default(false) // Whether the product is a best seller
    val newProduct = bool("new_product").default(false) // Whether the product is newly added
    val freeShipping = bool("free_shipping").default(false) // Whether the product offers free shipping
    val images = varchar("images", 2000) // Comma-separated image URLs for the product - increased size
    val status = enumerationByName<ProductStatus>("status", 50).default(ProductStatus.ACTIVE) // Product status
    val viewCount = integer("view_count").default(0) // Number of times the product has been viewed
    val rating = decimal("rating", 3, 2).default(BigDecimal("0.00")) // Average rating
    val totalReviews = integer("total_reviews").default(0) // Total number of reviews
    val totalSales = integer("total_sales").default(0) // Total number of items sold
    val stockQuantity = integer("stock_quantity").default(0) // Current available stock quantity
    // createdAt and updatedAt are inherited from BaseIntIdTable
}

class ProductDAO(id: EntityID<String>) : BaseIntEntity(id, ProductTable) {
    companion object : BaseIntEntityClass<ProductDAO>(ProductTable, ProductDAO::class.java)

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
    var images by ProductTable.images
    var status by ProductTable.status
    var viewCount by ProductTable.viewCount
    var rating by ProductTable.rating
    var totalReviews by ProductTable.totalReviews
    var totalSales by ProductTable.totalSales
    var stockQuantity by ProductTable.stockQuantity

    fun response() = Product(
        id = id.value,
        categoryId = categoryId.value,
        subCategoryId = subCategoryId?.value,
        brandId = brandId?.value,
        name = name,
        description = description,
        minOrderQuantity = minOrderQuantity,
        stockQuantity = stockQuantity,
        price = price.toDouble(),
        discountPrice = discountPrice?.toDouble(),
        videoLink = videoLink,
        hotDeal = hotDeal,
        featured = featured,
        images = images,
        status = status
    )
}