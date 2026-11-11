package com.piashcse.plugin

import com.piashcse.constants.Localization
import com.piashcse.feature.i18n.I18nService
import io.ktor.http.HttpHeaders
import io.ktor.server.application.*
import io.ktor.server.request.header
import io.ktor.util.AttributeKey

private val LocaleAttribute = AttributeKey<String>("request-locale")
private val i18n = I18nService()

/** Locale negotiated from `?lang=`, `X-Locale` or `Accept-Language`. */
val ApplicationCall.locale: String
    get() = attributes.getOrNull(LocaleAttribute) ?: Localization.DEFAULT_LOCALE

fun Application.installLocalization() {
    intercept(ApplicationCallPipeline.Setup) {
        val raw =
            call.request.queryParameters["lang"]
                ?: call.request.header("X-Locale")
                ?: call.request.header(HttpHeaders.AcceptLanguage)
        val locale = i18n.resolve(raw)
        call.attributes.put(LocaleAttribute, locale)
        call.response.headers.append(HttpHeaders.ContentLanguage, locale)
    }
}
