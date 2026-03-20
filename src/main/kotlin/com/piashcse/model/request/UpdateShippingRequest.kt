package com.piashcse.model.request

import kotlinx.serialization.Serializable
import com.piashcse.database.entities.ShippingTable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate


@Serializable
data class UpdateShippingRequest(
    val id: String,
    val address: String?,
    val city: String?,
    val country: String?,
    val phone: String?,
    val shippingMethod: String?,
    val email: String?,
    val status: ShippingTable.ShippingStatus?,
    val trackingNumber: String?
) {
    fun validation() {
        validate(this) {
            validate(UpdateShippingRequest::id).isNotNull().isNotEmpty()
        }
    }
}