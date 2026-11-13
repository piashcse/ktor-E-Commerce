package com.piashcse.utils.common

import com.piashcse.utils.validator.ValidationException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Single UTC wire format for timestamps: ISO-8601 with an explicit offset. */
object DateTimes {
    val FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun LocalDateTime.toUtcString(): String = atOffset(ZoneOffset.UTC).format(FORMAT)

    fun LocalDateTime?.toUtcStringOrNull(): String? = this?.toUtcString()

    /**
     * Parses the date/time shapes accepted by the API (`LocalDateTime`,
     * `LocalDate`, or offset-aware ISO-8601) and normalizes offsets to UTC.
     * All services use this instead of duplicating the same try/catch chain.
     */
    fun parseFlexible(
        value: String,
        field: String,
        endOfDay: Boolean = false,
    ): LocalDateTime =
        runCatching { LocalDateTime.parse(value) }
            .recoverCatching {
                val date = LocalDate.parse(value)
                if (endOfDay) date.atTime(LocalTime.MAX) else date.atStartOfDay()
            }
            .recoverCatching {
                OffsetDateTime.parse(value).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
            }
            .getOrElse { throw ValidationException("Invalid $field: $value") }

    fun parseFlexibleOrNull(
        value: String?,
        field: String,
        endOfDay: Boolean = false,
    ): LocalDateTime? = value?.takeIf { it.isNotBlank() }?.let { parseFlexible(it, field, endOfDay) }

    fun parseLocalDate(
        value: String,
        field: String,
    ): LocalDate =
        runCatching { LocalDate.parse(value) }
            .getOrElse { throw ValidationException("Invalid $field: $value (expected yyyy-MM-dd)") }
}
