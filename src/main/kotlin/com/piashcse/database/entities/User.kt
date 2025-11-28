package com.piashcse.database.entities

import com.piashcse.constants.UserType
import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import com.piashcse.feature.auth.JwtConfig
import com.piashcse.model.request.JwtTokenRequest
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object UserTable : BaseIntIdTable("user") {
    val email = varchar("email", 255) // Nullable for mobile users
    val userType = enumerationByName<UserType>("user_type", 100)
    val password = varchar("password", 200)
    val otpCode = varchar("otp_code", 6)
    val otpExpiry = datetime("otp_expiry").nullable()
    val isVerified = bool("is_verified").default(false)
    val isActive = bool("is_active").default(true)
    override val primaryKey = PrimaryKey(id)

    // Create a composite unique index on email and userType
    init {
        uniqueIndex("email_userType_idx", email, userType)
    }
}

class UserDAO(id: EntityID<String>) : BaseIntEntity(id, UserTable) {
    companion object : BaseIntEntityClass<UserDAO>(UserTable, UserDAO::class.java)

    var email by UserTable.email
    var userType by UserTable.userType
    var password by UserTable.password
    var otpCode by UserTable.otpCode
    var otpExpiry by UserTable.otpExpiry
    var isVerified by UserTable.isVerified
    var isActive by UserTable.isActive

    fun response() = UserResponse(
        id.value,
        email,
        isVerified,
        userType,
        isActive,
        createdAt,
        updatedAt
    )

    fun loggedInWithToken() = LoginResponse(
        response(), JwtConfig.tokenProvider(JwtTokenRequest(id.value, email, userType.name))
    )
}

data class UserResponse(
    val id: String,
    val email: String,
    val isVerified: Boolean?,
    var userType: UserType,
    val isActive: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)

data class LoginResponse(val user: UserResponse?, val accessToken: String)
data class ChangePassword(val oldPassword: String, val newPassword: String)