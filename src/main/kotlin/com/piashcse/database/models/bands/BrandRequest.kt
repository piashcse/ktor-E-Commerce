package com.piashcse.database.models.bands

import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class BrandRequest(val name: String) {
    fun validation() {
        validate(this) {
            validate(BrandRequest::name).isNotNull().isNotEmpty()
        }
    }
}