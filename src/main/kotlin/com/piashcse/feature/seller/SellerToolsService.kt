package com.piashcse.feature.seller

import com.piashcse.constants.DisputeStatus
import com.piashcse.constants.ReviewStatus
import com.piashcse.constants.UserType
import com.piashcse.utils.common.PaginatedResponse

class SellerToolsService {
    private val reviews = SellerReviewService()
    private val disputes = SellerDisputeService()

    suspend fun createReview(
        userId: String,
        request: SellerReviewRequest,
    ): SellerReviewResponse = reviews.createReview(userId, request)

    suspend fun reviewsForSeller(
        sellerId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SellerReviewResponse> = reviews.reviewsForSeller(sellerId, limit, offset)

    suspend fun reviewsForSellerUser(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SellerReviewResponse> = reviews.reviewsForSellerUser(sellerUserId, limit, offset)

    suspend fun reviewsForShop(
        shopId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SellerReviewResponse> = reviews.reviewsForShop(shopId, limit, offset)

    suspend fun reviewsForUser(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SellerReviewResponse> = reviews.reviewsForUser(userId, limit, offset)

    suspend fun adminReviews(
        status: ReviewStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SellerReviewResponse> = reviews.adminReviews(status, limit, offset)

    suspend fun updateReviewStatus(
        reviewId: String,
        status: ReviewStatus,
        reason: String? = null,
    ): SellerReviewResponse = reviews.updateReviewStatus(reviewId, status, reason)

    suspend fun replyToSellerReview(
        sellerUserId: String,
        reviewId: String,
        reply: String,
    ): SellerReviewResponse = reviews.replyToSellerReview(sellerUserId, reviewId, reply)

    suspend fun createDispute(
        userId: String,
        request: DisputeRequest,
    ): DisputeResponse = disputes.createDispute(userId, request)

    suspend fun myDisputes(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<DisputeResponse> = disputes.myDisputes(userId, limit, offset)

    suspend fun adminDisputes(
        status: DisputeStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<DisputeResponse> = disputes.adminDisputes(status, limit, offset)

    suspend fun updateDispute(
        disputeId: String,
        actorUserId: String,
        request: DisputeUpdateRequest,
    ): DisputeResponse = disputes.updateDispute(disputeId, actorUserId, request)

    suspend fun disputeDetail(
        userId: String,
        userType: UserType,
        disputeId: String,
    ): DisputeDetailResponse = disputes.disputeDetail(userId, userType, disputeId)

    suspend fun addDisputeMessage(
        userId: String,
        userType: UserType,
        disputeId: String,
        request: DisputeMessageRequest,
    ): DisputeDetailResponse = disputes.addDisputeMessage(userId, userType, disputeId, request)
}
