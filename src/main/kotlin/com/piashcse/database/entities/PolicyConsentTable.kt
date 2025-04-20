package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDateTime


/**
 * Table for tracking user consent to various policies
 */
object PolicyConsentTable : BaseIntIdTable("policy_consents") {
    val userId = reference("user_id", UserTable)
    val policyId = reference("policy_id", PolicyDocumentTable)
    val consentDate = varchar("consent_date", 50).default(LocalDateTime.now().toString())
    val ipAddress = varchar("ip_address", 50).nullable()
    val userAgent = varchar("user_agent", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}

/**
 * Data Access Object for user policy consents
 */
class PolicyConsentDAO(id: EntityID<String>) : BaseIntEntity(id, PolicyConsentTable) {
    companion object : BaseIntEntityClass<PolicyConsentDAO>(PolicyConsentTable)

    var userId by PolicyConsentTable.userId
    var policyId by PolicyDocumentDAO referencedOn PolicyConsentTable.policyId
    var consentDate by PolicyConsentTable.consentDate
    var ipAddress by PolicyConsentTable.ipAddress
    var userAgent by PolicyConsentTable.userAgent

    fun response() = UserPolicyConsentResponse(
        id.value,
        userId.value,
        policyId.id.value,
        consentDate,
        ipAddress,
        userAgent
    )
}

/**
 * Response model for user policy consents
 */
data class UserPolicyConsentResponse(
    val id: String,
    val userId: String,
    val policyId: String,
    val consentDate: String,
    val ipAddress: String?,
    val userAgent: String?
)