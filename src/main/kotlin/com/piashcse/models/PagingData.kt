package com.piashcse.models

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotZero
import org.valiktor.validate

data class PagingData(@QueryParam("limit") val limit: Int, @QueryParam("offset") val offset: Long) {
    fun validation() {
        validate(this) {
            validate(PagingData::limit).isNotNull().isNotZero()
            validate(PagingData::offset).isNotNull()
        }
    }
}
