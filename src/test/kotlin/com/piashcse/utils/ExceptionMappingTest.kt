package com.piashcse.utils

import com.piashcse.utils.common.toErrorResponse
import com.piashcse.utils.validator.ConflictException
import com.piashcse.utils.validator.DatabaseException
import com.piashcse.utils.validator.DeactivatedAccountException
import com.piashcse.utils.validator.EmailException
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.InternalServerException
import com.piashcse.utils.validator.InvalidCredentialsException
import com.piashcse.utils.validator.InvalidEnumValueException
import com.piashcse.utils.validator.MissingParameterException
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.RateLimitExceededException
import com.piashcse.utils.validator.UnauthorizedException
import com.piashcse.utils.validator.UnverifiedAccountException
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class ExceptionMappingTest {
    @Test
    fun `client errors carry the right status and machine code`() {
        assertMapping(ValidationException("bad"), HttpStatusCode.BadRequest, "VALIDATION_FAILED")
        assertMapping(
            FieldValidationException(listOf(FieldViolation("name", "blank"))),
            HttpStatusCode.BadRequest,
            "VALIDATION_FAILED",
        )
        assertMapping(InvalidEnumValueException("bad", "E", "X"), HttpStatusCode.BadRequest, "INVALID_ENUM_VALUE")
        assertMapping(MissingParameterException("id"), HttpStatusCode.BadRequest, "MISSING_PARAMETER")
        assertMapping(UnauthorizedException(), HttpStatusCode.Unauthorized, "UNAUTHORIZED")
        assertMapping(InvalidCredentialsException(), HttpStatusCode.Unauthorized, "INVALID_CREDENTIALS")
        assertMapping(UnverifiedAccountException(), HttpStatusCode.Unauthorized, "ACCOUNT_NOT_VERIFIED")
        assertMapping(DeactivatedAccountException(), HttpStatusCode.Unauthorized, "ACCOUNT_DEACTIVATED")
        assertMapping(ForbiddenException(), HttpStatusCode.Forbidden, "FORBIDDEN")
        assertMapping(NotFoundException(), HttpStatusCode.NotFound, "NOT_FOUND")
        assertMapping(ConflictException("dup"), HttpStatusCode.Conflict, "CONFLICT")
        assertMapping(RateLimitExceededException(), HttpStatusCode.TooManyRequests, "RATE_LIMITED")
    }

    @Test
    fun `server errors no longer masquerade as bad request`() {
        assertMapping(InternalServerException(), HttpStatusCode.InternalServerError, "INTERNAL")
        assertMapping(DatabaseException("db"), HttpStatusCode.InternalServerError, "DATABASE_ERROR")
        assertMapping(EmailException("smtp"), HttpStatusCode.InternalServerError, "EMAIL_ERROR")
    }

    @Test
    fun `invalid credentials can include remaining attempts`() {
        assertEquals(
            "Invalid email or password. 2 attempts remaining.",
            InvalidCredentialsException(2).message,
        )
    }

    @Test
    fun `error response populates rfc style aliases and request id`() {
        val (status, body) = NotFoundException("missing").toErrorResponse(requestId = "req-1")
        assertEquals(HttpStatusCode.NotFound, status)
        assertEquals("missing", body.message)
        assertEquals("NOT_FOUND", body.code)
        assertEquals("NOT_FOUND", body.type)
        assertEquals("missing", body.title)
        assertEquals(404, body.status)
        assertEquals("req-1", body.requestId)
    }

    private fun assertMapping(
        exception: com.piashcse.utils.validator.AppException,
        expectedStatus: HttpStatusCode,
        expectedCode: String,
    ) {
        val (status, body) = exception.toErrorResponse()
        assertEquals(expectedStatus, status)
        assertEquals(expectedCode, body.code)
    }
}
