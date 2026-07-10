package com.piashcse.feature.refund_request

import com.piashcse.constants.UserType
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.utils.common.PaginatedResponse

class RefundRequestService(private val repo: RefundRequestRepository) : RefundRequestRepository by repo
