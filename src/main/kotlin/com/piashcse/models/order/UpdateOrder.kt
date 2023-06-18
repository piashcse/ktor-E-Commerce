package com.piashcse.models.order

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class UpdateOrder(@QueryParam("orderId") val orderId: String, @QueryParam("orderStatus") val orderStatus: String) {
    fun validation() {
        validate(this) {
            validate(UpdateOrder::orderId).isNotNull()
            validate(UpdateOrder::orderStatus).isNotNull()
        }
    }
}