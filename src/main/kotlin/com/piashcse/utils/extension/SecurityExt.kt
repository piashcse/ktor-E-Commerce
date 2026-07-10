package com.piashcse.utils.extension

import com.piashcse.constants.Message
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.utils.validator.ForbiddenException
import org.jetbrains.exposed.v1.core.eq

/**
 * Verifies that the resource is owned by the specified user ID.
 * Throws ForbiddenException if the user is not the owner.
 */
fun <T : BaseEntity> T.verifyOwnership(expectedUserId: String, resourceName: String, getUserId: (T) -> String): T {
    if (getUserId(this) != expectedUserId) {
        throw ForbiddenException(Message.Errors.notOwner(resourceName))
    }
    return this
}

/** Finds the seller record for a given user ID. */
fun findSellerByUserId(userId: String): SellerDAO? =
    SellerDAO.find { SellerTable.userId eq userId }.firstOrNull()

/** Finds the seller record for a given user ID or throws ForbiddenException. */
fun requireSellerByUserId(userId: String): SellerDAO =
    findSellerByUserId(userId) ?: throw ForbiddenException(Message.Errors.SELLER_REQUIRED)

/** Checks if a seller owns a specific shop. Uses the provided seller or looks it up. */
fun sellerOwnsShop(userId: String, shopId: String): Boolean =
    sellerOwnsShop(findSellerByUserId(userId), shopId)

fun sellerOwnsShop(seller: SellerDAO?, shopId: String): Boolean =
    seller?.shopId?.value == shopId
