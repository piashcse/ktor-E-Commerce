package com.piashcse.utils.resilience

import com.piashcse.utils.validator.AppException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlin.random.Random

class CircuitOpenException(message: String) :
    AppException(message, HttpStatusCode.ServiceUnavailable, "CIRCUIT_OPEN")

/**
 * Small thread-safe circuit breaker for outbound integrations (payment gateway,
 * SMTP, carriers). After [failureThreshold] consecutive failures the circuit
 * opens for [cooldownMs]; the next request after the cooldown is a single
 * half-open probe. A failed probe opens the circuit again.
 */
class CircuitBreaker(
    private val name: String,
    private val failureThreshold: Int = 5,
    private val cooldownMs: Long = 30_000,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    enum class State { CLOSED, OPEN, HALF_OPEN }

    private val lock = Any()
    private var failures = 0
    private var openUntil = 0L
    private var halfOpenProbeInFlight = false

    fun state(): State =
        synchronized(lock) {
            when {
                openUntil == 0L -> State.CLOSED
                openUntil > clock() -> State.OPEN
                else -> State.HALF_OPEN
            }
        }

    suspend fun <T> execute(block: suspend () -> T): T {
        val now = clock()
        synchronized(lock) {
            if (openUntil > now) {
                throw CircuitOpenException("Circuit '$name' is open; retry later")
            }
            if (openUntil != 0L && openUntil <= now) {
                if (halfOpenProbeInFlight) {
                    throw CircuitOpenException("Circuit '$name' is half-open; probe already in flight")
                }
                halfOpenProbeInFlight = true
            }
        }

        return try {
            val result = block()
            synchronized(lock) {
                failures = 0
                openUntil = 0L
                halfOpenProbeInFlight = false
            }
            result
        } catch (e: Throwable) {
            synchronized(lock) {
                halfOpenProbeInFlight = false
                failures += 1
                if (failures >= failureThreshold) {
                    openUntil = clock() + cooldownMs
                }
            }
            throw e
        }
    }
}

/** Retry helper with exponential backoff and jitter for transient failures. */
object Retry {
    suspend fun <T> withJitter(
        attempts: Int = 3,
        baseDelayMs: Long = 200,
        maxDelayMs: Long = 2_000,
        block: suspend (attempt: Int) -> T,
    ): T {
        require(attempts > 0) { "attempts must be positive" }
        var last: Throwable? = null
        repeat(attempts) { index ->
            try {
                return block(index + 1)
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                last = e
                if (index < attempts - 1) {
                    val exponential = baseDelayMs * (1L shl index)
                    val capped = exponential.coerceAtMost(maxDelayMs)
                    delay(capped / 2 + Random.nextLong(capped / 2 + 1))
                }
            }
        }
        throw last ?: IllegalStateException("Retry failed")
    }
}
