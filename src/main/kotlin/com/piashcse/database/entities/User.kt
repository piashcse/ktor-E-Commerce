package com.piashcse.database.entities

import com.piashcse.constants.UserType
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object UserTable : BaseIdTable("user") {
    val email = varchar("email", 255) // Nullable for mobile users
    val userType = enumerationByName<UserType>("user_type", 100)
    val password = varchar("password", 200)
    val otpCode = varchar("otp_code", 6)
    val otpExpiry = datetime("otp_expiry").nullable()
    val isVerified = bool("is_verified").default(false)
    val isActive = bool("is_active").default(true)

    // Create a composite unique index on email and userType
    init {
        uniqueIndex("email_userType_idx", email, userType)
    }
}

class UserDAO(id: EntityID<String>) : BaseEntity(id, UserTable) {
    companion object : BaseEntityClass<UserDAO>(UserTable, UserDAO::class.java)

    var email by UserTable.email
    var userType by UserTable.userType
    var password by UserTable.password
    var otpCode by UserTable.otpCode
    var otpExpiry by UserTable.otpExpiry
    var isVerified by UserTable.isVerified
    var isActive by UserTable.isActive

    /**
     * Get the user response with role-based information
     */
    fun response() =
        UserResponse(
            id.value,
            email,
            isVerified,
            userType,
            isActive,
            createdAt,
            updatedAt,
        )

    /**
     * Get the seller information if the user is a seller
     */
    fun getSellerInfo(): SellerResponse? {
        if (userType != UserType.SELLER) return null
        val seller = SellerDAO.find { SellerTable.userId eq id }.singleOrNull()
        return seller?.response()
    }

    /**
     * Check if user is active and verified
     */
    fun isActiveAndVerified(): Boolean = isVerified && isActive
}

data class UserResponse(
    val id: String,
    val email: String,
    val isVerified: Boolean?,
    var userType: UserType,
    val isActive: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)

data class LoginResponse(
    val user: UserResponse?,
    val accessToken: String,
    val refreshToken: String = "",
    val expiresIn: Long = 86400,
    val tokenType: String = "Bearer",
)

data class ChangePassword(val oldPassword: String, val newPassword: String)
