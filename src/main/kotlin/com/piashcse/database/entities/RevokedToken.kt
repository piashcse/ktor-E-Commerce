package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object RevokedTokenTable : BaseIdTable("revoked_token") {
    val tokenHash = varchar("token_hash", 255).index()
    val userId = reference("user_id", UserTable.id)
    val expiresAt = datetime("expires_at")
}

class RevokedTokenDAO(id: EntityID<String>) : BaseEntity(id, RevokedTokenTable) {
    companion object : BaseEntityClass<RevokedTokenDAO>(RevokedTokenTable, RevokedTokenDAO::class.java)

    var tokenHash by RevokedTokenTable.tokenHash
    var userId by RevokedTokenTable.userId
    var expiresAt by RevokedTokenTable.expiresAt
}
