package com.piashcse.feature.cms

import com.piashcse.constants.CmsPageStatus
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
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.readQuery
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDateTime

data class SitemapProduct(
    val id: String,
    val updatedAt: LocalDateTime?,
)

data class SitemapPage(
    val id: String,
    val slug: String,
    val updatedAt: LocalDateTime?,
)

data class SitemapData(
    val products: List<SitemapProduct>,
    val categoryIds: List<String>,
    val pages: List<SitemapPage>,
    val noIndex: Set<String>,
)

interface CmsRepository {
    suspend fun publicPages(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<CmsPageResponse>

    suspend fun publicPage(slug: String): CmsPageResponse?

    suspend fun adminPages(
        status: CmsPageStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<CmsPageResponse>

    suspend fun findBySlug(slug: String): CmsPageResponse?

    suspend fun create(
        request: CmsPageRequest,
        sanitizedBody: String,
    ): CmsPageResponse

    suspend fun update(
        id: String,
        request: CmsPageRequest,
        sanitizedBody: String,
    ): CmsPageResponse?

    suspend fun archive(id: String): Boolean

    suspend fun findSeo(
        entityType: SeoEntityType,
        entityId: String?,
        locale: String,
    ): SeoMetadataResponse?

    suspend fun findPublishedPage(slug: String): CmsPageResponse?

    suspend fun upsertSeo(
        request: SeoMetadataRequest,
        locale: String,
    ): SeoMetadataResponse

    suspend fun sitemapData(): SitemapData
}

class CmsRepositoryImpl : CmsRepository {
    override suspend fun publicPages(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<CmsPageResponse> =
        readQuery {
            CmsPageTable.selectAll()
                .where { CmsPageTable.status eq CmsPageStatus.PUBLISHED }
                .orderBy(CmsPageTable.publishedAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { CmsPageDAO.wrapRow(it).toResponse() }
        }

    override suspend fun publicPage(slug: String): CmsPageResponse? =
        readQuery {
            CmsPageDAO.find {
                (CmsPageTable.slug eq slug) and (CmsPageTable.status eq CmsPageStatus.PUBLISHED)
            }.firstOrNull()?.toResponse()
        }

    override suspend fun adminPages(
        status: CmsPageStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<CmsPageResponse> =
        readQuery {
            CmsPageTable.selectAll()
                .also { query ->
                    status?.let { query.andWhere { CmsPageTable.status eq it } }
                    query.orderBy(CmsPageTable.createdAt to SortOrder.DESC)
                }.toPaginatedResponse(limit, offset) { CmsPageDAO.wrapRow(it).toResponse() }
        }

    override suspend fun findBySlug(slug: String): CmsPageResponse? =
        readQuery {
            CmsPageDAO.find { CmsPageTable.slug eq slug }.firstOrNull()?.toResponse()
        }

    override suspend fun create(
        request: CmsPageRequest,
        sanitizedBody: String,
    ): CmsPageResponse =
        query {
            CmsPageDAO.new {
                slug = request.slug
                title = request.title
                excerpt = request.excerpt
                body = sanitizedBody
                status = request.status
                seoTitle = request.seoTitle
                seoDescription = request.seoDescription
                canonicalUrl = request.canonicalUrl
                ogImage = request.ogImage
                publishedAt = if (request.status == CmsPageStatus.PUBLISHED) currentUtc() else null
            }.toResponse()
        }

    override suspend fun update(
        id: String,
        request: CmsPageRequest,
        sanitizedBody: String,
    ): CmsPageResponse? =
        query {
            val page = CmsPageDAO.findById(id) ?: return@query null
            page.apply {
                slug = request.slug
                title = request.title
                excerpt = request.excerpt
                body = sanitizedBody
                status = request.status
                seoTitle = request.seoTitle
                seoDescription = request.seoDescription
                canonicalUrl = request.canonicalUrl
                ogImage = request.ogImage
                if (request.status == CmsPageStatus.PUBLISHED && publishedAt == null) publishedAt = currentUtc()
                if (request.status != CmsPageStatus.PUBLISHED) publishedAt = null
            }.toResponse()
        }

    override suspend fun archive(id: String): Boolean =
        query {
            val page = CmsPageDAO.findById(id) ?: return@query false
            page.status = CmsPageStatus.ARCHIVED
            true
        }

    override suspend fun findSeo(
        entityType: SeoEntityType,
        entityId: String?,
        locale: String,
    ): SeoMetadataResponse? =
        readQuery {
            SeoMetadataDAO.find {
                (SeoMetadataTable.entityType eq entityType) and
                    (SeoMetadataTable.entityId eq entityId) and
                    (SeoMetadataTable.locale eq locale)
            }.firstOrNull()?.toResponse()
        }

    override suspend fun findPublishedPage(slug: String): CmsPageResponse? =
        readQuery {
            CmsPageDAO.find {
                (CmsPageTable.slug eq slug) and (CmsPageTable.status eq CmsPageStatus.PUBLISHED)
            }.firstOrNull()?.toResponse()
        }

    override suspend fun upsertSeo(
        request: SeoMetadataRequest,
        locale: String,
    ): SeoMetadataResponse =
        query {
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

    override suspend fun sitemapData(): SitemapData =
        readQuery {
            val approvedShops =
                ShopDAO.find { ShopTable.status eq ShopStatus.APPROVED }
                    .map { it.id }
                    .toSet()
            val products =
                ProductDAO.find {
                    (ProductTable.status eq ProductStatus.ACTIVE) and
                        (ProductTable.deletedAt.isNull()) and
                        (ProductTable.shopId inList approvedShops)
                }.limit(5000).toList()
            val categories = ProductCategoryTable.selectAll().limit(1000).map { it[ProductCategoryTable.id].value }
            val pages = CmsPageDAO.find { CmsPageTable.status eq CmsPageStatus.PUBLISHED }.limit(1000).toList()
            val noIndex =
                SeoMetadataDAO.find { SeoMetadataTable.noIndex eq true }
                    .mapNotNull { it.entityId }
                    .toSet()
            SitemapData(
                products = products.map { SitemapProduct(it.id.value, it.updatedAt ?: it.createdAt) },
                categoryIds = categories,
                pages = pages.map { SitemapPage(it.id.value, it.slug, it.updatedAt ?: it.publishedAt) },
                noIndex = noIndex,
            )
        }

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
            createdAt = createdAt?.toUtcString(),
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
