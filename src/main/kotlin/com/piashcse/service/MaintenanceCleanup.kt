package com.piashcse.service

import com.piashcse.database.entities.AnalyticsEventTable
import com.piashcse.database.entities.BlacklistedTokenTable
import com.piashcse.database.entities.CarrierWebhookEventTable
import com.piashcse.database.entities.CartItemTable
import com.piashcse.database.entities.DigitalEntitlementTable
import com.piashcse.database.entities.LoginAttemptTable
import com.piashcse.database.entities.NotificationTable
import com.piashcse.database.entities.OtpAttemptTable
import com.piashcse.database.entities.OutboxDeadLetterTable
import com.piashcse.database.entities.OutboxTable
import com.piashcse.database.entities.PasskeyChallengeTable
import com.piashcse.database.entities.PaymentWebhookEventTable
import com.piashcse.database.entities.RefreshTokenTable
import com.piashcse.database.entities.SupportTicketTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.extension.query
import kotlinx.coroutines.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

/**
 * Daily retention job. Flyway is the schema source of truth, but these tables
 * grow monotonically without a cleanup policy:
 * - published outbox rows
 * - expired/revoked refresh tokens
 * - expired blacklisted access tokens
 * - old login/OTP attempt counters
 * - read notifications
 * - provider webhook inboxes (90 days)
 * - analytics events (13 months)
 * - expired digital entitlements
 * - idle carts (30 days)
 * - resolved support tickets (3 years)
 */
object MaintenanceCleanup {
    private val log = LoggerFactory.getLogger(MaintenanceCleanup::class.java)
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job != null) return
        job =
            scope.launch {
                while (isActive) {
                    runCatching { clean() }.onFailure { log.error("Maintenance cleanup failed", it) }
                    delay(Duration.ofHours(24).toMillis())
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    suspend fun clean() =
        query {
            val now = Instant.now()
            val nowLocal = currentUtc()

            OutboxTable.deleteWhere {
                (OutboxTable.publishedAt.isNotNull()) and
                    (OutboxTable.publishedAt less nowLocal.minusDays(7))
            }

            RefreshTokenTable.deleteWhere {
                (RefreshTokenTable.expiresAt less now) or
                    (
                        RefreshTokenTable.revokedAt.isNotNull() and
                            (RefreshTokenTable.revokedAt less now.minus(Duration.ofDays(30)))
                    )
            }

            BlacklistedTokenTable.deleteWhere {
                BlacklistedTokenTable.blacklistedAt less now.minus(Duration.ofDays(2))
            }

            LoginAttemptTable.deleteWhere {
                (LoginAttemptTable.createdAt less nowLocal.minusDays(30)) and
                    (LoginAttemptTable.lockedUntil.isNull())
            }

            OtpAttemptTable.deleteWhere {
                (OtpAttemptTable.createdAt less nowLocal.minusDays(30)) and
                    (OtpAttemptTable.lockedUntil.isNull())
            }

            NotificationTable.deleteWhere {
                (NotificationTable.isRead eq true) and
                    (NotificationTable.type neq "NOTIFICATION_PREFS") and
                    (NotificationTable.createdAt less nowLocal.minusDays(365))
            }

            OutboxDeadLetterTable.deleteWhere {
                OutboxDeadLetterTable.createdAt less nowLocal.minusDays(90)
            }

            PasskeyChallengeTable.deleteWhere {
                PasskeyChallengeTable.expiresAt less nowLocal
            }

            var purged = 0
            // Provider webhook payloads are only needed for replay/debugging for 90 days.
            purged +=
                PaymentWebhookEventTable.deleteWhere {
                    PaymentWebhookEventTable.receivedAt less nowLocal.minusDays(90)
                }
            purged +=
                CarrierWebhookEventTable.deleteWhere {
                    CarrierWebhookEventTable.receivedAt less nowLocal.minusDays(90)
                }

            // Analytics identifiers/events roll out after 13 months.
            purged +=
                AnalyticsEventTable.deleteWhere {
                    AnalyticsEventTable.occurredAt less nowLocal.minusMonths(13)
                }

            purged +=
                DigitalEntitlementTable.deleteWhere {
                    (DigitalEntitlementTable.expiresAt.isNotNull()) and
                        (DigitalEntitlementTable.expiresAt less nowLocal)
                }

            // Abandoned carts are removed after 30 idle days.
            purged +=
                CartItemTable.deleteWhere {
                    CartItemTable.createdAt less nowLocal.minusDays(30)
                }

            // Closed support tickets and their messages follow the 3-year retention window.
            purged +=
                SupportTicketTable.deleteWhere {
                    (SupportTicketTable.resolvedAt.isNotNull()) and
                        (SupportTicketTable.resolvedAt less nowLocal.minusYears(3))
                }
            if (purged > 0) AppMetrics.retentionPurged.increment(purged.toDouble())
        }
}
