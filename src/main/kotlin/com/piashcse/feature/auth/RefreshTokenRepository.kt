package com.piashcse.feature.auth

import com.piashcse.database.entities.RefreshTokenDAO
import com.piashcse.database.entities.RefreshTokenTable
import com.piashcse.database.entities.UserTable
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import java.time.Instant

interface RefreshTokenRepository {
    suspend fun createRefreshToken(userId: String, tokenHash: String, expiresAt: Instant): Boolean
    suspend fun getRefreshTokenByHash(tokenHash: String): RefreshTokenDAO?
    suspend fun revokeRefreshToken(tokenHash: String): Boolean
    suspend fun revokeAllUserTokens(userId: String): Boolean
    suspend fun cleanupExpiredTokens(): Int
}

class RefreshTokenRepositoryImpl : RefreshTokenRepository {
    override suspend fun createRefreshToken(userId: String, tokenHash: String, expiresAt: Instant): Boolean = query {
        RefreshTokenDAO.new {
            this.userId = EntityID(userId, UserTable)
            this.tokenHash = tokenHash
            this.expiresAt = expiresAt
        }
        true
    }

    override suspend fun getRefreshTokenByHash(tokenHash: String): RefreshTokenDAO? = query {
        RefreshTokenDAO.find { RefreshTokenTable.tokenHash eq tokenHash }.singleOrNull()
    }

    override suspend fun revokeRefreshToken(tokenHash: String): Boolean = query {
        val token = RefreshTokenDAO.find { RefreshTokenTable.tokenHash eq tokenHash }.singleOrNull()
        if (token != null) {
            token.revokedAt = Instant.now()
            true
        } else false
    }

    override suspend fun revokeAllUserTokens(userId: String): Boolean = query {
        RefreshTokenDAO.find { RefreshTokenTable.userId eq EntityID(userId, UserTable) }
            .forEach { it.revokedAt = Instant.now() }
        true
    }

    override suspend fun cleanupExpiredTokens(): Int = query {
        val now = Instant.now()
        val tokens = RefreshTokenDAO.find { RefreshTokenTable.expiresAt less now }.toList()
        tokens.forEach { it.delete() }
        tokens.size
    }
}
