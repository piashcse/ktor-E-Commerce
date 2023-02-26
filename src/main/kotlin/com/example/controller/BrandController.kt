package com.example.controller

import com.example.entities.category.CategoryEntity
import com.example.entities.category.CategoryTable
import com.example.entities.product.BrandEntity
import com.example.entities.product.BrandTable
import com.example.models.PagingData
import com.example.models.bands.AddBrand
import com.example.models.bands.DeleteBrand
import com.example.models.bands.UpdateBrand
import com.example.utils.CommonException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class BrandController {
    fun createBrand(addBand: AddBrand) = transaction {
        val brandExist = BrandEntity.find { BrandTable.brandName eq addBand.brandName }.toList().singleOrNull()
        return@transaction if (brandExist == null) {
            BrandEntity.new {
                brandName = addBand.brandName
            }.brandResponse()
        } else {
            throw CommonException("${addBand.brandName} already exist")
        }
    }

    fun getBrand(paging: PagingData) = transaction {
        val brands = BrandEntity.all().limit(paging.limit, paging.offset)
        return@transaction brands.map {
            it.brandResponse()
        }
    }

    fun updateBrand(updateBrand: UpdateBrand) = transaction {
        val isBrandExist = BrandEntity.find { BrandTable.id eq updateBrand.brandId }.toList().singleOrNull()
        isBrandExist?.let {
            it.brandName = updateBrand.brandName
            // return category response
            it.brandResponse()
        } ?: run {
            throw CommonException("Brand not  exist")
        }
    }

    fun deleteBrand(deleteBrand: DeleteBrand) = transaction {
        val isBrandExist = BrandEntity.find { BrandTable.id eq deleteBrand.brandId }.toList().singleOrNull()
        isBrandExist?.delete()
    }
}