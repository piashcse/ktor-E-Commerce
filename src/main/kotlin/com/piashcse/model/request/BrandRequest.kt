package com.piashcse.model.request

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