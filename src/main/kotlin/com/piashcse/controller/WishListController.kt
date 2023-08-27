package com.piashcse.controller

import com.piashcse.entities.product.ProductEntity
import com.piashcse.entities.product.ProductTable
import com.piashcse.entities.product.WishListEntity
import com.piashcse.entities.product.WishListTable
import com.piashcse.utils.CommonException
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class WishListController {
    fun addToWishList(userId: String, productId: String) = transaction {
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

    fun getWishList(userId: String) = transaction {
        WishListEntity.find { WishListTable.userId eq userId }.toList().map {
            ProductEntity.find { ProductTable.id eq it.productId }.first().response()
        }
    }

    fun deleteFromWishList(userId: String, productId: String) = transaction {
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