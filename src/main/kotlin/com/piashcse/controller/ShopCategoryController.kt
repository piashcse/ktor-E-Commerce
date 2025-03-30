package com.piashcse.controller

import com.piashcse.entities.ShopCategory
import com.piashcse.entities.ShopCategoryEntity
import com.piashcse.entities.ShopCategoryTable
import com.piashcse.repository.ShopCategoryRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query

/**
 * Controller for managing shop categories. Provides methods to create, retrieve, update, and delete categories.
 */
class ShopCategoryController : ShopCategoryRepo {

    /**
     * Creates a new shop category. If a category with the same name already exists, an exception is thrown.
     *
     * @param categoryName The name of the category to be created.
     * @return The created shop category.
     * @throws alreadyExistException If a category with the same name already exists.
     */
    override suspend fun createCategory(categoryName: String): ShopCategory = query {
        val isExistShopCategory =
            ShopCategoryEntity.find { ShopCategoryTable.categoryName eq categoryName }.toList().singleOrNull()
        isExistShopCategory?.let {
            throw categoryName.alreadyExistException()
        } ?: ShopCategoryEntity.new {
            this.categoryName = categoryName
        }.response()
    }

    /**
     * Retrieves a list of shop categories with a specified limit on the number of categories.
     *
     * @param limit The maximum number of categories to retrieve.
     * @return A list of shop categories.
     */
    override suspend fun getCategories(limit: Int): List<ShopCategory> = query {
        val shopCategories = ShopCategoryEntity.all().limit(limit)
        shopCategories.map {
            it.response()
        }
    }

    /**
     * Updates an existing shop category's name. If the category does not exist, an exception is thrown.
     *
     * @param categoryId The ID of the category to be updated.
     * @param categoryName The new name of the category.
     * @return The updated shop category.
     * @throws categoryId.notFoundException If the category with the specified ID is not found.
     */
    override suspend fun updateCategory(categoryId: String, categoryName: String): ShopCategory = query {
        val isShopCategoryExist =
            ShopCategoryEntity.find { ShopCategoryTable.id eq categoryId }.toList().singleOrNull()
        isShopCategoryExist?.let {
            it.categoryName = categoryName
            it.response()
        } ?: throw categoryId.notFoundException()
    }

    /**
     * Deletes a shop category. If the category does not exist, an exception is thrown.
     *
     * @param categoryId The ID of the category to be deleted.
     * @return The ID of the deleted category.
     * @throws categoryId.notFoundException If the category with the specified ID is not found.
     */
    override suspend fun deleteCategory(categoryId: String): String = query {
        val shopCategoryExist =
            ShopCategoryEntity.find { ShopCategoryTable.id eq categoryId }.toList().singleOrNull()
        shopCategoryExist?.let {
            it.delete()
            categoryId
        } ?: throw categoryId.notFoundException()
    }
}