package com.piashcse.utils

import com.piashcse.utils.db.isCheckViolation
import com.piashcse.utils.db.isDataException
import com.piashcse.utils.db.isForeignKeyViolation
import com.piashcse.utils.db.isNotNullViolation
import com.piashcse.utils.db.isUniqueViolation
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SqlErrorsTest {
    private fun sql(state: String) = SQLException("constraint failure", state)

    @Test
    fun `detects unique violations through nested causes`() {
        assertTrue(sql("23505").isUniqueViolation())
        assertTrue(RuntimeException("wrapped", sql("23505")).isUniqueViolation())
        assertFalse(sql("23503").isUniqueViolation())
    }

    @Test
    fun `detects foreign key and not-null violations`() {
        assertTrue(sql("23503").isForeignKeyViolation())
        assertTrue(RuntimeException(sql("23503")).isForeignKeyViolation())
        assertTrue(sql("23502").isNotNullViolation())
        assertFalse(sql("23505").isNotNullViolation())
    }

    @Test
    fun `detects check violations`() {
        assertTrue(sql("23514").isCheckViolation())
        assertFalse(RuntimeException("plain").isCheckViolation())
    }

    @Test
    fun `detects data exceptions from client payloads`() {
        assertTrue(sql("22001").isDataException())
        assertTrue(sql("22P02").isDataException())
        assertFalse(sql("23505").isDataException())
    }
}
