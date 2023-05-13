package com.piashcse.controller

import com.piashcse.entities.orders.CartEntity
import com.piashcse.entities.product.ProductTable
import com.piashcse.entities.user.UserTable
import com.piashcse.models.PagingData
import com.piashcse.models.cart.AddCart
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction

class CartController {
    fun createCart(userId: String, addCart: AddCart) = transaction {
        return@transaction CartEntity.new {
            this.userId = EntityID(userId, UserTable)
            productId = EntityID(addCart.productId, ProductTable)
            totalPrice = addCart.singlePrice * addCart.quantity
            singlePrice = addCart.singlePrice
            quantity = addCart.quantity
        }
    }

    fun getCartItems(pagingData: PagingData) = transaction {
        return@transaction CartEntity.all().limit(pagingData.limit, pagingData.offset).map {
            it.cartResponse()
        }
    }
}