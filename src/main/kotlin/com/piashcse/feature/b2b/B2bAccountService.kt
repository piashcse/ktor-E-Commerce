package com.piashcse.feature.b2b

import com.piashcse.constants.B2bAccountStatus
import com.piashcse.database.entities.B2bAccountDAO
import com.piashcse.database.entities.B2bAccountTable
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.AdminAudit
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal

/** B2B account application, approval and directory queries. */
class B2bAccountService {
    suspend fun account(userId: String): B2bAccountResponse =
        query {
            val account =
                B2bAccountDAO.find { B2bAccountTable.userId eq userId.entityID(UserTable) }
                    .firstOrNull() ?: throw ValidationException("B2B account not found")
            account.toAccountResponse()
        }

    suspend fun apply(
        userId: String,
        request: B2bAccountRequest,
    ): B2bAccountResponse =
        query {
            val existing =
                B2bAccountDAO.find { B2bAccountTable.userId eq userId.entityID(UserTable) }
                    .firstOrNull()
            val account =
                existing?.apply {
                    companyName = request.companyName
                    taxId = request.taxId
                    contactName = request.contactName
                    phone = request.phone
                } ?: B2bAccountDAO.new {
                    this.userId = userId.entityID(UserTable)
                    companyName = request.companyName
                    taxId = request.taxId
                    contactName = request.contactName
                    phone = request.phone
                    creditLimit = BigDecimal.ZERO
                    paymentTermsDays = 30
                    status = B2bAccountStatus.PENDING
                }
            account.toAccountResponse()
        }

    suspend fun adminAccounts(
        status: B2bAccountStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<B2bAccountResponse> =
        query {
            B2bAccountTable.selectAll()
                .also { query ->
                    status?.let { query.andWhere { B2bAccountTable.status eq it } }
                    query.orderBy(B2bAccountTable.createdAt to SortOrder.DESC)
                }.toPaginatedResponse(limit, offset) { B2bAccountDAO.wrapRow(it).toAccountResponse() }
        }

    suspend fun updateAccount(
        userId: String,
        actorUserId: String,
        request: B2bAccountUpdateRequest,
    ): B2bAccountResponse =
        query {
            val account =
                B2bAccountDAO.find { B2bAccountTable.userId eq userId.entityID(UserTable) }
                    .firstOrNull() ?: throw ValidationException("B2B account not found")
            request.creditLimit?.let {
                if (it < BigDecimal.ZERO) throw ValidationException("Credit limit cannot be negative")
                account.creditLimit = it
            }
            request.paymentTermsDays?.let {
                if (it !in 1..365) throw ValidationException("Payment terms must be between 1 and 365 days")
                account.paymentTermsDays = it
            }
            request.status?.let {
                account.status = it
                if (it == B2bAccountStatus.APPROVED) {
                    account.approvedBy = actorUserId.entityID(UserTable)
                    account.approvedAt = currentUtc()
                }
            }
            AdminAudit.publish(
                actorUserId,
                "B2B_ACCOUNT_UPDATED",
                "B2B_ACCOUNT",
                account.id.value,
                "Status ${account.status}, credit ${account.creditLimit.toPlainString()}, terms ${account.paymentTermsDays}",
            )
            account.toAccountResponse()
        }

    private fun B2bAccountDAO.toAccountResponse() =
        B2bAccountResponse(
            id = id.value,
            userId = userId.value,
            companyName = companyName,
            taxId = taxId,
            contactName = contactName,
            phone = phone,
            creditLimit = Money.str(creditLimit),
            paymentTermsDays = paymentTermsDays,
            status = status,
            approvedAt = approvedAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
        )
}
