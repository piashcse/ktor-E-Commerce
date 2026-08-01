package com.piashcse.model.request

import kotlinx.serialization.Serializable

@Serializable
data class ChangePassword(val oldPassword: String, val newPassword: String)
