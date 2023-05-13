package com.example.controller

import com.example.entities.orders.CartEntity
import com.example.entities.product.ProductTable
import com.example.entities.user.UserTable
import com.example.models.PagingData
import com.example.models.cart.AddCart
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