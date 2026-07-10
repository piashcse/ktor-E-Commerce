package com.piashcse.feature.product_sub_category

import com.piashcse.model.request.ProductSubCategoryRequest
import com.piashcse.model.response.ProductSubCategoryResponse
import com.piashcse.utils.common.PaginatedResponse

class ProductSubCategoryService(private val repo: ProductSubCategoryRepository) : ProductSubCategoryRepository by repo
