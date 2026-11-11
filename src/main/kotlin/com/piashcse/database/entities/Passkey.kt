package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

/** WebAuthn credential (passkey). Public keys are stored as base64 SPKI. */
object PasskeyCredentialTable : BaseIdTable("passkey_credential") {
    val userId = reference("user_id", UserTable.id).index()
    val credentialId = varchar("credential_id", 500).uniqueIndex()
    val publicKey = text("public_key")
    val signCount = long("sign_count").default(0)
    val transports = varchar("transports", 200).nullable()
    val name = varchar("name", 100).nullable()
    val lastUsedAt = datetime("last_used_at").nullable()
}

class PasskeyCredentialDAO(id: EntityID<String>) : BaseEntity(id, PasskeyCredentialTable) {
    companion object : BaseEntityClass<PasskeyCredentialDAO>(PasskeyCredentialTable, PasskeyCredentialDAO::class.java)

    var userId by PasskeyCredentialTable.userId
    var credentialId by PasskeyCredentialTable.credentialId
    var publicKey by PasskeyCredentialTable.publicKey
    var signCount by PasskeyCredentialTable.signCount
    var transports by PasskeyCredentialTable.transports
    var name by PasskeyCredentialTable.name
    var lastUsedAt by PasskeyCredentialTable.lastUsedAt
}

/** Durable WebAuthn ceremony challenge (single-use, five-minute TTL). */
object PasskeyChallengeTable : BaseIdTable("passkey_challenge") {
    val challenge = varchar("challenge", 200).uniqueIndex()
    val userId = reference("user_id", UserTable.id).index()
    val type = varchar("type", 20)
    val expiresAt = datetime("expires_at")
    val consumedAt = datetime("consumed_at").nullable()
}

class PasskeyChallengeDAO(id: EntityID<String>) : BaseEntity(id, PasskeyChallengeTable) {
    companion object : BaseEntityClass<PasskeyChallengeDAO>(PasskeyChallengeTable, PasskeyChallengeDAO::class.java)

    var challenge by PasskeyChallengeTable.challenge
    var userId by PasskeyChallengeTable.userId
    var type by PasskeyChallengeTable.type
    var expiresAt by PasskeyChallengeTable.expiresAt
    var consumedAt by PasskeyChallengeTable.consumedAt
}
