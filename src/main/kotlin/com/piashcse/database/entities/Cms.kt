package com.piashcse.database.entities

import com.piashcse.constants.CmsPageStatus
import com.piashcse.constants.SeoEntityType
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object CmsPageTable : BaseIdTable("cms_page") {
    val slug = varchar("slug", 200).uniqueIndex()
    val title = varchar("title", 200)
    val excerpt = text("excerpt").nullable()
    val body = text("body")
    val status = enumerationByName("status", 20, CmsPageStatus::class).default(CmsPageStatus.DRAFT)
    val seoTitle = varchar("seo_title", 200).nullable()
    val seoDescription = varchar("seo_description", 500).nullable()
    val canonicalUrl = varchar("canonical_url", 500).nullable()
    val ogImage = varchar("og_image", 500).nullable()
    val publishedAt = datetime("published_at").nullable()
}

class CmsPageDAO(id: EntityID<String>) : BaseEntity(id, CmsPageTable) {
    companion object : BaseEntityClass<CmsPageDAO>(CmsPageTable, CmsPageDAO::class.java)

    var slug by CmsPageTable.slug
    var title by CmsPageTable.title
    var excerpt by CmsPageTable.excerpt
    var body by CmsPageTable.body
    var status by CmsPageTable.status
    var seoTitle by CmsPageTable.seoTitle
    var seoDescription by CmsPageTable.seoDescription
    var canonicalUrl by CmsPageTable.canonicalUrl
    var ogImage by CmsPageTable.ogImage
    var publishedAt by CmsPageTable.publishedAt
}

object SeoMetadataTable : BaseIdTable("seo_metadata") {
    val entityType = enumerationByName("entity_type", 30, SeoEntityType::class)
    val entityId = varchar("entity_id", 100).nullable()
    val locale = varchar("locale", 10).default("en")
    val title = varchar("title", 200).nullable()
    val description = varchar("description", 500).nullable()
    val keywords = varchar("keywords", 500).nullable()
    val canonicalUrl = varchar("canonical_url", 500).nullable()
    val ogImage = varchar("og_image", 500).nullable()
    val noIndex = bool("no_index").default(false)
}

class SeoMetadataDAO(id: EntityID<String>) : BaseEntity(id, SeoMetadataTable) {
    companion object : BaseEntityClass<SeoMetadataDAO>(SeoMetadataTable, SeoMetadataDAO::class.java)

    var entityType by SeoMetadataTable.entityType
    var entityId by SeoMetadataTable.entityId
    var locale by SeoMetadataTable.locale
    var title by SeoMetadataTable.title
    var description by SeoMetadataTable.description
    var keywords by SeoMetadataTable.keywords
    var canonicalUrl by SeoMetadataTable.canonicalUrl
    var ogImage by SeoMetadataTable.ogImage
    var noIndex by SeoMetadataTable.noIndex
}
