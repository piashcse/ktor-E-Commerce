package com.example.entities.product

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import com.example.entities.shop.ShopTable
import org.jetbrains.exposed.dao.id.EntityID

object StockTable : BaseIntIdTable("stock") {
    val productId = text("product_id").references(ProductTable.id)
    val shopId = text("shop_id").references(ShopTable.id)
    val quantity = integer("quantity") // product quantity
}

class StockEntity(id: EntityID<String>) : BaseIntEntity(id, ProductImage) {
    companion object : BaseIntEntityClass<StockEntity>(ProductImage)

    var productId by StockTable.productId
    var shopId by StockTable.shopId
    var quantity by StockTable.quantity
}
