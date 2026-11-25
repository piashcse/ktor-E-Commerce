package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class FxRateResponse(
    val targetCurrency: String,
    val rate: String,
    val effectiveAt: String,
)
