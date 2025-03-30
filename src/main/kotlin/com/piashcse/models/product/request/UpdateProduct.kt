package com.piashcse.models.product.request

data class UpdateProduct(
    val categoryId: String?,
    val subCategoryId: String?,
    val brandId: String?,
    val name: String?,
    val productCode: String?,
    val productQuantity: Int?,
    val detail: String,
    val price: Double?,
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
