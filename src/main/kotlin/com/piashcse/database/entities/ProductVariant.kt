package com.piashcse.database.entities

import com.piashcse.constants.ProductStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.ProductOptionResponse
import com.piashcse.model.response.ProductOptionValueResponse
import com.piashcse.model.response.ProductVariantOptionResponse
import com.piashcse.model.response.ProductVariantResponse
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import java.math.BigDecimal

/** Sellable variant of a product (e.g. "T-Shirt / Red / M"). */
object ProductVariantTable : BaseIdTable("product_variant") {
    val productId = reference("product_id", ProductTable.id).index()
    val sku = varchar("sku", 100).uniqueIndex()
    val name = varchar("name", 150)
    val price = decimal("price", 10, 2)
    val discountPrice = decimal("discount_price", 10, 2).nullable()
    val discountPercentage = decimal("discount_percentage", 5, 2).nullable()
    val barcode = varchar("barcode", 100).nullable().index()
    val weight = decimal("weight", 10, 3).nullable()
    val dimensions = varchar("dimensions", 100).nullable()
    val imageUrl = varchar("image_url", 500).nullable()
    val status = enumerationByName<ProductStatus>("status", 50).default(ProductStatus.ACTIVE).index()
    val isDefault = bool("is_default").default(false)
    val sortOrder = integer("sort_order").default(0)
}

class ProductVariantDAO(id: EntityID<String>) : BaseEntity(id, ProductVariantTable) {
    companion object : BaseEntityClass<ProductVariantDAO>(ProductVariantTable, ProductVariantDAO::class.java)

    var productId by ProductVariantTable.productId
    var sku by ProductVariantTable.sku
    var name by ProductVariantTable.name
    var price by ProductVariantTable.price
    var discountPrice by ProductVariantTable.discountPrice
    var discountPercentage by ProductVariantTable.discountPercentage
    var barcode by ProductVariantTable.barcode
    var weight by ProductVariantTable.weight
    var dimensions by ProductVariantTable.dimensions
    var imageUrl by ProductVariantTable.imageUrl
    var status by ProductVariantTable.status
    var isDefault by ProductVariantTable.isDefault
    var sortOrder by ProductVariantTable.sortOrder

    val effectivePrice: BigDecimal
        get() = discountPrice ?: price

    fun optionValues(): List<ProductVariantOptionResponse> {
        val variantId = id
        return ProductVariantOptionDAO.find { ProductVariantOptionTable.variantId eq variantId }
            .mapNotNull { link ->
                val option = ProductOptionDAO.findById(link.optionId.value) ?: return@mapNotNull null
                val value = ProductOptionValueDAO.findById(link.optionValueId.value) ?: return@mapNotNull null
                ProductVariantOptionResponse(
                    optionId = option.id.value,
                    optionName = option.name,
                    valueId = value.id.value,
                    value = value.value,
                )
            }.sortedBy { it.optionName }
    }

    fun toResponse(options: List<ProductVariantOptionResponse> = optionValues()) =
        ProductVariantResponse(
            id = id.value,
            productId = productId.value,
            sku = sku,
            name = name,
            price = price.toPlainString(),
            discountPrice = discountPrice?.toPlainString(),
            discountPercentage = discountPercentage?.toDouble(),
            barcode = barcode,
            weight = weight?.toDouble(),
            dimensions = dimensions,
            imageUrl = imageUrl,
            status = status,
            isDefault = isDefault,
            sortOrder = sortOrder,
            options = options,
        )
}

/** Option dimension for a product, e.g. "Size" or "Color". */
object ProductOptionTable : BaseIdTable("product_option") {
    val productId = reference("product_id", ProductTable.id).index()
    val name = varchar("name", 100)
    val sortOrder = integer("sort_order").default(0)

    init {
        uniqueIndex("product_option_product_name_idx", productId, name)
    }
}

class ProductOptionDAO(id: EntityID<String>) : BaseEntity(id, ProductOptionTable) {
    companion object : BaseEntityClass<ProductOptionDAO>(ProductOptionTable, ProductOptionDAO::class.java)

    var productId by ProductOptionTable.productId
    var name by ProductOptionTable.name
    var sortOrder by ProductOptionTable.sortOrder
}

