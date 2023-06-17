package com.piashcse.entities.orders

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object CartItemTable : BaseIntIdTable("cart_items") {
    val userId = reference("user_id", id)
    val productId = reference("product_id", id)
    val totalPrice = float("total_price")
    val singlePrice = float("single_price")
    val quantity = integer("quantity")
}

class CartItemEntity(id: EntityID<String>) : BaseIntEntity(id, CartItemTable) {
    companion object : BaseIntEntityClass<CartItemEntity>(CartItemTable)

    var userId by CartItemTable.userId
    var productId by CartItemTable.productId
    var totalPrice by CartItemTable.totalPrice
    var singlePrice by CartItemTable.singlePrice
    var quantity by CartItemTable.quantity

    fun cartResponse() = Cart(userId.value, productId.value, totalPrice, singlePrice, quantity)
}

data class Cart(
    val userId: String,
    val productId: String,
    val totalPrice: Float,
    val singlePrice: Float,
    val quantity: Int
)