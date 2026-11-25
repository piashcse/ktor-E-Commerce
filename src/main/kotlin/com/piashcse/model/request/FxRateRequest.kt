package com.piashcse.model.request

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class FxRateRequest(
    @Contextual val rate: BigDecimal,
)
