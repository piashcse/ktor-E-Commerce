package com.piashcse.mapper

import com.piashcse.database.entities.Cart
import com.piashcse.database.entities.CartItemDAO
import com.piashcse.database.entities.WishList
import com.piashcse.database.entities.WishListDAO
import com.piashcse.model.response.ProductResponse

fun CartItemDAO.toCartResponse(product: ProductResponse? = null) = Cart(productId.value, quantity, product)

fun WishListDAO.toWishListResponse(product: ProductResponse? = null) = WishList(product)
