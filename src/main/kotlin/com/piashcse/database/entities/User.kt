package com.piashcse.database.entities

import com.piashcse.constants.UserType
import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import com.piashcse.feature.auth.JwtConfig
import com.piashcse.model.request.JwtTokenRequest
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
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

    /**
     * Get the user response with role-based information
     */
    fun response() = UserResponse(
        id.value,
        email,
        isVerified,
        userType,
        isActive,
        createdAt,
        updatedAt
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
     * Generate login response with JWT token
     */
    fun loggedInWithToken() = LoginResponse(
        response(), JwtConfig.tokenProvider(JwtTokenRequest(id.value, email, userType.name))
    )

    /**
     * Check if the user has a specific role
     */
    fun hasRole(role: UserType): Boolean = this.userType == role

    /**
     * Check if the user has access to a specific role (with hierarchy)
     */
    fun hasAccessTo(role: UserType): Boolean = com.piashcse.utils.RoleHierarchy.hasAccess(this.userType, role)

    /**
     * Check if user is active and verified
     */
    fun isActiveAndVerified(): Boolean = isVerified && isActive

    /**
     * Check if user is Super Admin
     */
    fun isSuperAdmin(): Boolean = userType == UserType.SUPER_ADMIN

    /**
     * Check if user is Admin
     */
    fun isAdmin(): Boolean = userType == UserType.ADMIN || userType == UserType.SUPER_ADMIN

    /**
     * Check if user is Seller
     */
    fun isSeller(): Boolean = userType == UserType.SELLER

    /**
     * Check if user is Customer
     */
    fun isCustomer(): Boolean = userType == UserType.CUSTOMER || userType == UserType.SELLER ||
                                userType == UserType.ADMIN || userType == UserType.SUPER_ADMIN
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