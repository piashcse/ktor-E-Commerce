package com.piashcse.feature.cms

import com.piashcse.constants.CmsPageStatus
import com.piashcse.constants.Localization
import com.piashcse.constants.ProductStatus
import com.piashcse.constants.SeoEntityType
import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.CmsPageDAO
import com.piashcse.database.entities.CmsPageTable
import com.piashcse.database.entities.ProductCategoryTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.SeoMetadataDAO
import com.piashcse.database.entities.SeoMetadataTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

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

class CmsService {
    suspend fun publicPages(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<CmsPageResponse> =
        query {
            CmsPageTable.selectAll()
                .where { CmsPageTable.status eq CmsPageStatus.PUBLISHED }
                .orderBy(CmsPageTable.publishedAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { CmsPageDAO.wrapRow(it).toResponse() }
        }

    suspend fun publicPage(slug: String): CmsPageResponse =
        query {
            CmsPageDAO.find {
                (CmsPageTable.slug eq slug) and (CmsPageTable.status eq CmsPageStatus.PUBLISHED)
            }.firstOrNull()?.toResponse() ?: slug.throwNotFound("Page")
        }

    suspend fun adminPages(
        status: CmsPageStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<CmsPageResponse> =
        query {
            CmsPageTable.selectAll()
                .also { query ->
                    status?.let { query.andWhere { CmsPageTable.status eq it } }
                    query.orderBy(CmsPageTable.createdAt to SortOrder.DESC)
                }.toPaginatedResponse(limit, offset) { CmsPageDAO.wrapRow(it).toResponse() }
        }

    suspend fun create(request: CmsPageRequest): CmsPageResponse =
        query {
            validateSlug(request.slug)
            if (CmsPageDAO.find { CmsPageTable.slug eq request.slug }.any()) {
                throw ValidationException("Slug already exists")
            }
            CmsPageDAO.new {
                slug = request.slug
                title = request.title
                excerpt = request.excerpt
                body = sanitizeHtml(request.body)
                status = request.status
                seoTitle = request.seoTitle
                seoDescription = request.seoDescription
                canonicalUrl = request.canonicalUrl
                ogImage = request.ogImage
                publishedAt = if (request.status == CmsPageStatus.PUBLISHED) currentUtc() else null
            }.toResponse()
        }

    suspend fun update(
        id: String,
        request: CmsPageRequest,
    ): CmsPageResponse =
        query {
            validateSlug(request.slug)
            val page = CmsPageDAO.findById(id) ?: id.throwNotFound("Page")
            val duplicate =
                CmsPageDAO.find { CmsPageTable.slug eq request.slug }.firstOrNull()
            if (duplicate != null && duplicate.id.value != id) throw ValidationException("Slug already exists")
            page.apply {
                slug = request.slug
                title = request.title
                excerpt = request.excerpt
                body = sanitizeHtml(request.body)
                status = request.status
                seoTitle = request.seoTitle
                seoDescription = request.seoDescription
                canonicalUrl = request.canonicalUrl
                ogImage = request.ogImage
                if (request.status == CmsPageStatus.PUBLISHED && publishedAt == null) publishedAt = currentUtc()
                if (request.status != CmsPageStatus.PUBLISHED) publishedAt = null
            }.toResponse()
        }

    suspend fun archive(id: String): Boolean =
        query {
            val page = CmsPageDAO.findById(id) ?: return@query false
            page.status = CmsPageStatus.ARCHIVED
            true
        }

    suspend fun seo(
        entityType: SeoEntityType,
        entityId: String?,
        locale: String,
    ): SeoMetadataResponse =
        query {
            val normalized = Localization.normalize(locale)
            val row =
                SeoMetadataDAO.find {
                    (SeoMetadataTable.entityType eq entityType) and
                        (SeoMetadataTable.entityId eq entityId) and
                        (SeoMetadataTable.locale eq normalized)
                }.firstOrNull()
            if (row != null) return@query row.toResponse()
            if (entityType == SeoEntityType.PAGE && !entityId.isNullOrBlank()) {
                val page = CmsPageDAO.find { CmsPageTable.slug eq entityId }.firstOrNull()
                if (page != null) {
                    return@query SeoMetadataResponse(
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
            SeoMetadataResponse(entityType, entityId, normalized, null, null, null, null, null, false)
        }

    suspend fun upsertSeo(request: SeoMetadataRequest): SeoMetadataResponse =
        query {
            if (request.entityType != SeoEntityType.HOME && request.entityId.isNullOrBlank()) {
                throw ValidationException("entityId is required for ${request.entityType}")
            }
            val locale = Localization.normalize(request.locale)
            val existing =
                SeoMetadataDAO.find {
                    (SeoMetadataTable.entityType eq request.entityType) and
                        (SeoMetadataTable.entityId eq request.entityId) and
                        (SeoMetadataTable.locale eq locale)
                }.firstOrNull()
            val row =
                existing?.apply {
                    title = request.title
                    description = request.description
                    keywords = request.keywords
                    canonicalUrl = request.canonicalUrl
                    ogImage = request.ogImage
                    noIndex = request.noIndex
                } ?: SeoMetadataDAO.new {
                    entityType = request.entityType
                    entityId = request.entityId
                    this.locale = locale
                    title = request.title
                    description = request.description
                    keywords = request.keywords
                    canonicalUrl = request.canonicalUrl
                    ogImage = request.ogImage
                    noIndex = request.noIndex
                }
            row.toResponse()
        }

    suspend fun sitemap(baseUrl: String): String =
        query {
            val approvedShops =
                com.piashcse.database.entities.ShopDAO.find { ShopTable.status eq ShopStatus.APPROVED }
                    .map { it.id }
                    .toSet()
            val products =
                ProductDAO.find {
                    (ProductTable.status eq ProductStatus.ACTIVE) and
                        (ProductTable.deletedAt.isNull()) and
                        (ProductTable.shopId inList approvedShops)
                }.take(5000)
            val categories = ProductCategoryTable.selectAll().limit(1000).map { it[ProductCategoryTable.id].value }
            val pages =
                CmsPageDAO.find { CmsPageTable.status eq CmsPageStatus.PUBLISHED }.take(1000)
            buildString {
                append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n")
                appendUrl(baseUrl, "/", null)
                products.forEach { appendUrl(baseUrl, "/products/${it.id.value}", it.updatedAt ?: it.createdAt) }
                categories.forEach { appendUrl(baseUrl, "/product-categories/$it", null) }
                pages.forEach { appendUrl(baseUrl, "/pages/${it.slug}", it.updatedAt ?: it.publishedAt) }
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

    private fun sanitizeHtml(html: String): String =
        html
            .replace(Regex("(?is)<(script|style|iframe)[^>]*>.*?</\\1>"), "")
            .replace(Regex("(?i)\\son\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)"), "")
            .replace(Regex("(?i)javascript:"), "")

    private fun CmsPageDAO.toResponse() =
        CmsPageResponse(
            id = id.value,
            slug = slug,
            title = title,
            excerpt = excerpt,
            body = body,
            status = status,
            seoTitle = seoTitle,
            seoDescription = seoDescription,
            canonicalUrl = canonicalUrl,
            ogImage = ogImage,
            publishedAt = publishedAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
            updatedAt = updatedAt?.toUtcString(),
        )

    private fun SeoMetadataDAO.toResponse() =
        SeoMetadataResponse(
            entityType = entityType,
            entityId = entityId,
            locale = locale,
            title = title,
            description = description,
            keywords = keywords,
            canonicalUrl = canonicalUrl,
            ogImage = ogImage,
            noIndex = noIndex,
        )
}
