package com.piashcse.models.product.request

import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ProductRequest(
    val categoryId: String,
    val subCategoryId: String?,
    val brandId: String?,
    val productName: String,
    val productCode: String?,
    val productQuantity: Int,
    val productDetail: String,
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
) {
    fun validation() {
        validate(this) {
            validate(ProductRequest::categoryId).isNotNull().isNotEmpty()
            validate(ProductRequest::productName).isNotNull().isNotEmpty()
            validate(ProductRequest::productDetail).isNotNull().isNotEmpty()
            validate(ProductRequest::price).isNotNull().isGreaterThan(0.0)
            validate(ProductRequest::productQuantity).isNotNull().isGreaterThan(0)
        }
    }
}
