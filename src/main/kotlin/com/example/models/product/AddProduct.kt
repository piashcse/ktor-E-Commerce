package com.example.models.product

import org.valiktor.functions.*
import org.valiktor.validate

data class AddProduct(
    val categoryId: String,
    val title: String,
    val description: String,
    val price: Int,
    val color: String?,
    val size: String?,
) {
    fun validation() {
        validate(this) {
            validate(AddProduct::categoryId).isNotNull()
            validate(AddProduct::title).isNotNull()
            validate(AddProduct::price).isGreaterThan(10).isLessThan(15)
        }
    }
}

