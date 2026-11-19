package com.piashcse.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** At-least-once delivery contract used by the outbox poller. */
class EventBusTest {
    @AfterTest
    fun cleanup() {
        EventBus.stop()
    }

    private fun scope() = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun event(action: String = "TEST") =
        AdminActionEvent(
            actorId = "actor-1",
            actorEmail = "actor@example.com",
            actorRole = "ADMIN",
            action = action,
            resourceType = "TEST",
            resourceId = "resource-1",
        )

    @Test
    fun `publishAndAwait returns true only after subscribers finish`() =
        runBlocking {
            val scope = scope()
            EventBus.start(scope)
            val seen = AtomicInteger()
            EventBus.subscribe(
                object : Subscriber {
                    override suspend fun onEvent(event: DomainEvent) {
                        seen.incrementAndGet()
                    }
                },
            )

            assertTrue(EventBus.publishAndAwait(event()))
            assertEquals(1, seen.get())
            EventBus.stop()
            scope.cancel()
        }

    @Test
    fun `failing subscribers surface a failed acknowledgement`() =
        runBlocking {
            val scope = scope()
            EventBus.start(scope)
            EventBus.subscribe(
                object : Subscriber {
                    override suspend fun onEvent(event: DomainEvent) {
                        error("subscriber down")
                    }
                },
            )
            val before = EventBus.metrics().failed

            assertFalse(EventBus.publishAndAwait(event()))
            assertTrue(EventBus.metrics().failed > before)
            EventBus.stop()
            scope.cancel()
        }

    @Test
    fun `publishing before start is rejected`() {
        EventBus.stop()
        assertFalse(EventBus.publish(event()))
    }
}
