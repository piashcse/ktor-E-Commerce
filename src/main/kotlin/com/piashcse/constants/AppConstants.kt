package com.piashcse.constants

object AppConstants {

    object DataBaseTransaction {
        const val FOUND = 1
        const val NOT_FOUND = 2
    }

    object SmtpServer {
        const val HOST_NAME = "smtp.googlemail.com"
        const val PORT = 465
        const val DEFAULT_AUTHENTICATOR = "smtp@gmail.com"
        const val DEFAULT_AUTHENTICATOR_PASSWORD = "smtpcredential"
        const val EMAIL_SUBJECT = "Forget Password"
        const val SENDING_EMAIL = "sendingemail.@gmail.com"
    }

    object ImageFolder {
        const val PROFILE_IMAGE_LOCATION = "src/main/resources/profile-image/"
        const val PRODUCT_IMAGE_LOCATION = "src/main/resources/product-image/"
    }

    /**
     * File upload security constants
     */
    object FileUpload {
        val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "svg")
        val ALLOWED_IMAGE_MIME_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/svg+xml"
        )
        const val MAX_IMAGE_SIZE = 5 * 1024 * 1024L // 5MB
        const val MAX_PROFILE_IMAGE_SIZE = 2 * 1024 * 1024L // 2MB
    }

    /**
     * Pagination defaults
     */
    object Pagination {
        const val DEFAULT_LIMIT = 20
        const val MAX_LIMIT = 100
        const val DEFAULT_OFFSET = 0
    }

    /**
     * Account security
     */
    object Security {
        const val MAX_FAILED_LOGIN_ATTEMPTS = 5
        const val ACCOUNT_LOCKOUT_DURATION_MINUTES = 30
    }

    /**
     * OTP configuration
     */
    object OTP {
        const val LENGTH = 6
        const val EXPIRY_HOURS = 24
    }
}