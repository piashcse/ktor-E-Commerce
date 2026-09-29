package com.piashcse.mapper

import com.piashcse.database.entities.ShippingAddressDAO
import com.piashcse.database.entities.ShippingMethodDAO
import com.piashcse.model.response.ShippingAddressResponse
import com.piashcse.model.response.ShippingMethodResponse
import com.piashcse.utils.common.Money

fun ShippingAddressDAO.toShippingAddressResponse() =
    ShippingAddressResponse(
        id = id.value,
        userId = userId.value,
        firstName = firstName,
        lastName = lastName,
        email = email,
        phoneNumber = phoneNumber,
        streetAddress = streetAddress,
        city = city,
        state = state,
        country = country,
        zipCode = zipCode,
        isDefault = isDefault,
    )

fun ShippingMethodDAO.toShippingMethodResponse() =
    ShippingMethodResponse(
        id = id.value,
        name = name,
        type = type,
        price = Money.str(price),
        deliveryTime = deliveryTime,
    )
