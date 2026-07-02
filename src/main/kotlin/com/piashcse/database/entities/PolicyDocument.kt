package com.piashcse.database.entities

import com.piashcse.constants.PolicyType
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.PolicyDocumentResponse
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object PolicyDocumentTable : BaseIdTable("policy_documents") {
    val title = varchar("title", 255)
    val type = enumerationByName("type", 30, PolicyType::class)
    val content = text("content")
    val version = varchar("version", 50)
    val effectiveDate = datetime("effective_date")
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)
}

/**
 * Data Access Object for policy documents
 */
class PolicyDocumentDAO(id: EntityID<String>) : BaseEntity(id, PolicyDocumentTable) {
    companion object : BaseEntityClass<PolicyDocumentDAO>(PolicyDocumentTable, PolicyDocumentDAO::class.java)

    var title by PolicyDocumentTable.title
    var type by PolicyDocumentTable.type
    var content by PolicyDocumentTable.content
    var version by PolicyDocumentTable.version
    var effectiveDate by PolicyDocumentTable.effectiveDate
    var isActive by PolicyDocumentTable.isActive

    fun response() =
        PolicyDocumentResponse(
            id.value,
            title,
            type.name,
            content,
            version,
            effectiveDate.toString(), // Convert LocalDateTime to string for response
            isActive,
        )
}
