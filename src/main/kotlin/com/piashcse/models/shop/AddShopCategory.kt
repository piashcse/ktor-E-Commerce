package com.piashcse.models.shop

import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddShopCategory( val name: String) {
    fun validation() {
        validate(this) {
            validate(AddShopCategory::name).isNotNull()
        }
    }
}
