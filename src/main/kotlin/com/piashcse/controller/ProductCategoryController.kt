package com.piashcse.controller

import com.piashcse.entities.ProductCategory
import com.piashcse.entities.ProductCategoryEntity
import com.piashcse.entities.ProductCategoryTable
import com.piashcse.repository.ProductCategoryRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query

/**
 * Controller for managing product categories.
 */
class ProductCategoryController : ProductCategoryRepo {

    /**
     * Creates a new product category with the given category name.
     *
     * @param categoryName The name of the category to be created.
     * @return The created product category entity.
     * @throws Exception if a category with the provided name already exists.
     */
    override suspend fun createCategory(categoryName: String): ProductCategory = query {
        val isCategoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.categoryName eq categoryName }.toList().singleOrNull()
        isCategoryExist?.let {
            throw categoryName.alreadyExistException()
        } ?: ProductCategoryEntity.new {
            this.categoryName = categoryName
        }.response()
    }

    /**
     * Retrieves the list of product categories with a limit on the number of categories returned.
     *
     * @param limit The maximum number of categories to retrieve.
     * @return A list of product category entities.
     */
    override suspend fun getCategories(limit: Int): List<ProductCategory> = query {
        val categories = ProductCategoryEntity.all().limit(limit)
        categories.map {
            it.response()
        }
    }

    /**
     * Updates the name of an existing product category.
     *
     * @param categoryId The ID of the category to update.
     * @param categoryName The new name for the category.
     * @return The updated product category entity.
     * @throws Exception if no category is found with the provided category ID.
     */
    override suspend fun updateCategory(categoryId: String, categoryName: String): ProductCategory = query {
        val isCategoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.id eq categoryId }.toList().singleOrNull()
        isCategoryExist?.let {
            it.categoryName = categoryName
            it.response()
        } ?: throw categoryId.notFoundException()
    }

    /**
     * Deletes an existing product category by its ID.
     *
     * @param categoryId The ID of the category to delete.
     * @return The ID of the deleted category.
     * @throws Exception if no category is found with the provided category ID.
     */
    override suspend fun deleteCategory(categoryId: String): String = query {
        val isCategoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.id eq categoryId }.toList().singleOrNull()
        isCategoryExist?.let {
            isCategoryExist.delete()
            categoryId
        } ?: throw categoryId.notFoundException()
    }
}