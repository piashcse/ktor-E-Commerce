package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.security.SecureRandom
import java.util.*

object RefreshTokenTable : BaseIntIdTable(name = "refresh_tokens") {
    val token = varchar("token", 64).uniqueIndex() // Base64 encoded 32-byte token = 44 chars + padding
    val userId = varchar("user_id", 50) // Matches the id type of UserTable
    val expiryDate = long("expiry_date") // Storing timestamp as long
    val createdDate = long("created_date").default(System.currentTimeMillis()) // Storing creation timestamp
    val isActive = bool("is_active").default(true)
    val userAgent = varchar("user_agent", 512).nullable() // Store user agent for security
    val ipAddress = varchar("ip_address", 45).nullable() // Store IP address for security (IPv6 max length)

    init {
        index(false, userId) // Create index for foreign key lookups
    }
}

class RefreshTokenDAO(id: EntityID<String>) : BaseIntEntity(id, RefreshTokenTable) {
    companion object : BaseIntEntityClass<RefreshTokenDAO>(RefreshTokenTable, RefreshTokenDAO::class.java)

    var token by RefreshTokenTable.token
    var userId by RefreshTokenTable.userId
    var expiryDate by RefreshTokenTable.expiryDate
    var createdDate by RefreshTokenTable.createdDate
    var isActive by RefreshTokenTable.isActive
    var userAgent by RefreshTokenTable.userAgent
    var ipAddress by RefreshTokenTable.ipAddress
}

fun generateRefreshToken(): String {
    val secureRandom = SecureRandom()
    val bytes = ByteArray(32) // 256 bits of randomness
    secureRandom.nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}