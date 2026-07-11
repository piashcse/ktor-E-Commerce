package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object OtpAttemptTable : BaseIdTable("otp_attempt") {
    val userId = reference("user_id", UserTable.id).uniqueIndex()
    val attemptCount = integer("attempt_count").default(0)
    val lockedUntil = timestamp("locked_until").nullable()
}

class OtpAttemptDAO(id: EntityID<String>) : BaseEntity(id, OtpAttemptTable) {
    companion object : BaseEntityClass<OtpAttemptDAO>(OtpAttemptTable, OtpAttemptDAO::class.java)

    var userId by OtpAttemptTable.userId
    var attemptCount by OtpAttemptTable.attemptCount
    var lockedUntil by OtpAttemptTable.lockedUntil

    val isLocked: Boolean get() = lockedUntil?.isAfter(Instant.now()) == true
    val isLockExpired: Boolean get() = lockedUntil?.isBefore(Instant.now()) == true
}
