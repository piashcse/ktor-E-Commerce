package com.piashcse.feature.product_sub_category

import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductCategoryTable
import com.piashcse.database.entities.ProductSubCategoryDAO
import com.piashcse.database.entities.ProductSubCategoryTable
import com.piashcse.model.request.ProductSubCategoryRequest
import com.piashcse.model.response.ProductSubCategory
import com.piashcse.utils.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.throwConflict
import com.piashcse.utils.throwNotFound
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Controller for managing product subcategories. Provides methods to add, update, retrieve, and delete product subcategories.
 */
class ProductSubCategoryService : ProductSubCategoryRepository {

    /**
     * Adds a new product subcategory to the system.
     *
     * @param productSubCategory The details of the product subcategory to be added.
     * @return The added product subcategory.
     * @throws productSubCategory.subCategoryName.throwConflict("Resource") If a subcategory with the same name already exists.
     * @throws productSubCategory.categoryId.throwNotFound("Resource") If the provided category ID does not exist.
     */
    override suspend fun addProductSubCategory(productSubCategory: ProductSubCategoryRequest): ProductSubCategory =
        query {
            val isCategoryIdExist =
                ProductCategoryDAO.find { ProductCategoryTable.id eq productSubCategory.categoryId }.toList()
                    .singleOrNull()
            isCategoryIdExist?.let {
                val isSubCategoryExist =
                    ProductSubCategoryDAO.find {
                        ProductSubCategoryTable.name eq productSubCategory.name
                    }
                        .toList()
                        .singleOrNull()
                isSubCategoryExist?.let {
                    throw productSubCategory.name.throwConflict("Subcategory")
                } ?: ProductSubCategoryDAO.new {
                    categoryId = EntityID(productSubCategory.categoryId, ProductSubCategoryTable)
                    name = productSubCategory.name
                }.response()
            } ?: productSubCategory.categoryId.throwNotFound("Category")
        }

    /**
     * Retrieves a list of product sub-categories for a given category with a limit on the number of sub-categories returned.
     *
     * @param categoryId The ID of the parent category for which sub-categories are to be retrieved.
     * @param limit The maximum number of sub-categories to retrieve.
     * @return A list of product sub-category entities.
     */
    override suspend fun getProductSubCategory(
            categoryId: String,
            limit: Int,
            offset: Int
    ): PaginatedResponse<ProductSubCategory> = query {
        ProductSubCategoryTable.selectAll().andWhere { ProductSubCategoryTable.categoryId eq categoryId }
            .toPaginatedResponse(limit, offset) {
                ProductSubCategoryDAO.wrapRow(it).response()
            }
    }

    /**
     * Updates the name of a product subcategory.
     *
     * @param id The ID of the product subcategory to update.
     * @param subCategoryName The new name for the subcategory.
     * @return The updated product subcategory.
     * @throws id.throwNotFound("Resource") If the subcategory ID does not exist.
     */
    override suspend fun updateProductSubCategory(id: String, name: String): ProductSubCategory = query {
        val suCategoryExist =
            ProductSubCategoryDAO.find { ProductSubCategoryTable.id eq id }
                .toList().singleOrNull()
        suCategoryExist?.let {
            it.name = name
            it.response()
        } ?: id.throwNotFound("Subcategory")
    }

    /**
     * Deletes a product subcategory.
     *
     * @param subCategoryId The ID of the product subcategory to delete.
     * @return The ID of the deleted subcategory.
     * @throws subCategoryId.throwNotFound("Resource") If the subcategory ID does not exist.
     */
    override suspend fun deleteProductSubCategory(subCategoryId: String): String = query {
        val isSubCategoryExist =
            ProductSubCategoryDAO.find { ProductSubCategoryTable.id eq subCategoryId }.toList().singleOrNull()
        isSubCategoryExist?.let {
            isSubCategoryExist.delete()
            subCategoryId
        } ?: subCategoryId.throwNotFound("Subcategory")
    }
}