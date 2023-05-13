package com.piashcse.entities.product.defaultproductcategory

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import com.piashcse.entities.shop.ShopTable
import org.jetbrains.exposed.dao.id.EntityID

object ProductCategoryTable : BaseIntIdTable("product_category") {
    val productCategoryName = text("product_category_name")
    val productCategoryCreatorType =
        text("product_category_creator_type") // admin and shop owner can create category
    val shopId = reference("shop_id", ShopTable.id).nullable() // if shop owner creates category id
}

class ProductCategoryEntity(id: EntityID<String>) : BaseIntEntity(id, ProductCategoryTable) {
    companion object : BaseIntEntityClass<ProductCategoryEntity>(ProductCategoryTable)

    var productCategoryName by ProductCategoryTable.productCategoryName
    var productCategoryCreatorType by ProductCategoryTable.productCategoryCreatorType
    var shopId by ProductCategoryTable.shopId
    fun productCategoryResponse() = ProductCategoryResponse(id.value, productCategoryName)
}

data class ProductCategoryResponse(val id: String, val productCategoryName: String)