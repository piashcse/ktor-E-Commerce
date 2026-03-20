package com.piashcse.model.request

import kotlinx.serialization.Serializable

@Serializable
data class ReviewRatingRequest(val productId:String, val reviewText:String, val rating:Int)
