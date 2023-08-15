package com.piashcse.entities.product

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import com.piashcse.entities.user.UserTable
import org.jetbrains.exposed.dao.id.EntityID

object WishListTable : BaseIntIdTable("wishlist") {
    val userId = reference("user_id", UserTable.id)
    val productId = reference("product_id", ProductTable.id)
}

class WishListEntity(id: EntityID<String>) : BaseIntEntity(id, WishListTable) {
    companion object : BaseIntEntityClass<WishListEntity>(WishListTable)

    var userId by WishListTable.userId
    var productId by WishListTable.productId
    fun response(product: Product? = null) = WishList(product)
}

data class WishList(val product: Product? = null)