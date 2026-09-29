package com.piashcse.utils

import com.piashcse.utils.common.ApiError
import com.piashcse.utils.common.toErrorResponse
import com.piashcse.utils.validator.ConflictException
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiErrorCodeTest {
    @Test
    fun `validation maps to code`() {
        val (status, body) = ValidationException("bad").toErrorResponse()
        assertEquals(HttpStatusCode.BadRequest, status)
        assertEquals("VALIDATION_FAILED", body.code)
    }

    @Test
    fun `not found maps to code`() {
        val (status, body: ApiError) = NotFoundException("missing").toErrorResponse()
        assertEquals(HttpStatusCode.NotFound, status)
        assertEquals("NOT_FOUND", body.code)
    }

    @Test
    fun `conflict maps to code`() {
        val (status, body) = ConflictException("dup").toErrorResponse()
        assertEquals(HttpStatusCode.Conflict, status)
        assertEquals("CONFLICT", body.code)
    }
}
