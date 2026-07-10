package com.piashcse.feature.auth

import com.piashcse.model.request.RefreshTokenRequest
import com.piashcse.model.request.TokenPair

class TokenManagementService(private val authRepo: AuthRepository) {

    suspend fun refreshAccessToken(request: RefreshTokenRequest): TokenPair =
        authRepo.refreshAccessToken(request)

    suspend fun logout(userId: String, refreshToken: String?): Boolean =
        authRepo.logout(userId, refreshToken)

    suspend fun blacklistToken(token: String): Boolean =
        authRepo.blacklistToken(token)
}
