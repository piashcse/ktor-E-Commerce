package com.piashcse.feature.wishlist

import com.piashcse.constants.Message
import com.piashcse.constants.ProductStatus
import com.piashcse.database.entities.*
import com.piashcse.feature.cart.requireProduct
import com.piashcse.feature.cart.requireProductWithStock
import com.piashcse.feature.cart.requireWishlistItem
import com.piashcse.mapper.toCartResponse
import com.piashcse.mapper.toProductResponse
import com.piashcse.mapper.toWishlistResponse
import com.piashcse.model.response.ProductResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class WishlistRepositoryImpl : WishlistRepository {
    override suspend fun addToWishlist(
        userId: String,
        productId: String,
    ): Wishlist =
        query {
            val product = requireProduct(productId, "ProductResponse")
            if (product.deletedAt != null || product.status != ProductStatus.ACTIVE) {
                throw ValidationException("Cannot add an inactive product to the wishlist")
            }

            val existing =
                WishlistDAO.find { WishlistTable.userId eq userId and (WishlistTable.productId eq productId) }
                    .firstOrNull()

            if (existing == null) {
                WishlistDAO.new {
                    this.userId = userId.entityID(UserTable)
                    this.productId = productId.entityID(ProductTable)
                }.toWishlistResponse(product.toProductResponse())
            } else {
                throw productId.throwConflict("ProductResponse")
            }
        }

    override suspend fun getWishlist(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ProductResponse> =
        query {
            WishlistTable.innerJoin(ProductTable)
                .selectAll()
                .andWhere {
                    (WishlistTable.userId eq userId) and
                        (ProductTable.status eq ProductStatus.ACTIVE) and
                        ProductTable.deletedAt.isNull()
                }
                .paginateWithPreload(
                    limit,
                    offset,
                    rowMapper = { ProductDAO.wrapRow(it) },
                ) { products ->
                    val images = ProductImageDAO.imagesForProducts(products.map { it.id })
                    products.map { it.toProductResponse(images[it.id.value]) }
                }
        }

    override suspend fun removeFromWishlist(
        userId: String,
        productId: String,
    ): ProductResponse =
        query {
            val wishlistItem = requireWishlistItem(userId, productId)
            val product = requireProduct(productId, "ProductResponse").toProductResponse()
            wishlistItem.delete()
            product
        }

    override suspend fun moveToCart(
        userId: String,
        productId: String,
        quantity: Int,
        variantId: String?,
    ): Cart =
        query {
            if (quantity <= 0) {
                throw ValidationException(
                    Message.Validation.notPositive("Quantity"),
                )
            }
            val product = requireProduct(productId, "ProductResponse")
            if (product.deletedAt != null || product.status != ProductStatus.ACTIVE) {
                throw ValidationException("Cannot move an inactive product to the cart")
            }
            val item = requireWishlistItem(userId, productId)
            val variantFilter =
                if (variantId == null) {
                    CartItemTable.variantId.isNull()
                } else {
                    CartItemTable.variantId eq variantId.entityID(ProductVariantTable)
                }
            val existing =
                CartItemDAO.find {
                    (CartItemTable.userId eq userId) and (CartItemTable.productId eq productId) and variantFilter
                }.singleOrNull()
            val requestedQuantity = (existing?.quantity ?: 0) + quantity
            requireProductWithStock(productId, requestedQuantity, "ProductResponse", variantId = variantId)

            val cart =
                if (existing != null) {
                    existing.quantity = requestedQuantity
                    existing.toCartResponse()
                } else {
                    CartItemDAO.new {
                        this.userId = userId.entityID(UserTable)
                        this.productId = productId.entityID(ProductTable)
                        this.variantId = variantId?.entityID(ProductVariantTable)
                        this.quantity = requestedQuantity
                    }.toCartResponse()
                }
            item.delete()
            cart
        }

    override suspend fun isProductInWishlist(
        userId: String,
        productId: String,
    ): Boolean =
        query {
            !WishlistDAO.find { WishlistTable.userId eq userId and (WishlistTable.productId eq productId) }
                .empty()
        }
}
