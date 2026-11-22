package com.piashcse.feature.payout

import com.piashcse.constants.KycStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.validator.ForbiddenException
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq

/** Shared seller lookup/eligibility helpers for payout reads and writes. */
internal object PayoutSupport {
    fun sellerIdsFor(sellerUserId: String): List<EntityID<String>> = SellerDAO.find { SellerTable.userId eq sellerUserId }.map { it.id }

    fun requireActiveSeller(sellerUserId: String): SellerDAO {
        val seller = requireSellerByUserId(sellerUserId)
        val user = UserDAO.findById(sellerUserId)
        if (user?.isActive != true) throw ForbiddenException("Seller account is inactive")
        if (seller.status != ShopStatus.APPROVED) throw ForbiddenException("Seller is not approved for payouts")
        if (seller.kycStatus != KycStatus.VERIFIED) {
            throw ForbiddenException("Seller KYC must be verified before payouts")
        }
        return seller
    }
}
