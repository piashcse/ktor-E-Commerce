package com.piashcse.feature.recommendation

import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.docs
import com.piashcse.plugin.ok
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.respondOk
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/** Clamps recommendation page size to a safe, cache-friendly range. */
fun ApplicationCall.recommendationLimit(default: Int = 10): Int =
    request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, RecommendationRanking.MAX_LIMIT) ?: default

fun Route.recommendationRoutes() {
    val service: RecommendationService by inject()
    customerOnlyAuth {
        /**
         * Personalized recommendations from purchase, cart and wishlist signals
         *
         * @tag Recommendation
         * @description Personalized recommendations from purchase, cart and wishlist signals
         */
        get {
            call.respondOk(service.personalized(call.currentUserId, call.recommendationLimit()))
        }.docs("Personalized recommendations", "Order/cart/wishlist category signals, backfilled with best sellers") {
            responses { ok<RecommendationResponse>("Ranked products") }
        }
    }
}
