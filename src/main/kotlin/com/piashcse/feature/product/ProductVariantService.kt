package com.piashcse.feature.product

import com.piashcse.constants.ProductStatus
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductOptionDAO
import com.piashcse.database.entities.ProductOptionTable
import com.piashcse.database.entities.ProductOptionValueDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.ProductVariantDAO
import com.piashcse.database.entities.ProductVariantOptionDAO
import com.piashcse.database.entities.ProductVariantOptionTable
import com.piashcse.database.entities.ProductVariantTable
import com.piashcse.model.response.ProductOptionResponse
import com.piashcse.model.response.ProductOptionValueResponse
import com.piashcse.model.response.ProductVariantResponse
import com.piashcse.service.Cache
import com.piashcse.service.CacheService
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.verifyOwnership
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import java.math.BigDecimal

@Serializable
data class ProductVariantRequest(
    val sku: String,
    val name: String,
    @Contextual val price: BigDecimal,
    @Contextual val discountPrice: BigDecimal? = null,
    val barcode: String? = null,
    @Contextual val weight: BigDecimal? = null,
    val dimensions: String? = null,
    val imageUrl: String? = null,
    val status: ProductStatus = ProductStatus.ACTIVE,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
) {
    init {
        if (sku.isBlank()) throw ValidationException("SKU is required")
        if (name.isBlank()) throw ValidationException("Variant name is required")
        if (price <= BigDecimal.ZERO) throw ValidationException("Variant price must be positive")
        discountPrice?.let {
            if (it <= BigDecimal.ZERO) throw ValidationException("Discount price must be positive")
            if (it >= price) throw ValidationException("Discount price must be lower than the regular price")
        }
    }
}

@Serializable
data class ProductOptionRequest(
    val name: String,
    val sortOrder: Int = 0,
) {
    init {
        if (name.isBlank()) throw ValidationException("Option name is required")
    }
}

@Serializable
data class ProductOptionValueRequest(
    val value: String,
    val sortOrder: Int = 0,
) {
    init {
        if (value.isBlank()) throw ValidationException("Option value is required")
    }
}

@Serializable
data class SetVariantOptionsRequest(val valueIds: List<String> = emptyList())

