package com.example.entities.product

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import com.example.entities.user.UserTable
import org.jetbrains.exposed.dao.id.EntityID

object WishListTable : BaseIntIdTable("wishlist") {
    val userId = text("user_id").references(UserTable.id)
    val productId = text("product_id").references(ProductTable.id)
}

class WishListEntity(id: EntityID<String>) : BaseIntEntity(id, WishListTable) {
    companion object : BaseIntEntityClass<WishListEntity>(WishListTable)

    var productId by StockTable.productId
    var shopId by StockTable.shopId
    var quantity by StockTable.quantity
}