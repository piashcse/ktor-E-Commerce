package com.piashcse.controller

import com.piashcse.entities.*
import com.piashcse.repository.WishListRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class WishListController : WishListRepo {
    override suspend fun addToWishList(userId: String, productId: String): WishList = query {
        val isExits =
            WishListEntity.find { WishListTable.userId eq userId and (WishListTable.productId eq productId) }.toList()
                .singleOrNull()
        if (isExits == null) {
            WishListEntity.new {
                this.userId = EntityID(userId, WishListTable)
                this.productId = EntityID(productId, WishListTable)
            }.response(ProductEntity.find { ProductTable.id eq productId }.first().response())
        } else {
            throw productId.alreadyExistException()
        }
    }

    override suspend fun getWishList(userId: String, limit: Int): List<Product> = query {
        WishListEntity.find { WishListTable.userId eq userId }.limit(limit).map {
            ProductEntity.find { ProductTable.id eq it.productId }.first().response()
        }
    }

    override suspend fun deleteWishList(userId: String, productId: String): Product = query {
        val isExits =
            WishListEntity.find { WishListTable.userId eq userId and (WishListTable.productId eq productId) }.toList()
                .singleOrNull()
        isExits?.let {
            it.delete()
            ProductEntity.find { ProductTable.id eq it.productId }.first().response()
        } ?: run {
            throw productId.notFoundException()
        }
    }
}