package com.piashcse.models.shipping

import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate


data class AddShipping(
    val orderId: String,
    val shipAddress: String,
    val shipCity: String,
    val shipPhone: Int,
    val shipName: String?,
    val shipEmail: String?,
    val shipCountry: String?
) {
    fun validation() {
        validate(this) {
            validate(AddShipping::orderId).isNotNull().isNotEmpty()
            validate(AddShipping::shipAddress).isNotNull().isNotEmpty()
            validate(AddShipping::shipCity)
            validate(AddShipping::shipPhone)
            validate(AddShipping::shipEmail).isEmail()
        }
    }
}