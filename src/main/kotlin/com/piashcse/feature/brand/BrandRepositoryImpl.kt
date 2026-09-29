package com.piashcse.feature.brand

import com.piashcse.database.entities.BrandDAO
import com.piashcse.database.entities.BrandTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.feature.common.CatalogCrud
import com.piashcse.mapper.toBrandResponse
import com.piashcse.model.response.BrandResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ConflictException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class BrandRepositoryImpl : BrandRepository {
    override suspend fun createBrand(name: String): BrandResponse =
        query {
            CatalogCrud.createUniqueByName(
                name = name,
                validationLabel = "Brand",
                conflictLabel = "BrandResponse",
                findExisting = { BrandDAO.find { BrandTable.name eq name }.firstOrNull() },
                create = {
                    BrandDAO.new {
                        this.name = name
                    }
                },
                toResponse = { it.toBrandResponse() },
            )
        }

    override suspend fun getBrands(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<BrandResponse> =
        query {
            BrandTable.selectAll().toPaginatedResponse(limit, offset) {
                BrandDAO.wrapRow(it).toBrandResponse()
            }
        }

    override suspend fun updateBrand(
        brandId: String,
        name: String,
    ): BrandResponse =
        query {
            CatalogCrud.renameById(
                id = brandId,
                name = name,
                validationLabel = "Brand",
                notFoundLabel = "BrandResponse",
                findById = { BrandDAO.findById(it) },
                rename = { brand, newName -> brand.name = newName },
                toResponse = { it.toBrandResponse() },
            )
        }

    override suspend fun deleteBrand(brandId: String): String =
        query {
            CatalogCrud.deleteById(
                id = brandId,
                notFoundLabel = "BrandResponse",
                findById = { BrandDAO.findById(it) },
                guard = {
                    // Guard vs ON DELETE SET NULL/CASCADE data loss: block while products still reference this brand.
                    if (!ProductDAO.find { ProductTable.brandId eq brandId }.empty()) {
                        throw ConflictException(
                            "Cannot delete brand: products still reference it. Reassign or soft-delete products first.",
                        )
                    }
                },
                delete = { it.delete() },
            )
        }
}
