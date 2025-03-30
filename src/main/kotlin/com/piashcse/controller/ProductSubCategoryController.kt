package com.piashcse.controller

import com.piashcse.entities.*
import com.piashcse.models.subcategory.ProductSubCategoryRequest
import com.piashcse.repository.ProductSubCategoryRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID

/**
 * Controller for managing product subcategories. Provides methods to add, update, retrieve, and delete product subcategories.
 */
class ProductSubCategoryController : ProductSubCategoryRepo {

    /**
     * Adds a new product subcategory to the system.
     *
     * @param productSubCategory The details of the product subcategory to be added.
     * @return The added product subcategory.
     * @throws productSubCategory.subCategoryName.alreadyExistException() If a subcategory with the same name already exists.
     * @throws productSubCategory.categoryId.notFoundException() If the provided category ID does not exist.
     */
    override suspend fun addProductSubCategory(productSubCategory: ProductSubCategoryRequest): ProductSubCategory = query {
        val isCategoryIdExist =
            ProductCategoryEntity.find { ProductCategoryTable.id eq productSubCategory.categoryId }.toList()
                .singleOrNull()
        isCategoryIdExist?.let {
            val isSubCategoryExist =
                ProductSubCategoryEntity.find { ProductSubCategoryTable.subCategoryName eq productSubCategory.subCategoryName }
                    .toList()
                    .singleOrNull()
            isSubCategoryExist?.let {
                throw productSubCategory.subCategoryName.alreadyExistException()
            } ?: ProductSubCategoryEntity.new {
                categoryId = EntityID(productSubCategory.categoryId, ProductSubCategoryTable)
                subCategoryName = productSubCategory.subCategoryName
            }.response()
        } ?: throw productSubCategory.categoryId.notFoundException()
    }

    /**
     * Retrieves product subcategories for a given category ID.
     *
     * @param categoryId The category ID for which to retrieve subcategories.
     * @param limit The maximum number of subcategories to retrieve.
     * @return A list of product subcategories for the given category.
     */
    override suspend fun getProductSubCategory(categoryId: String, limit: Int): List<ProductSubCategory> =
        query {
            val subCategoryExist = ProductSubCategoryEntity.find { ProductSubCategoryTable.categoryId eq categoryId }
                .limit(limit)
            subCategoryExist.map {
                it.response()
            }
        }

    /**
     * Updates the name of a product subcategory.
     *
     * @param id The ID of the product subcategory to update.
     * @param subCategoryName The new name for the subcategory.
     * @return The updated product subcategory.
     * @throws id.notFoundException() If the subcategory ID does not exist.
     */
    override suspend fun updateProductSubCategory(id: String, subCategoryName: String): ProductSubCategory = query {
        val suCategoryExist =
            ProductSubCategoryEntity.find { ProductSubCategoryTable.id eq id }
                .toList().singleOrNull()
        suCategoryExist?.let {
            it.subCategoryName = subCategoryName
            it.response()
        } ?: throw id.notFoundException()
    }

    /**
     * Deletes a product subcategory.
     *
     * @param subCategoryId The ID of the product subcategory to delete.
     * @return The ID of the deleted subcategory.
     * @throws subCategoryId.notFoundException() If the subcategory ID does not exist.
     */
    override suspend fun deleteProductSubCategory(subCategoryId: String): String = query {
        val isSubCategoryExist =
            ProductSubCategoryEntity.find { ProductSubCategoryTable.id eq subCategoryId }.toList().singleOrNull()
        isSubCategoryExist?.let {
            isSubCategoryExist.delete()
            subCategoryId
        } ?: throw subCategoryId.notFoundException()
    }
}