package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class WishListResponse(val product: ProductResponse? = null)
