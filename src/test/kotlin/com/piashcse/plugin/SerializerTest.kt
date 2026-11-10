package com.piashcse.plugin

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializerTest {
    @Test
    fun `big decimal serializes as a string`() {
        assertEquals("\"19.90\"", Json.encodeToString(BigDecimalSerializer, BigDecimal("19.90")))
    }

    @Test
    fun `big decimal accepts strings and json numbers`() {
        assertEquals(BigDecimal("19.90"), Json.decodeFromString(BigDecimalSerializer, "\"19.90\""))
        assertEquals(BigDecimal("19.90"), Json.decodeFromString(BigDecimalSerializer, "19.90"))
    }

    @Test
    fun `local date time serializes as utc with offset`() {
        val value = LocalDateTime.of(2024, 5, 6, 12, 30, 15)
        assertEquals("\"2024-05-06T12:30:15Z\"", Json.encodeToString(LocalDateTimeSerializer, value))
    }

    @Test
    fun `local date time accepts offset and local input`() {
        val expected = LocalDateTime.of(2024, 5, 6, 12, 30, 15)
        assertEquals(expected, Json.decodeFromString(LocalDateTimeSerializer, "\"2024-05-06T12:30:15Z\""))
        assertEquals(expected, Json.decodeFromString(LocalDateTimeSerializer, "\"2024-05-06T12:30:15\""))
    }

    @Test
    fun `local date round trips`() {
        val value = LocalDate(2024, 5, 6)
        assertEquals("\"2024-05-06\"", Json.encodeToString(LocalDateSerializer, value))
        assertEquals(value, Json.decodeFromString(LocalDateSerializer, "\"2024-05-06\""))
    }
}
