package com.piashcse.controller

import com.piashcse.entities.product.*
import com.piashcse.models.product.request.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ProductController {
    fun addProduct(userId: String, addProduct: AddProduct) = transaction {
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
    fun updateProduct(userId: String, productId: String, updateProduct: UpdateProduct) = transaction {
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

    fun getProduct(productQuery: ProductWithFilter) = transaction {
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

    fun getProductById(userId: String, productQuery: ProductWithFilter) = transaction {
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

    fun productDetail(productDetail: ProductDetail) = transaction {
        val isProductExist = ProductEntity.find { ProductTable.id eq productDetail.productId }.toList().singleOrNull()
        isProductExist?.response()
    }

    fun deleteProduct(userId: String, deleteProduct: ProductId) = transaction {
        val isProductExist =
            ProductEntity.find { ProductTable.userId eq userId and (ProductTable.id eq deleteProduct.productId) }
                .toList().singleOrNull()
        isProductExist?.delete()
        deleteProduct.productId
    }

    fun uploadProductImages(userId: String, productId: String, productImages: String) = transaction {
        ProductImageEntity.new {
            this.userId = EntityID(userId, ProductTable)
            this.productId = EntityID(productId, ProductTable)
            this.imageUrl = productImages
        }.response()

    }
}