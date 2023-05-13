package com.piashcse.controller

import com.piashcse.entities.product.*
import com.piashcse.models.product.request.AddProduct
import com.piashcse.models.product.request.DeleteProduct
import com.piashcse.models.product.request.ProductWithFilter
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ProductController {
    fun createProduct(addProduct: AddProduct) = transaction {
        return@transaction ProductEntity.new {
            categoryId = EntityID(addProduct.categoryId, ProductTable)
            subCategoryId = EntityID(addProduct.subCategoryId, ProductTable)
            brandId = addProduct.brandId?.let { EntityID(addProduct.brandId, ProductTable) }
            productName = addProduct.productName
            productCode = addProduct.productCode
            productQuantity = addProduct.productQuantity
            productDetail = addProduct.productDetail
            price = addProduct.price
            discountPrice = addProduct.discountPrice
            status = addProduct.status
        }.response()
    }

    fun uploadProductImages(productId: String, productImages: String) = transaction {
        return@transaction {
            ProductImageEntity.new {
                this.productId = productId
                imageUrl = productImages
            }.response()
        }
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
        return@transaction query.limit(productQuery.limit, productQuery.offset).map {
            ProductEntity.wrapRow(it).response()
        }
    }

    fun deleteProduct(deleteProduct: DeleteProduct) = transaction {
        val isProductExist = ProductEntity.find { ProductTable.id eq deleteProduct.productId }.toList().singleOrNull()
        isProductExist?.delete()
    }

    /* fun createProduct(addProduct: AddProduct) = transaction {
         return@transaction {
             val product = ProductEntity.new {
                 categoryId = addProduct.categoryId
                 title = addProduct.title
                 description = addProduct.description
                 price = addProduct.price
             }
             val productImage =
                 ProductImageEntity.find { ProductImage.id eq addProduct.imageId }.toList().singleOrNull()?.let {
                     it.productId = product.id.value
                     it.response()
                 }
             addProduct.color?.let {
                 val variant = ProductVariantEntity.new {
                     productId = product.id
                     name = AppConstants.ProductVariant.COLOR
                 }
                 ProductVariantOptionEntity.new {
                     productVariantId = variant.id
                     name = addProduct.color
                 }
             }
             addProduct.size?.let {
                 val variant = ProductVariantEntity.new {
                     productId = product.id
                     name = AppConstants.ProductVariant.SIZE
                 }
                 ProductVariantOptionEntity.new {
                     productVariantId = variant.id
                     name = addProduct.size
                 }
             }
             productImage?.imageUrl?.split(",")?.let {
                 ProductResponse(
                     addProduct.categoryId,
                     addProduct.title,
                     it.map { it.trim() },
                     addProduct.description,
                     addProduct.color,
                     addProduct.size,
                     addProduct.price,
                     addProduct.discountPrice,
                     addProduct.quantity
                 )
             }
         }
     }*/
}