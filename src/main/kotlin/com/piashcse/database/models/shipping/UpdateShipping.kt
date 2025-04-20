package com.piashcse.database.models.shipping

import com.piashcse.database.entities.ShippingTable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate


data class UpdateShipping(
    val id: String,
    val address: String?,
    val city: String?,
    val country: String?,
    val phone: Int?,
    val shippingMethod: String?,
    val email: String?,
    val status: ShippingTable.ShippingStatus?,
    val trackingNumber: String?
) {
    fun validation() {
        validate(this) {
            validate(UpdateShipping::id).isNotNull().isNotEmpty()
        }
    }
}