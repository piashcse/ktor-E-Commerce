package com.piashcse.utils.extension

import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import org.jetbrains.exposed.v1.core.eq

/** Finds the seller record for a given user ID. */
fun findSellerByUserId(userId: String): SellerDAO? =
    SellerDAO.find { SellerTable.userId eq userId }.firstOrNull()

/** Checks if a seller owns a specific shop. Uses the provided seller or looks it up. */
fun sellerOwnsShop(userId: String, shopId: String): Boolean =
    sellerOwnsShop(findSellerByUserId(userId), shopId)

fun sellerOwnsShop(seller: SellerDAO?, shopId: String): Boolean =
    seller?.shopId?.value == shopId
