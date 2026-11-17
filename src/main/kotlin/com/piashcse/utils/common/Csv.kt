package com.piashcse.utils.common

/** RFC-4180-ish CSV helper with spreadsheet formula-injection protection. */
object Csv {
    fun row(values: List<Any?>): String = values.joinToString(",") { cell(it) }

    fun cell(value: Any?): String {
        if (value == null) return ""
        var text = value.toString()
        // Prevent CSV injection in spreadsheet apps.
        if (text.isNotEmpty() && text[0] in listOf('=', '+', '-', '@', '\t', '\r')) {
            text = "'$text"
        }
        val needsQuotes = text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"${text.replace("\"", "\"\"")}\"" else text
    }
}
