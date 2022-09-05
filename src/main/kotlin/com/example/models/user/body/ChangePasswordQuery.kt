package com.example.models.user.body

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam

data class ChangePasswordQuery(@QueryParam("userId") val userId: String? = null)