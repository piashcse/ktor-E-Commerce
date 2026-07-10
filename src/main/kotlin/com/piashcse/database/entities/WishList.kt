package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.ProductResponse
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import kotlinx.serialization.Serializable

object WishListTable : BaseIdTable("wishlist") {
    val userId = reference("user_id", UserTable.id).index()
    val productId = reference("product_id", ProductTable.id)

    init {
        index(customIndexName = "wishlist_user_product_idx", isUnique = false, userId, productId)
    }
}

class WishListDAO(id: EntityID<String>) : BaseEntity(id, WishListTable) {
    companion object : BaseEntityClass<WishListDAO>(WishListTable, WishListDAO::class.java)

    var userId by WishListTable.userId
    var productId by WishListTable.productId

}

@Serializable
data class WishList(val product: ProductResponse? = null)
