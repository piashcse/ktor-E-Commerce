package com.piashcse.utils

import com.piashcse.utils.resilience.CircuitBreaker
import com.piashcse.utils.resilience.CircuitOpenException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CircuitBreakerTest {
    @Test
    fun `opens after threshold and rejects calls`() {
        runBlocking {
            var now = 1_000L
            val breaker = CircuitBreaker("test", failureThreshold = 2, cooldownMs = 1_000, clock = { now })
            repeat(2) {
                assertFailsWith<IllegalStateException> {
                    breaker.execute<Unit> { throw IllegalStateException("boom") }
                }
            }
            assertEquals(CircuitBreaker.State.OPEN, breaker.state())
            assertFailsWith<CircuitOpenException> {
                breaker.execute { Unit }
            }
        }
    }

    @Test
    fun `half-open probe closes on success`() {
        runBlocking {
            var now = 1_000L
            val breaker = CircuitBreaker("test", failureThreshold = 1, cooldownMs = 500, clock = { now })
            assertFailsWith<IllegalStateException> {
                breaker.execute<Unit> { throw IllegalStateException("boom") }
            }
            assertEquals(CircuitBreaker.State.OPEN, breaker.state())

            now += 600
            assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.state())
            val value = breaker.execute { 42 }
            assertEquals(42, value)
            assertEquals(CircuitBreaker.State.CLOSED, breaker.state())
        }
    }

    @Test
    fun `jittered retry eventually succeeds`() {
        runBlocking {
            var attempts = 0
            val result =
                com.piashcse.utils.resilience.Retry.withJitter(attempts = 3, baseDelayMs = 1, maxDelayMs = 2) {
                    attempts += 1
                    if (attempts < 3) error("flaky")
                    "ok"
                }
            assertTrue(attempts == 3)
            assertEquals("ok", result)
        }
    }
}
