package com.piashcse.models

import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddPayment(
    val orderId: String,
    val amount: Long,
    val status: String,
    val paymentMethod: String
){
    fun validation(){
        validate(this){
            validate(AddPayment::orderId).isNotNull().isNotEmpty()
            validate(AddPayment::amount).isNotNull().isGreaterThan(0)
            validate(AddPayment::status).isNotNull().isNotEmpty()
            validate(AddPayment::paymentMethod).isNotNull().isNotEmpty()
        }
    }
}
