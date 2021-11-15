package com.example.utils

object AppConstants {
    val ALL_USERS_TYPE = listOf(UserType.SUPER_ADMIN, UserType.ADMIN, UserType.MERCHANT, UserType.CUSTOMER)
    object UserType {
        const val SUPER_ADMIN = "1"
        const val ADMIN = "2"
        const val MERCHANT = "3"
        const val CUSTOMER = "4"
    }
    object RoleManagement{
        const val SUPER_ADMIN = "super_admin"
        const val ADMIN = "admin"
        const val MERCHANT = "merchant"
        //const val CUSTOMER = "" default auth will be customer
    }
    object ErrorMessage {
        const val UNAUTHORIZED = "Unauthorized api call"
        const val INTERNAL_SERVER_ERROR = "Internal server error"
        const val BAD_REQUEST = "Parameter mismatch"
        const val USER_NOT_EXIT = "User not exist"
        const val IMAGE_UPLOAD_FAILED = "Image upload is failed"
        object MissingParameter{
            const val PROFILE_ID ="profileId"
            const val USER_ID ="userId"
        }
    }
    object SuccessMessage{
        object Password{
            const val PASSWORD_CHANGE_SUCCESS = "Password change successful"
        }
        object VerificationCode{
            const val VERIFICATION_CODE_SEND_TO =  "verification code send to"
            const val VERIFICATION_CODE_IS_NOT_VALID = "Verification code is not valid"
        }
    }
    object DataBaseTransaction{
        const val FOUND = 1
        const val NOT_FOUND = 2
    }
    object SmtpServer{
        const val HOST_NAME = "smtp.googlemail.com"
        const val PORT = 465
        const val DEFAULT_AUTHENTICATOR = "piashofficial599@gmail.com"
        const val DEFAULT_AUTHENTICATOR_PASSWORD = "qfzjsvdborylnaqh"
        const val EMAIL_SUBJECT = "Forget Password"
    }
    object Image{
        const val IMAGE_FOLDER_LOCATION = "src/main/resources/uploads/"
    }
}