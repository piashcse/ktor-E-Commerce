package com.example.models.product

import org.valiktor.functions.*
import org.valiktor.validate

data class AddProduct(
    val categoryId: String,
    val title: String,
    val description: String,
    val imageId: String,
    val price: Double,
    val discountPrice: Double?,
    val quantity: Int,
    val shopId: String,
    val color: String?,
    val size: String?,
) {
    fun validation() {
        validate(this) {
            validate(AddProduct::categoryId).isNotNull()
            validate(AddProduct::title).isNotNull()
            validate(AddProduct::price).isGreaterThan(0.0)
        }
    }
}

/*data class AddProduct(val map: Map<String?, PartData>) {
    val categoryId: PartData.FormItem by map
    val title: PartData.FormItem by map
    val description: PartData.FormItem by map
    val price: PartData.FormItem by map
    val discountPrice: PartData.FormItem by map
    val quantity: PartData.FormItem by map
    val shopId: PartData.FormItem by map
    val color: PartData.FormItem by map
    val size: PartData.FormItem by map
    fun validation() {
        validate(this) {
            validate(AddProduct::categoryId).isNotNull()
            validate(AddProduct::title).isNotNull()
            validate(AddProduct::price).isNotNull()
        }
    }
}*/
