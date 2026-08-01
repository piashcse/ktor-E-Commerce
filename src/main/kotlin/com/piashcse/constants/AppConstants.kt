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

    object Inventory {
        const val DEFAULT_MIN_STOCK = 10
        const val DEFAULT_MAX_STOCK = 1000
    }

    object Authentication {
        const val JWT_AUTHENTICATOR = "jwt-auth"
        const val REFRESH_TOKEN_EXPIRY_SECONDS = 7L * 24 * 60 * 60
        const val JWT_EXPIRY_SECONDS = 900L
        const val MAX_LOGIN_ATTEMPTS = 5
        const val ACCOUNT_LOCKOUT_MINUTES = 30L
        const val MAX_OTP_ATTEMPTS = 5
        const val OTP_LOCKOUT_MINUTES = 30L
    }
}
