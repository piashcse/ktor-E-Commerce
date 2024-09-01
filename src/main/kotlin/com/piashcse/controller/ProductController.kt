package com.piashcse.controller

import com.piashcse.entities.product.*
import com.piashcse.models.product.request.*
import com.piashcse.utils.AppConstants
import com.piashcse.utils.extension.isNotExistException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.io.File

class ProductController {
    suspend fun addProduct(userId: String, addProduct: AddProduct) = query {
        ProductEntity.new {
            this.userId = EntityID(userId, ProductTable)
            categoryId = EntityID(addProduct.categoryId, ProductTable)
            subCategoryId = addProduct.subCategoryId?.let { EntityID(addProduct.subCategoryId, ProductTable) }
            brandId = addProduct.brandId?.let { EntityID(addProduct.brandId, ProductTable) }
            productName = addProduct.productName
            productCode = addProduct.productCode
            productQuantity = addProduct.productQuantity
            productDetail = addProduct.productDetail
            price = addProduct.price
            discountPrice = addProduct.discountPrice
            status = addProduct.status
            videoLink = addProduct.videoLink
            mainSlider = addProduct.mainSlider
            hotDeal = addProduct.hotDeal
            bestRated = addProduct.bestRated
            midSlider = addProduct.midSlider
            hotNew = addProduct.hotNew
            trend = addProduct.trend
            buyOneGetOne = addProduct.buyOneGetOne
            imageOne = addProduct.imageOne
            imageTwo = addProduct.imageTwo
        }.response()
    }

    suspend fun updateProduct(userId: String, productId: String, updateProduct: UpdateProduct) = query {
        val isProductExist =
            ProductEntity.find { ProductTable.userId eq userId and (ProductTable.id eq productId) }.toList()
                .singleOrNull()
        isProductExist?.apply {
            this.userId = EntityID(userId, ProductTable)
            categoryId =
                updateProduct.categoryId?.let { EntityID(updateProduct.categoryId, ProductTable) } ?: categoryId
            subCategoryId = updateProduct.subCategoryId?.let { EntityID(updateProduct.subCategoryId, ProductTable) }
                ?: subCategoryId
            brandId = updateProduct.brandId?.let { EntityID(updateProduct.brandId, ProductTable) } ?: brandId
            productName = updateProduct.productName ?: productName
            productCode = updateProduct.productCode ?: productCode
            productQuantity = updateProduct.productQuantity ?: productQuantity
            productDetail = updateProduct.productDetail ?: productDetail
            price = updateProduct.price ?: price
            discountPrice = updateProduct.discountPrice ?: discountPrice
            status = updateProduct.status ?: status
            videoLink = updateProduct.videoLink ?: videoLink
            mainSlider = updateProduct.mainSlider ?: mainSlider
            hotDeal = updateProduct.hotDeal ?: hotDeal
            bestRated = updateProduct.bestRated ?: bestRated
            midSlider = updateProduct.midSlider ?: midSlider
            hotNew = updateProduct.hotNew ?: hotNew
            trend = updateProduct.trend ?: trend
            buyOneGetOne = updateProduct.buyOneGetOne ?: buyOneGetOne
            imageOne = updateProduct.imageOne ?: imageOne
            imageTwo = updateProduct.imageTwo ?: imageTwo
        }?.response()
    }

    suspend fun getProduct(productQuery: ProductWithFilter) = query {
        val query = ProductTable.selectAll()
        productQuery.maxPrice?.let {
            query.andWhere { ProductTable.price lessEq it }
        }
        productQuery.minPrice?.let {
            query.andWhere {
                ProductTable.price greaterEq it
            }
        }
        productQuery.categoryId?.let {
            query.adjustWhere {
                ProductTable.categoryId eq it
            }
        }
        productQuery.subCategoryId?.let {
            query.adjustWhere {
                ProductTable.subCategoryId eq it
            }
        }
        productQuery.brandId?.let {
            query.adjustWhere {
                ProductTable.brandId eq it
            }
        }
        query.limit(productQuery.limit, productQuery.offset).map {
            ProductEntity.wrapRow(it).response()
        }
    }

    suspend fun getProductById(userId: String, productQuery: ProductWithFilter) = query {
        val query = ProductTable.selectAll()
        query.andWhere { ProductTable.userId eq userId }

        productQuery.maxPrice?.let {
            query.andWhere { ProductTable.price lessEq it }
        }
        productQuery.maxPrice?.let {
            query.andWhere { ProductTable.price lessEq it }
        }
        productQuery.minPrice?.let {
            query.andWhere {
                ProductTable.price greaterEq it
            }
        }
        productQuery.categoryId?.let {
            query.adjustWhere {
                ProductTable.categoryId eq it
            }
        }
        productQuery.subCategoryId?.let {
            query.adjustWhere {
                ProductTable.subCategoryId eq it
            }
        }
        productQuery.brandId?.let {
            query.adjustWhere {
                ProductTable.brandId eq it
            }
        }
        query.limit(productQuery.limit, productQuery.offset).map {
            ProductEntity.wrapRow(it).response()
        }
    }

    suspend fun productDetail(productId: String) = query {
        val isProductExist = ProductEntity.find { ProductTable.id eq productId }.toList().singleOrNull()
        isProductExist?.response()
    }

    suspend fun deleteProduct(userId: String, productId: String) = query {
        val isProductExist =
            ProductEntity.find { ProductTable.userId eq userId and (ProductTable.id eq productId) }
                .toList().singleOrNull()
        isProductExist?.let {
            it.delete()
            productId
        } ?: productId.isNotExistException()
    }

    suspend fun uploadProductImage(userId: String, productId: String, productImage: String) = query {
            val isImageExist = ProductImageEntity.find { ProductImageTable.productId eq productId }
                    .toList().singleOrNull()
            isImageExist?.let {
                File("${AppConstants.Image.PRODUCT_IMAGE_LOCATION}${it.imageUrl}").delete()
                it.imageUrl = productImage
                it.response()
            } ?: run {
                ProductImageEntity.new {
                    this.userId = EntityID(userId, ProductTable)
                    this.productId = EntityID(productId, ProductTable)
                    this.imageUrl = productImage
                }.response()
            }
        }
}