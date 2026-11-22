package com.piashcse.feature.auth

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserProfileDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.model.request.RegisterRequest
import com.piashcse.model.response.RegistrationResult
import com.piashcse.utils.common.generateOTP
import com.piashcse.utils.extension.query
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

/** Customer/seller registration, OTP issuance and guest-account claim rules. */
class RegistrationService {
    private val tokens = AuthTokenService()

    suspend fun register(registerRequest: RegisterRequest): RegistrationResult {
        val userTypeEnum = userTypeOrNull(registerRequest.userType) ?: UserType.CUSTOMER
        val normalizedEmail = normalizeEmail(registerRequest.email)

        val (email, otp, result) =
            query {
                val existingUser =
                    UserDAO.find { UserTable.email eq normalizedEmail and (UserTable.userType eq userTypeEnum) }
                        .firstOrNull()

                val otp = generateOTP()
                val otpExpiryTime = currentUtc().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES)

                if (existingUser != null) {
                    if (existingUser.isVerified) throw ValidationException(Message.Auth.USER_EXISTS)
                    if (existingUser.otpExpiry?.isAfter(currentUtc()) == true) {
                        throw ValidationException(Message.Auth.OTP_ALREADY_SENT)
                    }
                    // Guest-checkout rows have a server-generated random password, so
                    // the first real registration may claim them. Any other unverified
                    // row must prove knowledge of the existing password: otherwise an
                    // attacker could register a victim's email and swap in their own
                    // password before the victim completes the OTP (pre-hijacking).
                    val mayReplacePassword =
                        existingUser.isGuest ||
                            PasswordHasher.verify(registerRequest.password, existingUser.password)
                    if (!mayReplacePassword) {
                        throw ValidationException(Message.Auth.USE_PASSWORD_RESET)
                    }
                    existingUser.otpCode = tokens.hashOtp(otp)
                    existingUser.otpExpiry = otpExpiryTime
                    existingUser.password = PasswordHasher.hash(registerRequest.password)
                    Triple(
                        existingUser.email,
                        otp,
                        RegistrationResult.OtpResent(
                            existingUser.id.value,
                            existingUser.email,
                            Message.Auth.OTP_SENT,
                            otp = otp,
                        ),
                    )
                } else {
                    val inserted =
                        UserDAO.new {
                            email = normalizedEmail
                            otpCode = tokens.hashOtp(otp)
                            otpExpiry = otpExpiryTime
                            password = PasswordHasher.hash(registerRequest.password)
                            userType = userTypeEnum
                        }
                    UserProfileDAO.new { userId = inserted.id }
                    if (userTypeEnum == UserType.SELLER) {
                        SellerDAO.new {
                            userId = inserted.id
                            status = ShopStatus.PENDING
                        }
                    }
                    Triple(
                        inserted.email,
                        otp,
                        RegistrationResult.Created(
                            inserted.id.value,
                            normalizedEmail,
                            Message.Auth.OTP_SENT,
                            otp = otp,
                        ),
                    )
                }
            }
        return result
    }

    private fun normalizeEmail(value: String): String = value.trim().lowercase()

    private fun userTypeOrNull(raw: String): UserType? = runCatching { UserType.valueOf(raw.uppercase()) }.getOrNull()

    private fun hashPassword(password: String) = PasswordHasher.hash(password)

    private fun verifyPassword(
        raw: String,
        hash: String,
    ) = PasswordHasher.verify(raw, hash)
}
