package com.piashcse.controller

import com.piashcse.entities.product.BrandEntity
import com.piashcse.entities.product.BrandTable
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException
import com.piashcse.utils.extension.query

class BrandController {
    suspend fun addBrand(brandName: String) = query {
        val brandExist = BrandEntity.find { BrandTable.brandName eq brandName }.toList().singleOrNull()
        if (brandExist == null) {
            BrandEntity.new {
                this.brandName = brandName
            }.brandResponse()
        } else {
            brandName.alreadyExistException()
        }
    }

    suspend fun getBrand(limit: Int, offset: Long) = query {
        val brands = BrandEntity.all().limit(limit, offset)
        brands.map {
            it.brandResponse()
        }
    }

    suspend fun updateBrand(brandId: String, brandName:String) = query {
        val isBrandExist = BrandEntity.find { BrandTable.id eq brandId }.toList().singleOrNull()
        isBrandExist?.let {
            it.brandName = brandName
            // return category response
            it.brandResponse()
        } ?: brandId.isNotExistException()

    }

    suspend fun deleteBrand(brandId: String) = query {
        val isBrandExist = BrandEntity.find { BrandTable.id eq brandId }.toList().singleOrNull()
        isBrandExist?.let {
            it.delete()
            brandId
        } ?: brandId.isNotExistException()
    }
}