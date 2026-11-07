package com.piashcse.utils.common

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Single UTC wire format for timestamps: ISO-8601 with an explicit offset. */
object DateTimes {
    val FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun LocalDateTime.toUtcString(): String = atOffset(ZoneOffset.UTC).format(FORMAT)

    fun LocalDateTime?.toUtcStringOrNull(): String? = this?.toUtcString()
}
