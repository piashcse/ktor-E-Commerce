package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotZero
import org.valiktor.validate

@Serializable
data class ProductSearchRequest(
    val limit: Int,
    val name: String,
    val maxPrice: Double?,
    val minPrice: Double?,
    val categoryId: String?,
) {
    fun validation() {
        validate(this) {
            validate(ProductSearchRequest::limit).isNotNull().isNotZero()
            validate(ProductSearchRequest::name).isNotNull().isNotEmpty()
        }
    }
}