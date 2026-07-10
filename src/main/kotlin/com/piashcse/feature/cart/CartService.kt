package com.piashcse.feature.cart

class CartService(private val cartRepo: CartRepository) : CartRepository by cartRepo
