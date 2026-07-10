package com.piashcse.mapper

import com.piashcse.database.entities.*
import com.piashcse.model.response.UserProfileResponse
import org.jetbrains.exposed.v1.core.eq

fun UserDAO.toUserResponse() = UserResponse(
    id.value, email, isVerified, userType, isActive, createdAt, updatedAt,
)

fun UserDAO.toSellerInfo(): SellerResponse? {
    if (userType != com.piashcse.constants.UserType.SELLER) return null
    val seller = SellerDAO.find { SellerTable.userId eq id }.singleOrNull()
    return seller?.toSellerResponse()
}

fun SellerDAO.toSellerResponse() = SellerResponse(
    id = id.value,
    userId = userId.value,
    shopId = shopId?.value,
    businessName = businessName,
    businessRegistrationNumber = businessRegistrationNumber,
    taxId = taxId,
    bankAccountNumber = bankAccountNumber,
    bankName = bankName,
    bankRoutingNumber = bankRoutingNumber,
    commissionRate = commissionRate,
    status = status,
    totalSales = totalSales,
    totalCommission = totalCommission,
    approvedAt = approvedAt,
    suspendedAt = suspendedAt,
    terminatedAt = terminatedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun UserProfileDAO.toUserProfileResponse() = UserProfileResponse(
    userId = userId.value,
    image = image,
    firstName = firstName,
    lastName = lastName,
    mobile = mobile,
    faxNumber = faxNumber,
    streetAddress = streetAddress,
    city = city,
    state = state,
    country = country,
    identificationType = identificationType,
    identificationNo = identificationNo,
    occupation = occupation,
    postCode = postCode,
    gender = gender,
    dateOfBirth = dateOfBirth,
    bio = bio,
    isActive = isActive,
    verified = verified,
)
