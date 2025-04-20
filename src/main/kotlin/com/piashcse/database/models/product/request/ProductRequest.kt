package com.piashcse.database.models.product.request

import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ProductRequest(
    val categoryId: String,
    val subCategoryId: String?,
    val brandId: String?,
    val name: String,
    val description: String,
    val productCode: String?,
    val stockQuantity: Int,
    val price: Double,
    val discountPrice: Double?,
    val status: Int?,
    val videoLink: String?,
    val hotDeal: Boolean,
    val featured: Boolean,
    val images: List<String>,
) {
    fun validation() {
        validate(this) {
            validate(ProductRequest::categoryId).isNotNull().isNotEmpty()
            validate(ProductRequest::name).isNotNull().isNotEmpty()
            validate(ProductRequest::description).isNotNull().isNotEmpty()
            validate(ProductRequest::price).isNotNull().isGreaterThan(0.0)
            validate(ProductRequest::stockQuantity).isNotNull().isGreaterThan(0)
        }
    }
}
