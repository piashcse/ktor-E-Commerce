@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.cms

import com.piashcse.config.DotEnvConfig
import com.piashcse.constants.CmsPageStatus
import com.piashcse.constants.SeoEntityType
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import io.ktor.http.ContentType
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Public CMS pages. */
fun Route.cmsRoutes() {
    val service: CmsService by inject()
    get("pages") {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.publicPages(limit, offset))
    }.describe { summary = "List published CMS pages" }

    get("pages/{slug}") {
        call.respondOk(service.publicPage(call.requirePathParameter("slug")))
    }.describe { summary = "Get a published CMS page by slug" }
}

/** Admin CMS page CRUD. */
fun Route.cmsAdminRoutes() {
    val service: CmsService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        val status = call.request.queryParameters["status"]?.parseEnum<CmsPageStatus>("page status")
        call.respondOk(service.adminPages(status, limit, offset))
    }.describe { summary = "Admin: list CMS pages" }

    adminWriteRateLimit {
        post {
            call.respondCreated(service.create(call.receive<CmsPageRequest>()))
        }.describe {
            summary = "Admin: create a CMS page"
            requestBody { jsonBody<CmsPageRequest>() }
            responses { created<CmsPageResponse>("Page created") }
        }

        put("{id}") {
            call.respondOk(service.update(call.requirePathParameter("id"), call.receive<CmsPageRequest>()))
        }.describe {
            summary = "Admin: update a CMS page"
            requestBody { jsonBody<CmsPageRequest>() }
            responses { ok<CmsPageResponse>("Page updated") }
        }

        delete("{id}") {
            val id = call.requirePathParameter("id")
            if (!service.archive(id)) throw NotFoundException("Page not found")
            call.respondOk(MessageResponse("Page archived"))
        }.describe { summary = "Admin: archive a CMS page" }
    }
}

/** Public SEO metadata lookup. */
fun Route.seoRoutes() {
    val service: CmsService by inject()
    get("metadata") {
        val entityType =
            call.request.queryParameters["entityType"]?.parseEnum<SeoEntityType>("entity type")
                ?: throw com.piashcse.utils.validator.ValidationException("entityType is required")
        call.respondOk(
            service.seo(
                entityType = entityType,
                entityId = call.request.queryParameters["entityId"],
                locale = call.request.queryParameters["locale"] ?: "en",
            ),
        )
    }.describe { summary = "Get SEO metadata for an entity" }
}

/** Admin SEO metadata upsert. */
fun Route.seoAdminRoutes() {
    val service: CmsService by inject()
    adminWriteRateLimit {
        put("metadata") {
            call.respondOk(service.upsertSeo(call.receive<SeoMetadataRequest>()))
        }.describe {
            summary = "Admin: create/update SEO metadata"
            requestBody { jsonBody<SeoMetadataRequest>() }
            responses { ok<SeoMetadataResponse>("SEO metadata saved") }
        }
    }
}

/** Root sitemap.xml and robots.txt. */
fun Route.seoSiteRoutes() {
    val service: CmsService by inject()
    get("/sitemap.xml") {
        call.respondText(service.sitemap(DotEnvConfig.publicBaseUrl), ContentType.Application.Xml)
    }.describe { summary = "XML sitemap" }

    get("/robots.txt") {
        call.respondText(service.robots(DotEnvConfig.publicBaseUrl), ContentType.Text.Plain)
    }.describe { summary = "Robots.txt" }
}
