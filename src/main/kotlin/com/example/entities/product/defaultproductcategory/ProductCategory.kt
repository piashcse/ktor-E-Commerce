package com.example.entities.product.defaultproductcategory

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import com.example.entities.shop.ShopTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProductCategoryTable : BaseIntIdTable("product_category") {
    val product_category_name = text("product_category_name")
    val product_category_creator_type =
        text("product_category_creator_type") // admin and shop owner can create category
    val shop_id = reference("shop_id", ShopTable.id).nullable() // if shop owner creates category id
    override val primaryKey = PrimaryKey(id)
}

class ProductCategoryEntity(id: EntityID<String>) : BaseIntEntity(id, ProductCategoryTable) {
    companion object : BaseIntEntityClass<ProductCategoryEntity>(ProductCategoryTable)

    var product_category_name by ProductCategoryTable.product_category_name
    var product_category_creator_type by ProductCategoryTable.product_category_creator_type
    var shop_id by ProductCategoryTable.shop_id
    fun productCategoryResponse() = ProductCategoryResponse(id.value, product_category_name)
}

data class ProductCategoryResponse(val id: String, val productCategoryName: String)