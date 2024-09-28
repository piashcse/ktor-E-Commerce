package com.piashcse.repository

import com.piashcse.entities.Payment
import com.piashcse.models.AddPayment

interface PaymentRepo {
    suspend fun addPayment(payment: AddPayment): Payment
    suspend fun getPayment(paymentId:String): Payment
}