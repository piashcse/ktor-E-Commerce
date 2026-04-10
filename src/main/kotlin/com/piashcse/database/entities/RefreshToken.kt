package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object RefreshTokenTable : BaseIdTable("refresh_token") {
    val userId = reference("user_id", UserTable.id)
    val token = varchar("token", 512).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val isRevoked = bool("is_revoked").default(false)
    val lastUsedAt = datetime("last_used_at").nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
}

class RefreshTokenDAO(id: EntityID<String>) : BaseEntity(id, RefreshTokenTable) {
    companion object : BaseEntityClass<RefreshTokenDAO>(RefreshTokenTable, RefreshTokenDAO::class.java)

    var userId by RefreshTokenTable.userId
    var token by RefreshTokenTable.token
    var expiresAt by RefreshTokenTable.expiresAt
    var isRevoked by RefreshTokenTable.isRevoked
    var lastUsedAt by RefreshTokenTable.lastUsedAt
    var ipAddress by RefreshTokenTable.ipAddress
    var userAgent by RefreshTokenTable.userAgent
}
