package com.piashcse.utils

import com.piashcse.utils.common.Csv
import kotlin.test.Test
import kotlin.test.assertEquals

class CsvTest {
    @Test
    fun `escapes commas quotes and newlines`() {
        assertEquals("\"a,b\"", Csv.cell("a,b"))
        assertEquals("\"he said \"\"hi\"\"\"", Csv.cell("he said \"hi\""))
        assertEquals("\"line1\nline2\"", Csv.cell("line1\nline2"))
    }

    @Test
    fun `prefixes spreadsheet formulas`() {
        assertEquals("'=1+1", Csv.cell("=1+1"))
        assertEquals("'+cmd", Csv.cell("+cmd"))
        assertEquals("'@SUM(A1)", Csv.cell("@SUM(A1)"))
    }

    @Test
    fun `renders a row`() {
        assertEquals("a,\"b,c\",", Csv.row(listOf("a", "b,c", null)))
    }
}
