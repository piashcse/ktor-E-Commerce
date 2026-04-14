package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant
import java.time.LocalDateTime

object LoginAttemptTable : BaseIdTable("login_attempt") {
    val email = varchar("email", 255)
    val userType = varchar("user_type", 20)
    val ipAddress = varchar("ip_address", 45).nullable()
    val attemptCount = integer("attempt_count").default(0)
    val lockedUntil = timestamp("locked_until").nullable()

    // Unique constraint on email + userType
    init {
        uniqueIndex(email, userType)
    }
}

class LoginAttemptDAO(id: EntityID<String>) : BaseEntity(id, LoginAttemptTable) {
    companion object : BaseEntityClass<LoginAttemptDAO>(LoginAttemptTable, LoginAttemptDAO::class.java)

    var email by LoginAttemptTable.email
    var userType by LoginAttemptTable.userType
    var ipAddress by LoginAttemptTable.ipAddress
    var attemptCount by LoginAttemptTable.attemptCount
    var lockedUntil by LoginAttemptTable.lockedUntil

    val isLocked: Boolean get() = lockedUntil?.isAfter(Instant.now()) == true
    val isLockExpired: Boolean get() = lockedUntil?.isBefore(Instant.now()) == true
}
