package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.ProductResponse
import com.piashcse.model.response.ProductVariantResponse
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object CartItemTable : BaseIdTable("cart_item") {
    val userId = reference("user_id", UserTable.id).index()
    val productId = reference("product_id", ProductTable.id)
    val variantId = reference("variant_id", ProductVariantTable.id).nullable()
    val quantity = integer("quantity").default(1)
}

class CartItemDAO(id: EntityID<String>) : BaseEntity(id, CartItemTable) {
    companion object : BaseEntityClass<CartItemDAO>(CartItemTable, CartItemDAO::class.java)

    var userId by CartItemTable.userId
    var productId by CartItemTable.productId
    var variantId by CartItemTable.variantId
    var quantity by CartItemTable.quantity
}

@Serializable
data class Cart(
    val productId: String,
    val quantity: Int,
    val product: ProductResponse?,
    val variantId: String? = null,
    val variant: ProductVariantResponse? = null,
)