/** Seller-facing variant/option management with public reads. */
class ProductVariantService(
    private val cache: Cache = CacheService.cache,
) {
    private suspend fun invalidateCatalog() = cache.invalidatePattern("products:.*")

    suspend fun listVariants(productId: String): List<ProductVariantResponse> =
        query {
            requireProduct(productId)
            ProductVariantDAO.find { ProductVariantTable.productId eq productId.entityID(ProductTable) }
                .orderBy(ProductVariantTable.sortOrder to SortOrder.ASC)
                .map { it.toResponse() }
        }

    suspend fun listOptions(productId: String): List<ProductOptionResponse> =
        query {
            requireProduct(productId)
            ProductOptionDAO.find { ProductOptionTable.productId eq productId.entityID(ProductTable) }
                .orderBy(ProductOptionTable.sortOrder to SortOrder.ASC)
                .map { it.toResponse() }
        }

    suspend fun createVariant(
        actorUserId: String,
        productId: String,
        request: ProductVariantRequest,
    ): ProductVariantResponse =
        query {
            val product = requireOwnedProduct(actorUserId, productId)
            if (request.isDefault) clearDefault(product.id.value)
            val hasVariants =
                ProductVariantDAO.find { ProductVariantTable.productId eq product.id }.firstOrNull() != null
            ProductVariantDAO.new {
                this.productId = product.id
                sku = request.sku
                name = request.name
                price = request.price
                discountPrice = request.discountPrice
                discountPercentage = discountPercentage(request.price, request.discountPrice)
                barcode = request.barcode
                weight = request.weight
                dimensions = request.dimensions
                imageUrl = request.imageUrl
                status = request.status
                isDefault = request.isDefault || !hasVariants
                sortOrder = request.sortOrder
            }.toResponse()
        }.also { invalidateCatalog() }

    suspend fun updateVariant(
        actorUserId: String,
        productId: String,
        variantId: String,
        request: ProductVariantRequest,
    ): ProductVariantResponse =
        query {
            val product = requireOwnedProduct(actorUserId, productId)
            val variant = requireVariant(product.id.value, variantId)
            if (request.isDefault && !variant.isDefault) clearDefault(product.id.value)
            variant.sku = request.sku
            variant.name = request.name
            variant.price = request.price
            variant.discountPrice = request.discountPrice
            variant.discountPercentage = discountPercentage(request.price, request.discountPrice)
            variant.barcode = request.barcode
            variant.weight = request.weight
            variant.dimensions = request.dimensions
            variant.imageUrl = request.imageUrl
            variant.status = request.status
            variant.isDefault = request.isDefault
            variant.sortOrder = request.sortOrder
            variant.toResponse()
        }.also { invalidateCatalog() }

    suspend fun deleteVariant(
        actorUserId: String,
        productId: String,
        variantId: String,
    ) {
        query {
            val product = requireOwnedProduct(actorUserId, productId)
            val variant = requireVariant(product.id.value, variantId)
            val wasDefault = variant.isDefault
            ProductVariantOptionTable.deleteWhere { ProductVariantOptionTable.variantId eq variant.id }
            variant.delete()
            if (wasDefault) {
                ProductVariantDAO.find { ProductVariantTable.productId eq product.id }
                    .orderBy(ProductVariantTable.sortOrder to SortOrder.ASC)
                    .firstOrNull()
                    ?.let { it.isDefault = true }
            }
        }
        invalidateCatalog()
    }

    suspend fun setVariantOptions(
        actorUserId: String,
        productId: String,
        variantId: String,
        request: SetVariantOptionsRequest,
    ): ProductVariantResponse =
        query {
            val product = requireOwnedProduct(actorUserId, productId)
            val variant = requireVariant(product.id.value, variantId)
            val seenOptions = mutableSetOf<String>()
            val links =
                request.valueIds.distinct().map { valueId ->
                    val value = ProductOptionValueDAO.findById(valueId) ?: valueId.throwNotFound("Option value")
                    val option = ProductOptionDAO.findById(value.optionId.value) ?: valueId.throwNotFound("Option")
                    if (option.productId.value != product.id.value) {
                        throw ValidationException("Option value does not belong to this product")
                    }
                    if (!seenOptions.add(option.id.value)) {
                        throw ValidationException("Only one value per option is allowed")
                    }
                    option.id to value.id
                }
            ProductVariantOptionTable.deleteWhere { ProductVariantOptionTable.variantId eq variant.id }
            links.forEach { (optionId, valueId) ->
                ProductVariantOptionDAO.new {
                    this.variantId = variant.id
                    this.optionId = optionId
                    this.optionValueId = valueId
                }
            }
            variant.toResponse()
        }.also { invalidateCatalog() }

    suspend fun createOption(
        actorUserId: String,
        productId: String,
        request: ProductOptionRequest,
    ): ProductOptionResponse =
        query {
            val product = requireOwnedProduct(actorUserId, productId)
            ProductOptionDAO.new {
                this.productId = product.id
                name = request.name
                sortOrder = request.sortOrder
            }.toResponse()
        }.also { invalidateCatalog() }

    suspend fun deleteOption(
        actorUserId: String,
        productId: String,
        optionId: String,
    ) {
        query {
            val product = requireOwnedProduct(actorUserId, productId)
            val option = ProductOptionDAO.findById(optionId) ?: optionId.throwNotFound("Option")
            if (option.productId.value != product.id.value) throw ValidationException("Option does not belong to this product")
            option.delete()
        }
        invalidateCatalog()
    }

    suspend fun addOptionValue(
        actorUserId: String,
        productId: String,
        optionId: String,
        request: ProductOptionValueRequest,
    ): ProductOptionValueResponse =
        query {
            val product = requireOwnedProduct(actorUserId, productId)
            val option = ProductOptionDAO.findById(optionId) ?: optionId.throwNotFound("Option")
            if (option.productId.value != product.id.value) throw ValidationException("Option does not belong to this product")
            ProductOptionValueDAO.new {
                this.optionId = option.id
                value = request.value
                sortOrder = request.sortOrder
            }.let {
                ProductOptionValueResponse(it.id.value, it.optionId.value, it.value, it.sortOrder)
            }
        }.also { invalidateCatalog() }

    suspend fun deleteOptionValue(
        actorUserId: String,
        productId: String,
        optionId: String,
        valueId: String,
    ) {
        query {
            val product = requireOwnedProduct(actorUserId, productId)
            val value = ProductOptionValueDAO.findById(valueId) ?: valueId.throwNotFound("Option value")
            val option = ProductOptionDAO.findById(value.optionId.value) ?: valueId.throwNotFound("Option")
            if (option.id.value != optionId || option.productId.value != product.id.value) {
                throw ValidationException("Option value does not belong to this product option")
            }
            value.delete()
        }
        invalidateCatalog()
    }

    private fun requireProduct(productId: String): ProductDAO =
        ProductDAO.findById(productId)?.takeIf { it.deletedAt == null } ?: productId.throwNotFound("Product")

    private fun requireOwnedProduct(
        actorUserId: String,
        productId: String,
    ): ProductDAO = requireProduct(productId).verifyOwnership(actorUserId, "product") { it.userId.value }

    private fun requireVariant(
        productId: String,
        variantId: String,
    ): ProductVariantDAO {
        val variant = ProductVariantDAO.findById(variantId) ?: variantId.throwNotFound("Variant")
        if (variant.productId.value != productId) throw ValidationException("Variant does not belong to this product")
        return variant
    }

    private fun clearDefault(productId: String) {
        ProductVariantDAO.find {
            (ProductVariantTable.productId eq productId.entityID(ProductTable)) and (ProductVariantTable.isDefault eq true)
        }.forEach { it.isDefault = false }
    }

    private fun discountPercentage(
        price: BigDecimal,
        discountPrice: BigDecimal?,
    ): BigDecimal? =
        discountPrice?.let {
            price.subtract(it)
                .divide(price, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .setScale(2, java.math.RoundingMode.HALF_UP)
        }
}
