package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.Product
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object CartItemTable : BaseIdTable("cart_item") {
    val userId = reference("user_id", UserTable.id)
    val productId = reference("product_id", ProductTable.id)
    val quantity = integer("quantity").default(1)
}

class CartItemDAO(id: EntityID<String>) : BaseEntity(id, CartItemTable) {
    companion object : BaseEntityClass<CartItemDAO>(CartItemTable, CartItemDAO::class.java)

    var userId by CartItemTable.userId
    var productId by CartItemTable.productId
    var quantity by CartItemTable.quantity
    fun response(product: Product? = null) = Cart(productId.value, quantity, product)
}

data class Cart(
    val productId: String,
    val quantity: Int,
    val product: Product?
)