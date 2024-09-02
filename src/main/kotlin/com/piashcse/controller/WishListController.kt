package com.piashcse.controller

import com.piashcse.entities.product.ProductEntity
import com.piashcse.entities.product.ProductTable
import com.piashcse.entities.WishListEntity
import com.piashcse.entities.WishListTable
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class WishListController {
    suspend fun addToWishList(userId: String, productId: String) = query {
        val isExits =
            WishListEntity.find { WishListTable.userId eq userId and (WishListTable.productId eq productId) }.toList()
                .singleOrNull()
        if (isExits == null) {
            WishListEntity.new {
                this.userId = EntityID(userId, WishListTable)
                this.productId = EntityID(productId, WishListTable)
            }.response(ProductEntity.find { ProductTable.id eq productId }.first().response())
        } else {
            productId.alreadyExistException()
        }
    }

    suspend fun getWishList(userId: String) = query {
        WishListEntity.find { WishListTable.userId eq userId }.toList().map {
            ProductEntity.find { ProductTable.id eq it.productId }.first().response()
        }
    }

    suspend fun deleteFromWishList(userId: String, productId: String) = query {
        val isExits =
            WishListEntity.find { WishListTable.userId eq userId and (WishListTable.productId eq productId) }.toList()
                .singleOrNull()
        isExits?.let {
            it.delete()
            ProductEntity.find { ProductTable.id eq it.productId }.first().response()
        } ?: run {
            productId.isNotExistException()
        }
    }
}