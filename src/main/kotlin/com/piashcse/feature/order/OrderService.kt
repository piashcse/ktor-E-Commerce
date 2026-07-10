package com.piashcse.feature.order

class OrderService(private val orderRepo: OrderRepository) : OrderRepository by orderRepo
