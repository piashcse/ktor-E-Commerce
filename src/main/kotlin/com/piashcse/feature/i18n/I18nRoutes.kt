@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.i18n

import com.piashcse.constants.Localization
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Public locale metadata and message catalogs for client-side rendering. */
fun Route.i18nRoutes() {
    val i18n: I18nService by inject()

    get("locales") {
        call.respondOk(i18n.locales())
    }.describe { summary = "List supported locales" }

    get("messages") {
        val locale = call.request.queryParameters["locale"] ?: Localization.DEFAULT_LOCALE
        call.respondOk(i18n.messages(locale))
    }.describe {
        summary = "Get translated API message catalog"
        description = "Falls back to English for unknown locales/keys"
    }
}

/** Admin product content translations. */
fun Route.productTranslationAdminRoutes() {
    val translations: ProductTranslationService by inject()

    get("{id}/translations") {
        call.respondOk(translations.list(call.requirePathParameter("id")))
    }.describe { summary = "List a product's translations" }

    adminWriteRateLimit {
        put("{id}/translations/{locale}") {
            val productId = call.requirePathParameter("id")
            val locale = call.requirePathParameter("locale")
            call.respondOk(
                translations.upsert(productId, locale, call.receive<ProductTranslationRequest>()),
            )
        }.describe {
            summary = "Create or update a product translation"
            requestBody { jsonBody<ProductTranslationRequest>() }
            responses { ok<ProductTranslationResponse>("Translation saved") }
        }

        delete("{id}/translations/{locale}") {
            val productId = call.requirePathParameter("id")
            val locale = call.requirePathParameter("locale")
            if (!translations.delete(productId, locale)) {
                throw NotFoundException("Translation not found")
            }
            call.respondOk(MessageResponse("Translation deleted"))
        }.describe { summary = "Delete a product translation" }
    }
}
