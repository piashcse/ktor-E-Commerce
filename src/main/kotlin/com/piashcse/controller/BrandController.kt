package com.piashcse.controller

import com.piashcse.entities.Brand
import com.piashcse.entities.BrandEntity
import com.piashcse.entities.BrandTable
import com.piashcse.repository.BrandRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query

class BrandController : BrandRepo {
    override suspend fun addBrand(brandName: String): Brand = query {
        val isBrandExist = BrandEntity.find { BrandTable.brandName eq brandName }.toList().singleOrNull()
        isBrandExist?.let {
            throw brandName.alreadyExistException()

        } ?: BrandEntity.new {
            this.brandName = brandName
        }.response()
    }

    override suspend fun getBrands(limit: Int): List<Brand> = query {
        BrandEntity.all().limit(limit).map {
            it.response()
        }
    }

    override suspend fun updateBrand(brandId: String, brandName: String): Brand = query {
        val isBrandExist = BrandEntity.find { BrandTable.id eq brandId }.toList().singleOrNull()
        isBrandExist?.let {
            it.brandName = brandName
            it.response()
        } ?: throw brandId.notFoundException()

    }

    override suspend fun deleteBrand(brandId: String): String = query {
        val isBrandExist = BrandEntity.find { BrandTable.id eq brandId }.toList().singleOrNull()
        isBrandExist?.let {
            it.delete()
            brandId
        } ?: throw brandId.notFoundException()
    }
}