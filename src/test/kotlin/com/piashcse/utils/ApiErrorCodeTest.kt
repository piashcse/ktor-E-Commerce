package com.piashcse.utils

import com.piashcse.utils.common.ApiError
import com.piashcse.utils.common.FieldError
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

    @Test
    fun `factory fills canonical and RFC 9457 fields`() {
        val error =
            ApiError.of(
                message = "bad input",
                code = "VALIDATION_FAILED",
                status = HttpStatusCode.UnprocessableEntity,
                requestId = "req-1",
                errors = listOf(FieldError("email", "invalid")),
            )
        assertEquals("bad input", error.message)
        assertEquals("bad input", error.title)
        assertEquals("VALIDATION_FAILED", error.type)
        assertEquals(422, error.status)
        assertEquals("req-1", error.requestId)
        assertEquals("email", error.errors?.single()?.field)
    }

    @Test
    fun `factory defaults to bad request`() {
        val error = ApiError.of("nope")
        assertEquals("BAD_REQUEST", error.code)
        assertEquals(400, error.status)
        assertEquals("nope", error.title)
    }
}
