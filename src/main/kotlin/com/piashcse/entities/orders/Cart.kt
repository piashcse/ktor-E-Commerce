package com.piashcse.entities.orders

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import com.piashcse.entities.product.ProductTable
import com.piashcse.entities.user.UserTable
import org.jetbrains.exposed.dao.id.EntityID

object CartItemTable : BaseIntIdTable("cart_items") {
    val userId = reference("user_id", UserTable.id)
    val productId = reference("product_id", ProductTable.id)
    val quantity = integer("quantity")
}

class CartItemEntity(id: EntityID<String>) : BaseIntEntity(id, CartItemTable) {
    companion object : BaseIntEntityClass<CartItemEntity>(CartItemTable)

    var userId by CartItemTable.userId
    var productId by CartItemTable.productId
    var quantity by CartItemTable.quantity
    fun cartResponse() = Cart(userId.value, productId.value, quantity)
}

data class Cart(
    val userId: String,
    val productId: String,
    val quantity: Int
)