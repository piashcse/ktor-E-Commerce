package com.piashcse.feature.notification

import com.piashcse.config.DotEnv
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Deep links used in transactional email and in-app notifications. */
object StorefrontLinks {
    private val baseUrl: String
        get() = DotEnv.get("STOREFRONT_URL", "http://localhost:3000").trimEnd('/')

    fun orderTracking(
        orderNumber: String,
        email: String?,
    ): String {
        val params =
            buildString {
                append("orderNumber=").append(encode(orderNumber))
                email?.takeIf { it.isNotBlank() }?.let { append("&email=").append(encode(it)) }
            }
        return "$baseUrl/track?$params"
    }

    fun product(productId: String): String = "$baseUrl/products/$productId"

    fun orders(): String = "$baseUrl/account/orders"

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
}