/** Allowed value for an option, e.g. "M" for Size. */
object ProductOptionValueTable : BaseIdTable("product_option_value") {
    val optionId = reference("option_id", ProductOptionTable.id).index()
    val value = varchar("value", 100)
    val sortOrder = integer("sort_order").default(0)

    init {
        uniqueIndex("product_option_value_option_value_idx", optionId, value)
    }
}

class ProductOptionValueDAO(id: EntityID<String>) : BaseEntity(id, ProductOptionValueTable) {
    companion object : BaseEntityClass<ProductOptionValueDAO>(ProductOptionValueTable, ProductOptionValueDAO::class.java)

    var optionId by ProductOptionValueTable.optionId
    var value by ProductOptionValueTable.value
    var sortOrder by ProductOptionValueTable.sortOrder
}

/** Maps a variant to one value per option; one row per option per variant. */
object ProductVariantOptionTable : BaseIdTable("product_variant_option") {
    val variantId = reference("variant_id", ProductVariantTable.id).index()
    val optionId = reference("option_id", ProductOptionTable.id)
    val optionValueId = reference("option_value_id", ProductOptionValueTable.id)

    init {
        uniqueIndex("product_variant_option_variant_option_idx", variantId, optionId)
    }
}

class ProductVariantOptionDAO(id: EntityID<String>) : BaseEntity(id, ProductVariantOptionTable) {
    companion object : BaseEntityClass<ProductVariantOptionDAO>(ProductVariantOptionTable, ProductVariantOptionDAO::class.java)

    var variantId by ProductVariantOptionTable.variantId
    var optionId by ProductVariantOptionTable.optionId
    var optionValueId by ProductVariantOptionTable.optionValueId
}

/**
 * Bulk variant mapping. Loads all option links/values in five queries instead
 * of one option query per variant, preserving the input order.
 */
fun variantResponses(variants: List<ProductVariantDAO>): List<ProductVariantResponse> {
    if (variants.isEmpty()) return emptyList()
    val variantIds = variants.map { it.id }
    val links =
        ProductVariantOptionDAO.find { ProductVariantOptionTable.variantId inList variantIds }.toList()
    val optionIds = links.map { it.optionId }.distinct()
    val valueIds = links.map { it.optionValueId }.distinct()
    val options =
        if (optionIds.isEmpty()) {
            emptyMap()
        } else {
            ProductOptionDAO.find { ProductOptionTable.id inList optionIds }.associateBy { it.id.value }
        }
    val values =
        if (valueIds.isEmpty()) {
            emptyMap()
        } else {
            ProductOptionValueDAO.find { ProductOptionValueTable.id inList valueIds }.associateBy { it.id.value }
        }
    val optionsByVariant =
        links.groupBy { it.variantId.value }.mapValues { (_, variantLinks) ->
            variantLinks
                .mapNotNull { link ->
                    val option = options[link.optionId.value] ?: return@mapNotNull null
                    val value = values[link.optionValueId.value] ?: return@mapNotNull null
                    ProductVariantOptionResponse(option.id.value, option.name, value.id.value, value.value)
                }.sortedBy { it.optionName }
        }
    return variants.map { it.toResponse(optionsByVariant[it.id.value].orEmpty()) }
}

/** Bulk option mapping; loads all values in one query. */
fun optionResponses(options: List<ProductOptionDAO>): List<ProductOptionResponse> {
    if (options.isEmpty()) return emptyList()
    val valuesByOption =
        ProductOptionValueDAO.find { ProductOptionValueTable.optionId inList options.map { it.id } }
            .orderBy(ProductOptionValueTable.sortOrder to SortOrder.ASC)
            .toList()
            .groupBy { it.optionId.value }
    return options.map { option ->
        ProductOptionResponse(
            id = option.id.value,
            productId = option.productId.value,
            name = option.name,
            sortOrder = option.sortOrder,
            values =
                valuesByOption[option.id.value].orEmpty().map {
                    ProductOptionValueResponse(
                        id = it.id.value,
                        optionId = it.optionId.value,
                        value = it.value,
                        sortOrder = it.sortOrder,
                    )
                },
        )
    }
}
