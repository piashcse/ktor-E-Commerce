package com.piashcse.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants
import com.piashcse.constants.UserType
import com.piashcse.database.entities.LoginResponse
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserProfileDAO
import com.piashcse.mapper.toUserResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import java.util.UUID

/** Exchanges a verified Google identity for an app session. */
class SocialAuthService(
    private val authRepo: AuthRepository,
    private val googleVerifier: GoogleTokenVerifier,
    private val mfaService: MfaService = MfaService(),
) {
    suspend fun googleLogin(
        idToken: String,
        mfaCode: String? = null,
    ): LoginResponse {
        val identity = googleVerifier.verify(idToken)
        val existing = authRepo.findUserByEmailAndType(identity.email, UserType.CUSTOMER)
        val user =
            existing ?: query {
                UserDAO.new {
                    email = identity.email
                    userType = UserType.CUSTOMER
                    password =
                        BCrypt.withDefaults().hashToString(
                            AppConstants.BCRYPT_COST,
                            UUID.randomUUID().toString().toCharArray(),
                        )
                    isVerified = true
                    isActive = true
                    socialProvider = "GOOGLE"
                    socialSubject = identity.subject
                }.also { UserProfileDAO.new { userId = it.id } }
            }
        if (!user.isActive) throw ValidationException("Account has been deactivated")
        if (user.mfaEnabled) {
            val secret = user.mfaSecret ?: throw ValidationException("MFA is not configured")
            if (mfaCode.isNullOrBlank()) throw com.piashcse.utils.validator.MfaRequiredException()
            if (!mfaService.verifyStoredSecret(secret, mfaCode)) {
                throw com.piashcse.utils.validator.InvalidMfaCodeException()
            }
        }
        if (user.socialSubject == null || !user.isVerified) {
            query {
                val current = UserDAO.findById(user.id) ?: user.id.value.throwNotFound("User")
                current.socialProvider = "GOOGLE"
                current.socialSubject = identity.subject
                current.isVerified = true
            }
        }
        val tokenPair = authRepo.generateTokenPair(user.id.value, user.email, user.userType.name)
        authRepo.storeRefreshToken(user.id.value, tokenPair.refreshToken)
        return LoginResponse(user.toUserResponse(), tokenPair.accessToken, tokenPair.refreshToken, tokenPair.expiresIn)
    }
}
