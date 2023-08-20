package com.piashcse.models.shipping

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class OrderId(@QueryParam("orderId") val orderId: String) {
    fun validation() {
        validate(this) {
            validate(OrderId::orderId).isNotNull()
        }
    }
}