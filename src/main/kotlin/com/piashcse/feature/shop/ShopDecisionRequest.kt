package com.piashcse.feature.shop

import com.piashcse.utils.extension.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class ShopDecisionRequest(val reason: String? = null)
