package com.example.controller

import com.example.entities.product.ProductCategoryEntity
import com.example.entities.product.ProductCategoryTable
import com.example.utils.CommonException
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class CategoryController {
    fun createProductCategory(productCategoryName: String) = transaction {
        val categoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.product_category_name eq productCategoryName }.toList()
                .singleOrNull()
        return@transaction if (categoryExist == null) {
            ProductCategoryEntity.new(UUID.randomUUID().toString()) {
                product_category_name = productCategoryName
            }.productCategoryResponse()
        } else {
            throw CommonException("Product category name $productCategoryName already exist")
        }
    }
}