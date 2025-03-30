package com.piashcse.models.bands

import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class BrandRequest(val brandName: String) {
    fun validation() {
        validate(this) {
            validate(BrandRequest::brandName).isNotNull().isNotEmpty()
        }
    }
}