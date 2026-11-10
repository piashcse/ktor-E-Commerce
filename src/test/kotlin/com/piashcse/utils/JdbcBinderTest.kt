package com.piashcse.utils

import com.piashcse.utils.db.bindParams
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.sql.PreparedStatement
import kotlin.test.Test
import kotlin.test.assertEquals

class JdbcBinderTest {
    @Test
    fun `binds supported parameter types and advances the index`() {
        val statement = mockk<PreparedStatement>(relaxed = true)
        val next =
            statement.bindParams(
                listOf(1, 2L, "text", 1.5, BigDecimal("2.50"), true),
                startIdx = 3,
            )

        assertEquals(9, next)
        verify { statement.setInt(3, 1) }
        verify { statement.setLong(4, 2L) }
        verify { statement.setString(5, "text") }
        verify { statement.setBigDecimal(6, BigDecimal("1.5")) }
        verify { statement.setBigDecimal(7, BigDecimal("2.50")) }
        verify { statement.setObject(8, true) }
    }

    @Test
    fun `empty parameter list leaves the index untouched`() {
        val statement = mockk<PreparedStatement>(relaxed = true)
        assertEquals(5, statement.bindParams(emptyList(), startIdx = 5))
    }
}
