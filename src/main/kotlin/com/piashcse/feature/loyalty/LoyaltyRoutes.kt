@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.loyalty

import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.standardErrors
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Customer wallet, ledger, gift-card balance and referrals. */
fun Route.walletRoutes() {
    val loyalty: LoyaltyService by inject()
    requireRole {
        get {
            call.respondOk(loyalty.wallet(call.currentUserId))
        }.describe {
            summary = "Get my loyalty wallet"
            responses {
                ok<WalletResponse>("Wallet")
                standardErrors()
            }
        }

        get("ledger") {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(loyalty.ledger(call.currentUserId, limit, offset))
        }.describe { summary = "List my wallet ledger" }

        get("gift-cards/{code}") {
            call.respondOk(loyalty.giftCard(call.requirePathParameter("code")))
        }.describe { summary = "Check a gift-card balance" }
    }
}

/** Customer referral claim/history. */
fun Route.referralRoutes() {
    val loyalty: LoyaltyService by inject()
    requireRole {
        get {
            call.respondOk(loyalty.listReferrals(call.currentUserId))
        }.describe { summary = "List my referrals" }

        post("claim") {
            call.respondCreated(loyalty.claimReferral(call.currentUserId, call.receive<ReferralClaimRequest>().code))
        }.describe {
            summary = "Claim a referral code"
            requestBody { jsonBody<ReferralClaimRequest>() }
            responses {
                created<ReferralResponse>("Referral claimed")
                standardErrors()
            }
        }
    }
}

/** Admin gift-card issuance and lifecycle. */
fun Route.giftCardAdminRoutes() {
    val loyalty: LoyaltyService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(loyalty.listGiftCards(limit, offset))
    }.describe { summary = "List gift cards" }

    adminWriteRateLimit {
        post {
            call.respondCreated(loyalty.issueGiftCard(call.currentUserId, call.receive<GiftCardIssueRequest>()))
        }.describe {
            summary = "Issue a gift card"
            requestBody { jsonBody<GiftCardIssueRequest>() }
            responses { created<GiftCardResponse>("Gift card issued") }
        }

        delete("{id}") {
            val id = call.requirePathParameter("id")
            if (!loyalty.voidGiftCard(id)) {
                throw com.piashcse.utils.validator.NotFoundException("Gift card not found")
            }
            call.respondOk(MessageResponse("Gift card voided"))
        }.describe { summary = "Void a gift card" }
    }
}

/** Admin store-credit adjustments. */
fun Route.walletAdminRoutes() {
    val loyalty: LoyaltyService by inject()
    adminWriteRateLimit {
        post("{userId}/credit") {
            val userId = call.requirePathParameter("userId")
            call.respondOk(loyalty.adminCredit(userId, call.receive<AdminCreditRequest>()))
        }.describe {
            summary = "Credit a customer's store balance"
            requestBody { jsonBody<AdminCreditRequest>() }
            responses { ok<WalletResponse>("Wallet credited") }
        }
    }
}
