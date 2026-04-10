package com.piashcse.constants

object Message {
    const val USER_ALREADY_EXIST_WITH_THIS_EMAIL = "User already exists with this email"
    const val NEW_PASSWORD_CANNOT_BE_SAME_AS_CURRENT_PASSWORD = "New password cannot be the same as current password"
    const val NEW_PASSWORD_CANNOT_BE_SAME_AS_OLD_PASSWORD = "New password cannot be the same as old password"
    const val OTP_ALREADY_SENT_WAIT_UNTIL_EXPIRY = "OTP already sent, wait until expiry"
    const val ACCOUNT_NOT_VERIFIED = "Account is not verified"
    const val NEW_OTP_SENT_TO = "New OTP sent to"
    const val OTP_SENT_TO = "OTP sent to"
    const val USER_NOT_EXIST = "User not exist"
    const val PASSWORD_IS_WRONG = "Password is wrong"
    const val TYPE_CAST_EXCEPTION = "Type cast exception"
    const val NULL_POINTER_ERROR = "Null pointer error"
    const val INTERNAL_SERVER_ERROR = "Internal server error"
    const val UNAUTHORIZED_API_CALL = "Unauthorized api call"
    const val PASSWORD_CHANGE_SUCCESS = "Password change successful"
    const val VERIFICATION_CODE_SENT_TO = "Verification code sent to"
    const val VERIFICATION_CODE_IS_NOT_VALID = "Verification code is not valid"
    const val SENDING_EMAIL_FAILED = "Sending email failed"
    const val YOU_ARE_NOT_ALLOWED_TO_SET_STATUS = "You are not allowed to set status to"
    const val INVALID_ORDER_STATUS = "Invalid order status:"
    const val ACCOUNT_DEACTIVATED = "Account has been deactivated"

    // Account lockout messages
    const val ACCOUNT_LOCKED_TRY_AGAIN_LATER = "Account is temporarily locked. Please try again later"
    const val ACCOUNT_LOCKED_DUE_TO_FAILED_ATTEMPTS = "Account locked due to too many failed login attempts"

    // Rate limiting messages
    const val TOO_MANY_REQUESTS = "Too many requests. Please try again later"

    // File upload messages
    const val INVALID_FILE_TYPE = "Invalid file type. Allowed types: jpg, jpeg, png, gif, webp"
    const val FILE_TOO_LARGE = "File too large. Maximum size: 5MB"
    const val INVALID_MIME_TYPE = "Invalid file MIME type"

    // Payment messages
    const val PAYMENT_NOT_FOUND = "Payment not found"
    const val REFUND_PROCESSED_SUCCESS = "Refund processed successfully"
    const val PAYMENT_MARKED_AS_PAID = "Payment marked as paid"
}