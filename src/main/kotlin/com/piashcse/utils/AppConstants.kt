package com.piashcse.utils

object AppConstants {
    object SuccessMessage {
        object Password {
            const val PASSWORD_CHANGE_SUCCESS = "Password change successful"
        }

        object VerificationCode {
            const val VERIFICATION_CODE_SENT_TO = "verification code sent to"
            const val VERIFICATION_CODE_IS_NOT_VALID = "Verification code is not valid"
        }
    }

    object DataBaseTransaction {
        const val FOUND = 1
        const val NOT_FOUND = 2
    }

    object SmtpServer {
        const val HOST_NAME = "smtp.googlemail.com"
        const val PORT = 465
        const val DEFAULT_AUTHENTICATOR = "piashofficial599@gmail.com"
        const val DEFAULT_AUTHENTICATOR_PASSWORD = "qfzjsvdborylnaqh"
        const val EMAIL_SUBJECT = "Forget Password"
    }

    object Image {
        const val PROFILE_IMAGE_LOCATION = "src/main/resources/profile-image/"
        const val PRODUCT_IMAGE_LOCATION = "src/main/resources/product-image/"
    }

    object ProductVariant {
        const val COLOR = "COLOR"
        const val SIZE = "SIZE"
    }
}