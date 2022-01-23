package com.example.controller

import com.example.entities.product.*
import com.example.entities.product.ProductImage.image_url
import com.example.entities.product.ProductTable.description
import com.example.entities.product.ProductTable.price
import com.example.entities.product.ProductTable.title
import com.example.entities.product.defaultproductcategory.ProductCategoryEntity
import com.example.entities.product.defaultproductcategory.ProductCategoryTable
import com.example.entities.product.defaultvariant.ProductColorEntity
import com.example.entities.product.defaultvariant.ProductColorTable
import com.example.entities.product.defaultvariant.ProductSizeEntity
import com.example.entities.product.defaultvariant.ProductSizeTable
import com.example.models.product.AddCategoryBody
import com.example.models.product.AddProduct
import com.example.models.product.ProductResponse
import com.example.utils.AppConstants
import com.example.utils.CommonException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ProductController {
    fun createProductCategory(productCategory: AddCategoryBody) = transaction {
        val categoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.product_category_name eq productCategory.categoryName }
                .toList().singleOrNull()
        return@transaction if (categoryExist == null) {
            ProductCategoryEntity.new(UUID.randomUUID().toString()) {
                product_category_name = productCategory.categoryName
                product_category_creator_type = productCategory.userType
            }.productCategoryResponse()
        } else {
            throw CommonException("Product category name ${productCategory.categoryName} already exist")
        }
    }

    /*    fun createVariant(colorName: String) = transaction {
            val colorExist = ProductVariantEntity.find { ProductVariantTable.name eq colorName }.toList().singleOrNull()
            return@transaction if (colorExist == null) {
                ProductVariantEntity.new(UUID.randomUUID().toString()) {
                    name = colorName
                }.response()
            } else {
                throw CommonException("Color name $colorName already exist")
            }
        }*/
    fun createDefaultColorOption(colorName: String) = transaction {
        val colorExist = ProductColorEntity.find { ProductColorTable.name eq colorName }.toList().singleOrNull()
        return@transaction if (colorExist == null) {
            ProductColorEntity.new(UUID.randomUUID().toString()) {
                name = colorName
            }.response()
        } else {
            throw CommonException("Color name $colorName already exist")
        }
    }

    fun createDefaultSizeOption(sizeName: String) = transaction {
        val sizeExist = ProductSizeEntity.find { ProductSizeTable.name eq sizeName }.toList().singleOrNull()
        return@transaction if (sizeExist == null) {
            ProductSizeEntity.new(UUID.randomUUID().toString()) {
                name = sizeName
            }.response()
        } else {
            throw CommonException("Size name $sizeName already exist")
        }
    }

    fun uploadProductImages(productImages: String) = transaction {
        return@transaction {
            ProductImageEntity.new(UUID.randomUUID().toString()) {
                image_url = productImages
            }.response()
        }
    }

    fun createProduct(addProduct: AddProduct) = transaction {
        return@transaction {
            val product = ProductEntity.new(UUID.randomUUID().toString()) {
                category_id = addProduct.categoryId
                title = addProduct.title
                description = addProduct.description
                price = addProduct.price
            }
            val productImage =
                ProductImageEntity.find { ProductImage.id eq addProduct.imageId }.toList().singleOrNull()?.let {
                    it.product_id = product.id.value
                    it.response()
                }
            StockEntity.new(UUID.randomUUID().toString()) {
                product_id = product.id.value
                shop_id = addProduct.shopId
                quantity = addProduct.quantity
            }
            addProduct.color?.let {
                val variant = ProductVariantEntity.new(UUID.randomUUID().toString()) {
                    product_id = product.id
                    name = AppConstants.ProductVariant.COLOR
                }
                ProductVariantOptionEntity.new(UUID.randomUUID().toString()) {
                    product_variant_id = variant.id
                    name = addProduct.color
                }
            }
            addProduct.size?.let {
                val variant = ProductVariantEntity.new(UUID.randomUUID().toString()) {
                    product_id = product.id
                    name = AppConstants.ProductVariant.SIZE
                }
                ProductVariantOptionEntity.new(UUID.randomUUID().toString()) {
                    product_variant_id = variant.id
                    name = addProduct.size
                }
            }
            ProductResponse(
                addProduct.categoryId,
                addProduct.title,
                productImage!!.imageUrl.split(",").map { it.trim() },
                addProduct.description,
                addProduct.color,
                addProduct.size,
                addProduct.price,
                addProduct.discountPrice,
                addProduct.quantity
            )
        }
    }
}