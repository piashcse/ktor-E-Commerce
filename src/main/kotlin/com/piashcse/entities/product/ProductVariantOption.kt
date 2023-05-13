package com.piashcse.entities.product

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import com.piashcse.entities.product.defaultproductcategory.ProductCategoryTable
import org.jetbrains.exposed.dao.id.EntityID

object ProductVariantOptionTable : BaseIntIdTable("product_variant_option") {
    val productVariantId = ProductCategoryTable.reference("product_variant_id", ProductVariantTable.id)
    val name = text("name")
}

class ProductVariantOptionEntity(id: EntityID<String>) : BaseIntEntity(id, ProductVariantOptionTable) {
    companion object : BaseIntEntityClass<ProductVariantOptionEntity>(ProductVariantOptionTable)

    var productVariantId by ProductVariantOptionTable.productVariantId
    var name by ProductVariantTable.name
}