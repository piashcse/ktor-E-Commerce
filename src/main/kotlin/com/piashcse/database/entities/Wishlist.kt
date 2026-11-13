package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.ProductResponse
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object WishlistTable : BaseIdTable("wishlist") {
    val userId = reference("user_id", UserTable.id).index()
    val productId = reference("product_id", ProductTable.id)

    init {
        index(customIndexName = "wishlist_user_product_idx", isUnique = true, userId, productId)
    }
}

class WishlistDAO(id: EntityID<String>) : BaseEntity(id, WishlistTable) {
    companion object : BaseEntityClass<WishlistDAO>(WishlistTable, WishlistDAO::class.java)

    var userId by WishlistTable.userId
    var productId by WishlistTable.productId
}

@Serializable
data class Wishlist(val product: ProductResponse? = null)
