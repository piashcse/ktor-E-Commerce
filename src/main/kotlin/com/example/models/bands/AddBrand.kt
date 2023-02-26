package com.example.models.bands

import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddBrand(val brandName: String) {
    fun validation() {
        validate(this) {
            validate(AddBrand::brandName).isNotNull()
        }
    }
}