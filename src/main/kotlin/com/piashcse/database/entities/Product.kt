package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ProductTable : BaseIntIdTable("product") {
    val userId = reference("user_id", UserTable.id)
    val name = text("name")
    val description = text("description")
    val categoryId = reference("category_id", ProductCategoryTable.id)
    val subCategoryId = reference("sub_category_id", ProductSubCategoryTable.id).nullable()
    val brandId = reference("brand_id", BrandTable.id).nullable()
    val stockQuantity = integer("stock_quantity") // Number of products in stock
    val minOrderQuantity = integer("min_order_quantity").default(1) // Minimum quantity required for purchase
    val price = double("price")
    val discountPrice = double("discount_price").nullable()
    val videoLink = text("video_link").nullable()
    val hotDeal = bool("hot_deal").default(false) // Whether it's a hot deal or not
    val featured = bool("featured").default(false) // Whether the product is featured or not
    val images = varchar("images", 1000) // Comma-separated image URLs for the product
    val status = enumerationByName("status", 50, ProductStatus::class).default(ProductStatus.ACTIVE) // Product status

    enum class ProductStatus {
        ACTIVE, // Product is available for purchase
        OUT_OF_STOCK // Product is not available
    }

}

class ProductDAO(id: EntityID<String>) : BaseIntEntity(id, ProductTable) {
    companion object : BaseIntEntityClass<ProductDAO>(ProductTable)

    var userId by ProductTable.userId
    var categoryId by ProductTable.categoryId
    var subCategoryId by ProductTable.subCategoryId
    var brandId by ProductTable.brandId
    var name by ProductTable.name
    var description by ProductTable.description
    var minOrderQuantity by ProductTable.minOrderQuantity
    var stockQuantity by ProductTable.stockQuantity
    var price by ProductTable.price
    var discountPrice by ProductTable.discountPrice
    var videoLink by ProductTable.videoLink
    var hotDeal by ProductTable.hotDeal
    var featured by ProductTable.featured
    var images by ProductTable.images
    var status by ProductTable.status
    fun response() = Product(
        id.value,
        categoryId.value,
        subCategoryId?.value,
        brandId?.value,
        name,
        description,
        minOrderQuantity,
        stockQuantity,
        price,
        discountPrice,
        videoLink,
        hotDeal,
        featured,
        images,
        status
    )
}

data class Product(
    val id: String,
    val categoryId: String,
    val subCategoryId: String?,
    val brandId: String?,
    val name: String,
    val description: String,
    val minOrderQuantity: Int,
    val stockQuantity: Int,
    val price: Double,
    val discountPrice: Double?,
    val videoLink: String?,
    val hotDeal: Boolean?,
    val featured: Boolean,
    val images: String,
    val status: ProductTable.ProductStatus
)