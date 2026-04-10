package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isValid
import org.valiktor.validate

@Serializable
data class UpdateProductRequest(
    val categoryId: String?,
    val subCategoryId: String?,
    val brandId: String?,
    val name: String?,
    val description: String?,
    val stockQuantity: Int?,
    val price: Double?,
    val discountPrice: Double?,
    val status: String?,
    val videoLink: String?,
    val hotDeal: Boolean?,
    val featured: Boolean?,
    val freeShipping: Boolean?,
    val images: List<String> = emptyList(),
) {
    fun validation() {
        validate(this) {
            // Only validate non-null values (partial update)
            price?.let {
                validate(UpdateProductRequest::price).isValid { it > 0 }
            }
            stockQuantity?.let {
                validate(UpdateProductRequest::stockQuantity).isValid { it >= 0 }
            }
            discountPrice?.let { p ->
                price?.let { pr ->
                    validate(UpdateProductRequest::discountPrice).isValid { it <= pr }
                }
            }
        }
    }
}
