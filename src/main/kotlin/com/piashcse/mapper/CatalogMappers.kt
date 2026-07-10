package com.piashcse.mapper

import com.piashcse.database.entities.BrandDAO
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductSubCategoryDAO
import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.model.response.BrandResponse
import com.piashcse.model.response.ProductCategoryResponse
import com.piashcse.model.response.ProductSubCategoryResponse
import com.piashcse.model.response.ShopCategoryResponse

fun BrandDAO.toBrandResponse() = BrandResponse(id.value, name, logo)

fun ProductCategoryDAO.toProductCategoryResponse() = ProductCategoryResponse(
    id.value, name,
    subCategories.map { it.toProductSubCategoryResponse() }, image,
)

fun ProductSubCategoryDAO.toProductSubCategoryResponse() = ProductSubCategoryResponse(
    id.value, categoryId.value, name, image,
)

fun ShopCategoryDAO.toShopCategoryResponse() = ShopCategoryResponse(id.value, name)
