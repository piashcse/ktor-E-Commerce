package com.piashcse.service

import com.piashcse.database.entities.BlacklistedTokenTable
import com.piashcse.database.entities.LoginAttemptTable
import com.piashcse.database.entities.NotificationTable
import com.piashcse.database.entities.OtpAttemptTable
import com.piashcse.database.entities.OutboxTable
import com.piashcse.database.entities.RefreshTokenTable
import com.piashcse.utils.extension.query
import kotlinx.coroutines.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Daily retention job. Flyway is the schema source of truth, but these tables
 * grow monotonically without a cleanup policy:
 * - published outbox rows
 * - expired/revoked refresh tokens
 * - expired blacklisted access tokens
 * - old login/OTP attempt counters
 * - read notifications
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
            val nowLocal = LocalDateTime.now(ZoneOffset.UTC)

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
                    (NotificationTable.createdAt less nowLocal.minusDays(90))
            }
        }
}
