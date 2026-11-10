package com.piashcse.constants

/**
 * Application-wide constants.
 *
 * Note: Upload directory configuration has been moved to UploadService
 * which supports configurable paths via UPLOAD_DIR environment variable.
 */
object AppConstants {
    const val APP_VERSION = "1.0.0"
    const val DEFAULT_TAX_PERCENTAGE = 0.05

    const val LOYALTY_POINTS_PER_USD = 1
    const val LOYALTY_POINTS_PER_USD_REDEEM = 100
    const val REFERRAL_REWARD_POINTS = 100

    const val BCRYPT_COST = 12
    const val OTP_EXPIRY_MINUTES = 10L

    object SmtpServer {
        const val OTP_SUBJECT = "Account Verification"
        const val RESET_SUBJECT = "Password Reset"
    }

    object Pagination {
        const val DEFAULT_LIMIT = 20
        const val MAX_LIMIT = 100
        const val DEFAULT_OFFSET = 0
    }

    object Products {
        const val BEST_SELLER_THRESHOLD = 10
    }

    object Authentication {
        const val JWT_AUTHENTICATOR = "jwt-auth"
        const val API_KEY_AUTHENTICATOR = "api-key"
        const val REFRESH_TOKEN_EXPIRY_SECONDS = 7L * 24 * 60 * 60
        const val JWT_EXPIRY_SECONDS = 900L
        const val MAX_LOGIN_ATTEMPTS = 5
        const val ACCOUNT_LOCKOUT_MINUTES = 30L
        const val MAX_OTP_ATTEMPTS = 5
        const val OTP_LOCKOUT_MINUTES = 30L
    }
}
