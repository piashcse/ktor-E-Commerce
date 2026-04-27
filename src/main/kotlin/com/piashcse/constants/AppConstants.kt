package com.piashcse.constants

/**
 * Application-wide constants.
 *
 * Note: Upload directory configuration has been moved to UploadService
 * which supports configurable paths via UPLOAD_DIR environment variable.
 */
object AppConstants {
    const val APP_VERSION = "1.0.0"

    object DataBaseTransaction {
        const val FOUND = 1
        const val NOT_FOUND = 2
    }

    object SmtpServer {
        const val EMAIL_SUBJECT = "Forget Password"
    }

    /**
     * @deprecated Use UploadService instead.
     * Upload paths are now configurable via UPLOAD_DIR environment variable.
     * See UploadService.kt for details.
     */
    @Deprecated("Use UploadService instead", ReplaceWith("UploadService"))
    object ImageFolder {
        const val PROFILE_IMAGE_LOCATION = "uploads/profile-images/"
        const val PRODUCT_IMAGE_LOCATION = "uploads/product-images/"
    }
}