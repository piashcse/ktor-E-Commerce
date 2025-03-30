package com.piashcse.models

import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class PaymentRequest(
    val orderId: String,
    val amount: Long,
    val status: String,
    val paymentMethod: String
){
    fun validation(){
        validate(this){
            validate(PaymentRequest::orderId).isNotNull().isNotEmpty()
            validate(PaymentRequest::amount).isNotNull().isGreaterThan(0)
            validate(PaymentRequest::status).isNotNull().isNotEmpty()
            validate(PaymentRequest::paymentMethod).isNotNull().isNotEmpty()
        }
    }
}
