package com.piashcse.feature.shop_category

import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopCategoryTable
import com.piashcse.model.response.ShopCategoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwConflict
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Service for managing shop categories. Provides methods to create, retrieve, update, and delete categories.
 */
class ShopCategoryService : ShopCategoryRepository {
    /**
     * Creates a new shop category. If a category with the same name already exists, an exception is thrown.
     *
     * @param name The name of the category to be created.
     * @return The created shop category.
     * @throws alreadyExistException If a category with the same name already exists.
     */
    override suspend fun createCategory(name: String): ShopCategoryResponse =
        query {
            val isExistShopCategory =
                ShopCategoryDAO.find { ShopCategoryTable.name eq name }.firstOrNull()
            isExistShopCategory?.let {
                throw name.throwConflict("Category")
            } ?: ShopCategoryDAO.new {
                this.name = name
            }.response()
        }

    /**
     * Retrieves a list of shop categories with a specified limit on the number of categories.
     *
     * @param limit The maximum number of categories to retrieve.
     * @return A list of shop categories.
     */
    override suspend fun getCategories(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ShopCategoryResponse> =
        query {
            ShopCategoryTable.selectAll().toPaginatedResponse(limit, offset) {
                ShopCategoryDAO.wrapRow(it).response()
            }
        }

    /**
     * Updates an existing shop category's name. If the category does not exist, an exception is thrown.
     *
     * @param categoryId The ID of the category to be updated.
     * @param name The new name of the category.
     * @return The updated shop category.
     * @throws categoryId.notFoundException If the category with the specified ID is not found.
     */
    override suspend fun updateCategory(
        categoryId: String,
        name: String,
    ): ShopCategoryResponse =
        query {
            val isShopCategoryExist =
                ShopCategoryDAO.findById(categoryId)
            isShopCategoryExist?.let {
                it.name = name
                it.response()
            } ?: categoryId.throwNotFound("Category")
        }

    /**
     * Deletes a shop category. If the category does not exist, an exception is thrown.
     *
     * @param categoryId The ID of the category to be deleted.
     * @return The ID of the deleted category.
     * @throws categoryId.notFoundException If the category with the specified ID is not found.
     */
    override suspend fun deleteCategory(categoryId: String): String =
        query {
            val shopCategoryExist =
                ShopCategoryDAO.findById(categoryId)
            shopCategoryExist?.let {
                it.delete()
                categoryId
            } ?: categoryId.throwNotFound("Category")
        }
}
