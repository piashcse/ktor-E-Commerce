package com.piashcse.controller

import com.piashcse.dbhelper.query
import com.piashcse.entities.product.BrandEntity
import com.piashcse.entities.product.BrandTable
import com.piashcse.models.PagingData
import com.piashcse.models.bands.AddBrand
import com.piashcse.models.bands.DeleteBrand
import com.piashcse.models.bands.UpdateBrand
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException
import org.jetbrains.exposed.sql.transactions.transaction

class BrandController {
    suspend fun createBrand(addBand: AddBrand) = query {
        val brandExist = BrandEntity.find { BrandTable.brandName eq addBand.brandName }.toList().singleOrNull()
         if (brandExist == null) {
            BrandEntity.new {
                brandName = addBand.brandName
            }.brandResponse()
        } else {
            addBand.brandName.alreadyExistException()
        }
    }

    suspend fun getBrand(paging: PagingData) = query {
        val brands = BrandEntity.all().limit(paging.limit, paging.offset)
        brands.map {
            it.brandResponse()
        }
    }

    suspend fun updateBrand(updateBrand: UpdateBrand) = query {
        val isBrandExist = BrandEntity.find { BrandTable.id eq updateBrand.brandId }.toList().singleOrNull()
        isBrandExist?.let {
            it.brandName = updateBrand.brandName
            // return category response
            it.brandResponse()
        } ?: updateBrand.brandId.isNotExistException()

    }

   suspend fun deleteBrand(deleteBrand: DeleteBrand) = query {
        val isBrandExist = BrandEntity.find { BrandTable.id eq deleteBrand.brandId }.toList().singleOrNull()
        isBrandExist?.let {
            it.delete()
            deleteBrand.brandId
        } ?: deleteBrand.brandId.isNotExistException()
    }
}