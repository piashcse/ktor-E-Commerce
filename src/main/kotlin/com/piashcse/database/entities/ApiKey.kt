package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

/** Programmatic API keys (hashed at rest; raw value shown once at creation). */
object ApiKeyTable : BaseIdTable("api_key") {
    val userId = reference("user_id", UserTable.id).index()
    val name = varchar("name", 100)
    val keyHash = varchar("key_hash", 64).uniqueIndex()
    val prefix = varchar("prefix", 16)

    /** Comma-separated [com.piashcse.constants.Permission] names. */
    val scopes = text("scopes")
    val isActive = bool("is_active").default(true)
    val lastUsedAt = datetime("last_used_at").nullable()
    val expiresAt = datetime("expires_at").nullable()
}

class ApiKeyDAO(id: EntityID<String>) : BaseEntity(id, ApiKeyTable) {
    companion object : BaseEntityClass<ApiKeyDAO>(ApiKeyTable, ApiKeyDAO::class.java)

    var userId by ApiKeyTable.userId
    var name by ApiKeyTable.name
    var keyHash by ApiKeyTable.keyHash
    var prefix by ApiKeyTable.prefix
    var scopes by ApiKeyTable.scopes
    var isActive by ApiKeyTable.isActive
    var lastUsedAt by ApiKeyTable.lastUsedAt
    var expiresAt by ApiKeyTable.expiresAt
}
