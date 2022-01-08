package com.example.utils.extension

import io.ktor.server.plugins.*

/*
inline fun <reified T : Any> T.ktorValidator() {
    val violations = Validation.buildDefaultValidatorFactory().validator.validate(this)
    if (violations.size > 0) {
        // Throw error messages when found violdations
        val details = violations.map {
            val propertyName = it.propertyPath.toString()
            val errorMessage = it.message
            "${propertyName}: $errorMessage"
        }
        // Your custom Exception in Status Page feature of Ktor application
        throw BadRequestException(details.toString())
    }
}*/
