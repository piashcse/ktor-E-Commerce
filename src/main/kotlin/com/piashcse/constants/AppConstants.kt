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

}
