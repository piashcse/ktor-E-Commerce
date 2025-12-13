package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.PolicyDocument
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

/**
 * Table for storing various policy documents like privacy policy, terms, etc.
 */
object PolicyDocumentTable : BaseIdTable("policy_documents") {
    val title = varchar("title", 255)
    val type = enumerationByName("type", 30, PolicyType::class) // PRIVACY_POLICY, TERMS_CONDITIONS, REFUND_POLICY, etc.
    val content = text("content")
    val version = varchar("version", 50)
    val effectiveDate = datetime("effective_date") // DateTime type instead of varchar
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)

    /**
     * Enum class for policy document types
     */
    enum class PolicyType {
        PRIVACY_POLICY,
        TERMS_CONDITIONS,
        REFUND_POLICY,
        COOKIE_POLICY,
        DISCLAIMER,
        EULA,
        SHIPPING_POLICY;
        val isLegalPolicy get() = this in listOf(PRIVACY_POLICY, TERMS_CONDITIONS, DISCLAIMER, EULA)
        val requiresConsent get() = this != SHIPPING_POLICY  // Shipping policy might not require consent
    }
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

    fun response() = PolicyDocument(
        id.value,
        title,
        type.name,
        content,
        version,
        effectiveDate.toString(),  // Convert LocalDateTime to string for response
        isActive,
    )
}