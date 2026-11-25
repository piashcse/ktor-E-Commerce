package com.piashcse.feature.cms

import com.piashcse.constants.CmsPageStatus
import com.piashcse.constants.Localization
import com.piashcse.constants.SeoEntityType
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable

@Serializable
data class CmsPageRequest(
    val slug: String,
    val title: String,
    val excerpt: String? = null,
    val body: String,
    val status: CmsPageStatus = CmsPageStatus.DRAFT,
    val seoTitle: String? = null,
    val seoDescription: String? = null,
    val canonicalUrl: String? = null,
    val ogImage: String? = null,
)

@Serializable
data class CmsPageResponse(
    val id: String,
    val slug: String,
    val title: String,
    val excerpt: String?,
    val body: String,
    val status: CmsPageStatus,
    val seoTitle: String?,
    val seoDescription: String?,
    val canonicalUrl: String?,
    val ogImage: String?,
    val publishedAt: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

@Serializable
data class SeoMetadataRequest(
    val entityType: SeoEntityType,
    val entityId: String? = null,
    val locale: String = Localization.DEFAULT_LOCALE,
    val title: String? = null,
    val description: String? = null,
    val keywords: String? = null,
    val canonicalUrl: String? = null,
    val ogImage: String? = null,
    val noIndex: Boolean = false,
)

@Serializable
data class SeoMetadataResponse(
    val entityType: SeoEntityType,
    val entityId: String?,
    val locale: String,
    val title: String?,
    val description: String?,
    val keywords: String?,
    val canonicalUrl: String?,
    val ogImage: String?,
    val noIndex: Boolean,
)

class CmsService(
    private val repository: CmsRepository,
) {
    suspend fun publicPages(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<CmsPageResponse> = repository.publicPages(limit, offset)

    suspend fun publicPage(slug: String): CmsPageResponse = repository.publicPage(slug) ?: slug.throwNotFound("Page")

    suspend fun adminPages(
        status: CmsPageStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<CmsPageResponse> = repository.adminPages(status, limit, offset)

    suspend fun create(request: CmsPageRequest): CmsPageResponse {
        validateSlug(request.slug)
        if (repository.findBySlug(request.slug) != null) {
            throw ValidationException("Slug already exists")
        }
        return repository.create(request, sanitizeHtml(request.body))
    }

    suspend fun update(
        id: String,
        request: CmsPageRequest,
    ): CmsPageResponse {
        validateSlug(request.slug)
        val duplicate = repository.findBySlug(request.slug)
        if (duplicate != null && duplicate.id != id) throw ValidationException("Slug already exists")
        return repository.update(id, request, sanitizeHtml(request.body)) ?: id.throwNotFound("Page")
    }

    suspend fun archive(id: String): Boolean = repository.archive(id)

    suspend fun seo(
        entityType: SeoEntityType,
        entityId: String?,
        locale: String,
    ): SeoMetadataResponse {
        val normalized = Localization.normalize(locale)
        repository.findSeo(entityType, entityId, normalized)?.let { return it }
        if (normalized != Localization.DEFAULT_LOCALE) {
            repository.findSeo(entityType, entityId, Localization.DEFAULT_LOCALE)?.let { return it }
        }
        if (entityType == SeoEntityType.PAGE && !entityId.isNullOrBlank()) {
            repository.findPublishedPage(entityId)?.let { page ->
                return SeoMetadataResponse(
                    entityType = entityType,
                    entityId = entityId,
                    locale = normalized,
                    title = page.seoTitle ?: page.title,
                    description = page.seoDescription ?: page.excerpt,
                    keywords = null,
                    canonicalUrl = page.canonicalUrl,
                    ogImage = page.ogImage,
                    noIndex = false,
                )
            }
        }
        return SeoMetadataResponse(entityType, entityId, normalized, null, null, null, null, null, false)
    }

    suspend fun upsertSeo(request: SeoMetadataRequest): SeoMetadataResponse {
        if (request.entityType != SeoEntityType.HOME && request.entityId.isNullOrBlank()) {
            throw ValidationException("entityId is required for ${request.entityType}")
        }
        val requestedLocale =
            request.locale.trim().lowercase().substringBefore('-').substringBefore('_')
        if (requestedLocale !in Localization.CODES) {
            throw ValidationException("Unsupported locale: ${request.locale}")
        }
        return repository.upsertSeo(request, Localization.normalize(request.locale))
    }

    suspend fun sitemap(baseUrl: String): String {
        val data = repository.sitemapData()
        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n")
            appendUrl(baseUrl, "/", null)
            data.products.filterNot { it.id in data.noIndex }
                .forEach { appendUrl(baseUrl, "/products/${it.id}", it.updatedAt) }
            data.categoryIds.filterNot { it in data.noIndex }
                .forEach { appendUrl(baseUrl, "/product-categories/$it", null) }
            data.pages.filterNot { it.id in data.noIndex || it.slug in data.noIndex }
                .forEach { appendUrl(baseUrl, "/pages/${it.slug}", it.updatedAt) }
            append("</urlset>\n")
        }
    }

    suspend fun robots(baseUrl: String): String =
        "User-agent: *\nAllow: /\nDisallow: /api/\nDisallow: /admin/\nSitemap: $baseUrl/sitemap.xml\n"

    private fun StringBuilder.appendUrl(
        baseUrl: String,
        path: String,
        lastModified: java.time.LocalDateTime?,
    ) {
        append("<url><loc>")
        append(escapeXml("$baseUrl$path"))
        append("</loc>")
        if (lastModified != null) append("<lastmod>${lastModified.toLocalDate()}</lastmod>")
        append("</url>\n")
    }

    private fun escapeXml(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun validateSlug(slug: String) {
        if (!slug.matches(Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$"))) {
            throw FieldValidationException(
                listOf(FieldViolation("slug", "Slug must be lowercase letters, digits and hyphens")),
            )
        }
    }

    private fun decodeNumericEntities(value: String): String =
        Regex("&#(x?)([0-9a-fA-F]+);").replace(value) { match ->
            val code =
                match.groupValues[2].toIntOrNull(if (match.groupValues[1].isNotEmpty()) 16 else 10)
            code?.takeIf { it in 0..0x10FFFF }?.let { String(Character.toChars(it)) } ?: match.value
        }

    private fun sanitizeHtml(html: String): String {
        // Browsers decode numeric entities before resolving a URL, so
        // `jav&#x61;script:` is executable even though the literal text does
        // not match. Reject the document instead of trying to rewrite it.
        val decoded = decodeNumericEntities(html)
        if (Regex("(?i)(javascript|vbscript|data)\\s*:").containsMatchIn(decoded)) {
            throw ValidationException("CMS body contains a disallowed URI scheme")
        }
        return html
            .replace(Regex("(?is)<(script|style|iframe)[^>]*>.*?</\\1>"), "")
            .replace(Regex("(?i)\\son\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)"), "")
            .replace(Regex("(?i)javascript:"), "")
    }
}
