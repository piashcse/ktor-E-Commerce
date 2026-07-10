package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.timestamp

object BlacklistedTokenTable : BaseIdTable("blacklisted_token") {
    val token = varchar("token", 1000).uniqueIndex()
    val blacklistedAt = timestamp("blacklisted_at")
}

class BlacklistedTokenDAO(id: EntityID<String>) : BaseEntity(id, BlacklistedTokenTable) {
    companion object : BaseEntityClass<BlacklistedTokenDAO>(BlacklistedTokenTable, BlacklistedTokenDAO::class.java)

    var token by BlacklistedTokenTable.token
    var blacklistedAt by BlacklistedTokenTable.blacklistedAt
}
