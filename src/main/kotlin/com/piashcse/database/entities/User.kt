package com.piashcse.database.entities

import com.piashcse.constants.UserType
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

object UserTable : BaseIdTable("user") {
    val email = varchar("email", 255)
    val userType = enumerationByName<UserType>("user_type", 100)
    val password = varchar("password", 200)
    val otpCode = varchar("otp_code", 6).nullable()
    val otpExpiry = datetime("otp_expiry").nullable()
    val resetOtpCode = varchar("reset_otp_code", 6).nullable()
    val resetOtpExpiry = datetime("reset_otp_expiry").nullable()
    val isVerified = bool("is_verified").default(false)
    val isActive = bool("is_active").default(true)

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
    var resetOtpCode by UserTable.resetOtpCode
    var resetOtpExpiry by UserTable.resetOtpExpiry
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

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val isVerified: Boolean?,
    var userType: UserType,
    val isActive: Boolean,
    val createdAt: @Contextual LocalDateTime?,
    val updatedAt: @Contextual LocalDateTime?,
)

@Serializable
data class LoginResponse(
    val user: UserResponse?,
    val accessToken: String,
    val refreshToken: String = "",
    val expiresIn: Long = 900,
    val tokenType: String = "Bearer",
)

@Serializable
data class ChangePassword(val oldPassword: String, val newPassword: String)
