package com.piashcse.models.shipping

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.validate


data class UpdateShipping(
    @PathParam("orderId") val orderId: String,
    @QueryParam("shipAddress") val shipAddress: String?,
    @QueryParam("shipCity") val shipCity: String?,
    @QueryParam("shipPhone") val shipPhone: Int?,
    @QueryParam("shipName") val shipName: String?,
    @QueryParam("shipEmail") val shipEmail: String?,
    @QueryParam("shipCountry") val shipCountry: String?
) {
    fun validation() {
        validate(this) {
            validate(UpdateShipping::orderId).isNotNull()
            validate(UpdateShipping::shipAddress).isNotNull()
        }
    }
}