package com.piashcse.entities

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ProductTable : BaseIntIdTable("product") {
    val userId = reference("user_id", UserTable.id)
    val name = text("name")
    val price = double("price")
    val detail = text("detail")
    val categoryId = reference("category_id", ProductCategoryTable.id)
    val subCategoryId = reference("sub_category_id", ProductSubCategoryTable.id).nullable()
    val brandId = reference("brand_id", BrandTable.id).nullable()
    val productCode = text("product_code").nullable()
    val productQuantity = integer("product_quantity")
    val discountPrice = double("discount_price").nullable()
    val status = integer("status").nullable()
    val videoLink = text("video_link").nullable()
    val mainSlider = text("main_slider").nullable()
    val hotDeal = text("hot_deal").nullable()
    val bestRated = text("best_rated").nullable()
    val midSlider = text("mid_slider").nullable()
    val hotNew = text("hot_new").nullable()
    val trend = text("trend").nullable()
    val buyOneGetOne = text("buy_one_get_one").nullable()
    val imageOne = text("image_one").nullable()
    val imageTwo = text("image_two").nullable()
}

class ProductEntity(id: EntityID<String>) : BaseIntEntity(id, ProductTable) {
    companion object : BaseIntEntityClass<ProductEntity>(ProductTable)

    var userId by ProductTable.userId
    var categoryId by ProductTable.categoryId
    var subCategoryId by ProductTable.subCategoryId
    var brandId by ProductTable.brandId
    var name by ProductTable.name
    var productCode by ProductTable.productCode
    var productQuantity by ProductTable.productQuantity
    var detail by ProductTable.detail
    var price by ProductTable.price
    var discountPrice by ProductTable.discountPrice
    var status by ProductTable.status
    var videoLink by ProductTable.videoLink
    var mainSlider by ProductTable.mainSlider
    var hotDeal by ProductTable.hotDeal
    var bestRated by ProductTable.bestRated
    var midSlider by ProductTable.midSlider
    var hotNew by ProductTable.hotNew
    var trend by ProductTable.trend
    var buyOneGetOne by ProductTable.buyOneGetOne
    var imageOne by ProductTable.imageOne
    var imageTwo by ProductTable.imageTwo
    fun response() = Product(
        id.value,
        categoryId.value,
        subCategoryId?.value,
        brandId?.value,
        name,
        productCode,
        productQuantity,
        detail,
        price,
        discountPrice,
        status,
        videoLink,
        mainSlider,
        hotDeal,
        bestRated,
        midSlider,
        hotNew,
        trend,
        buyOneGetOne,
        imageOne,
        imageTwo
    )
}

data class Product(
    val id: String,
    val categoryId: String,
    val subCategoryId: String?,
    val brandId: String?,
    val name: String,
    val productCode: String?,
    val productQuantity: Int,
    val detail: String,
    val price: Double,
    val discountPrice: Double?,
    val status: Int?,
    val videoLink: String?,
    val mainSlider: String?,
    val hotDeal: String?,
    val bestRated: String?,
    val midSlider: String?,
    val hotNew: String?,
    val trend: String?,
    val buyOneGetOne: String?,
    val imageOne: String?,
    val imageTwo: String?,
)