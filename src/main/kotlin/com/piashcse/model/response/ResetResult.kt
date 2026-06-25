package com.piashcse.model.response

sealed class ResetResult {
    data object Success : ResetResult()
    data object InvalidOrExpiredOtp : ResetResult()
}
