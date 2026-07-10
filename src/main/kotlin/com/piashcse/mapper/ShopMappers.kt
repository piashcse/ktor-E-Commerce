package com.piashcse.mapper

import com.piashcse.database.entities.ShopDAO
import com.piashcse.model.response.ShopResponse

fun ShopDAO.toShopResponse() = ShopResponse(
    id = id.value,
    name = name,
    categoryId = categoryId.value,
    description = description,
    address = address,
    phone = phone,
    email = email,
    logo = logo,
    coverImage = coverImage,
    status = status,
    rating = rating,
    totalReviews = totalReviews,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
