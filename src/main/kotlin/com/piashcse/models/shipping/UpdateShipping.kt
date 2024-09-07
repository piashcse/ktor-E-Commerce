package com.piashcse.models.shipping

import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate


data class UpdateShipping(
    val orderId: String,
    val shipAddress: String,
    val shipCity: String?,
    val shipPhone: Int?,
    val shipName: String?,
    val shipEmail: String?,
    val shipCountry: String?
) {
    fun validation() {
        validate(this) {
            validate(UpdateShipping::orderId).isNotNull().isNotEmpty()
            validate(UpdateShipping::shipAddress).isNotNull().isNotEmpty()
        }
    }
}