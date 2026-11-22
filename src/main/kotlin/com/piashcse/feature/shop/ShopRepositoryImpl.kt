package com.piashcse.feature.shop

import com.piashcse.constants.MembershipStatus
import com.piashcse.constants.Message
import com.piashcse.constants.RbacDefaults
import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.AuthRoleTable
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopCategoryTable
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopMemberDAO
import com.piashcse.database.entities.ShopMemberRoleTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.EventBus
import com.piashcse.event.ShopStatusChangedEvent
import com.piashcse.mapper.toShopResponse
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.model.response.ShopResponse
import com.piashcse.service.CatalogCache
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ConflictException
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

class ShopRepositoryImpl : ShopRepository {
    override suspend fun createShop(
        userId: String,
        shopRequest: ShopRequest,
    ): ShopResponse {
        val response =
            query {
                val seller =
                    try {
                        requireSellerByUserId(userId)
                    } catch (e: ForbiddenException) {
                        throw NotFoundException(e.message ?: Message.Errors.SELLER_REQUIRED)
                    }

                val existingShop =
                    ShopDAO.find {
                        ShopTable.userId eq userId
                    }.firstOrNull()

                if (existingShop != null) {
                    throw ConflictException(Message.Shops.ALREADY_EXISTS)
                }

                ShopCategoryTable.selectAll().andWhere { ShopCategoryTable.id eq shopRequest.categoryId.entityID(ShopCategoryTable) }
                    .firstOrNull() ?: throw NotFoundException("Shop category not found")

                val shop =
                    ShopDAO.new {
                        this.userId = userId.entityID(UserTable)
                        categoryId = shopRequest.categoryId.entityID(ShopCategoryTable)
                        name = shopRequest.name
                        description = shopRequest.description
                        address = shopRequest.address
                        phone = shopRequest.phone
                        email = shopRequest.email
                        logo = shopRequest.logo
                        coverImage = shopRequest.coverImage
                        status = ShopStatus.PENDING
                    }

                seller.shopId = shop.id
                com.piashcse.database.entities.WarehouseDAO.new {
                    shopId = shop.id
                    name = "Default"
                    code = "DEFAULT"
                }
                // Every shop has an owner membership from the moment it exists.
                val member =
                    ShopMemberDAO.new {
                        shopId = shop.id
                        this.userId = userId.entityID(UserTable)
                        status = MembershipStatus.ACTIVE
                        isOwner = true
                        joinedAt = currentUtc()
                    }
                ShopMemberRoleTable.insert {
                    it[ShopMemberRoleTable.memberId] = member.id
                    it[ShopMemberRoleTable.roleId] = RbacDefaults.SHOP_OWNER_ROLE_ID.entityID(AuthRoleTable)
                }

                shop.toShopResponse(includeInternal = true)
            }.also { response ->
                EventBus.publish(ShopStatusChangedEvent(response.id, userId, ShopStatus.PENDING.name))
            }
        return response
    }

    override suspend fun updateShop(
        userId: String,
        shopId: String,
        shopRequest: UpdateShopRequest,
    ): ShopResponse =
        query {
            val shop =
                ShopDAO.findById(shopId)
                    ?: shopId.throwNotFound("Shop")

            shop.verifyOwnership(userId, "shop") { it.userId.value }

            shop.apply {
                name = shopRequest.name ?: name
                description = shopRequest.description ?: description
                address = shopRequest.address ?: address
                phone = shopRequest.phone ?: phone
                email = shopRequest.email ?: email
                logo = shopRequest.logo ?: logo
                coverImage = shopRequest.coverImage ?: coverImage
            }

            shop.toShopResponse(includeInternal = true)
        }

    override suspend fun getShopById(shopId: String): ShopResponse? =
        query {
            val shop = ShopDAO.findById(shopId) ?: return@query null
            // Public discovery must not expose pending/rejected/suspended shop
            // contact details by ID.
            if (shop.status != ShopStatus.APPROVED) return@query null
            shop.toShopResponse()
        }

    override suspend fun getShopsByUser(
        userId: String,
        limit: Int,
        offset: Int,
    ) = shopPaginatedQuery(limit, offset, includeInternal = true) { andWhere { ShopTable.userId eq userId } }

    override suspend fun getShops(
        status: String?,
        category: String?,
        limit: Int,
        offset: Int,
    ) = shopPaginatedQuery(limit, offset, ShopTable.createdAt to SortOrder.DESC) {
        // Public discovery only exposes approved shops. Admin status listings use
        // getShopsByStatus instead.
        andWhere { ShopTable.status eq ShopStatus.APPROVED }
        status?.let {
            val statusEnum =
                try {
                    ShopStatus.valueOf(it.uppercase())
                } catch (e: IllegalArgumentException) {
                    throw ValidationException(Message.Shops.invalidStatus(it))
                }
            if (statusEnum != ShopStatus.APPROVED) {
                throw ValidationException("Public shop discovery only supports APPROVED")
            }
        }
        category?.let { andWhere { ShopTable.categoryId eq it.entityID(ShopCategoryTable) } }
    }

    override suspend fun getShopsByCategory(
        categoryId: String,
        limit: Int,
        offset: Int,
    ) = shopPaginatedQuery(limit, offset) {
        andWhere { ShopTable.categoryId eq categoryId }
        andWhere { ShopTable.status eq ShopStatus.APPROVED }
    }

    override suspend fun getFeaturedShops(
        limit: Int,
        offset: Int,
    ) = shopPaginatedQuery(limit, offset, ShopTable.rating to SortOrder.DESC) {
        andWhere { ShopTable.status eq ShopStatus.APPROVED }
    }

    override suspend fun getShopsByStatus(
        status: ShopStatus,
        limit: Int,
        offset: Int,
    ) = shopPaginatedQuery(limit, offset, includeInternal = true) { andWhere { ShopTable.status eq status } }

    override suspend fun getAllShops(
        status: ShopStatus?,
        search: String?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ShopResponse> =
        query {
            val shops = ShopTable.selectAll()
            status?.let { shops.andWhere { ShopTable.status eq it } }
            search?.takeIf { it.isNotBlank() }?.let { term ->
                val pattern = "%${term.trim().lowercase()}%"
                shops.andWhere { ShopTable.name.lowerCase() like pattern }
            }
            val (total, rows) =
                shops.orderBy(ShopTable.createdAt to SortOrder.DESC)
                    .toPaginatedList(limit, offset) { ShopDAO.wrapRow(it) }
            val sellers =
                SellerDAO.find { SellerTable.shopId inList rows.map { it.id } }
                    .associateBy { it.shopId?.value }
            val users =
                UserDAO.find {
                    UserTable.id inList sellers.values.map { it.userId }.distinct()
                }.associateBy { it.id.value }
            PaginatedResponse(
                rows.map { shop ->
                    val seller = sellers[shop.id.value]
                    shop.toShopResponse(users[seller?.userId?.value]?.email, seller?.kycStatus, includeInternal = true)
                },
                com.piashcse.utils.common.PaginationMetadata(total, limit, offset),
            )
        }

    private suspend fun shopPaginatedQuery(
        limit: Int,
        offset: Int,
        orderBy: Pair<Column<*>, SortOrder>? = null,
        includeInternal: Boolean = false,
        filter: Query.() -> Unit,
    ): PaginatedResponse<ShopResponse> =
        query {
            val q = ShopTable.selectAll()
            q.filter()
            q.also { orderBy?.let { (col, dir) -> q.orderBy(col to dir) } }
                .toPaginatedResponse(limit, offset) {
                    ShopDAO.wrapRow(it).toShopResponse(includeInternal = includeInternal)
                }
        }

    private suspend fun setShopStatus(
        shopId: String,
        action: String,
        actorId: String?,
        actorEmail: String?,
        actorRole: String?,
        requiredCurrent: Set<ShopStatus>,
        target: ShopStatus,
        reason: String? = null,
        sellerUpdate: ((SellerDAO) -> Unit)? = null,
    ): ShopResponse {
        var ownerUserId: String? = null
        val response =
            query {
                val shop = ShopDAO.findById(shopId) ?: shopId.throwNotFound("Shop")
                ownerUserId = shop.userId.value
                if (shop.status !in requiredCurrent) {
                    throw ConflictException(
                        "Cannot $action a shop in ${shop.status} status",
                    )
                }
                shop.status = target
                when (target) {
                    ShopStatus.REJECTED -> shop.rejectionReason = reason?.takeIf { it.isNotBlank() }
                    ShopStatus.SUSPENDED -> shop.suspendedReason = reason?.takeIf { it.isNotBlank() }
                    ShopStatus.APPROVED -> {
                        shop.rejectionReason = null
                        shop.suspendedReason = null
                    }
                    else -> Unit
                }
                if (sellerUpdate != null) {
                    SellerDAO.find { SellerTable.shopId eq shop.id }.firstOrNull()?.let(sellerUpdate)
                }
                shop.toShopResponse(includeInternal = true)
            }
        if (actorId != null) {
            EventBus.publishAdminAction(Triple(actorId, actorEmail.orEmpty(), actorRole ?: "ADMIN"), action, "SHOP", shopId)
        }
        ownerUserId?.let { EventBus.publish(ShopStatusChangedEvent(shopId, it, target.name, reason)) }
        CatalogCache.invalidate()
        return response
    }

    override suspend fun approveShop(
        shopId: String,
        actorId: String?,
        actorEmail: String?,
        actorRole: String?,
    ) = setShopStatus(
        shopId,
        "SHOP_APPROVE",
        actorId,
        actorEmail,
        actorRole,
        requiredCurrent = setOf(ShopStatus.PENDING, ShopStatus.REJECTED),
        target = ShopStatus.APPROVED,
    ) { seller ->
        seller.status = ShopStatus.APPROVED
        seller.approvedAt = currentUtc()
    }

    override suspend fun rejectShop(
        shopId: String,
        actorId: String?,
        actorEmail: String?,
        actorRole: String?,
        reason: String?,
    ) = setShopStatus(
        shopId,
        "SHOP_REJECT",
        actorId,
        actorEmail,
        actorRole,
        requiredCurrent = setOf(ShopStatus.PENDING),
        target = ShopStatus.REJECTED,
        reason = reason,
    ) { seller -> seller.status = ShopStatus.REJECTED }

    override suspend fun suspendShop(
        shopId: String,
        actorId: String?,
        actorEmail: String?,
        actorRole: String?,
        reason: String?,
    ) = setShopStatus(
        shopId,
        "SHOP_SUSPEND",
        actorId,
        actorEmail,
        actorRole,
        requiredCurrent = setOf(ShopStatus.APPROVED),
        target = ShopStatus.SUSPENDED,
        reason = reason,
    ) { seller ->
        seller.status = ShopStatus.SUSPENDED
        seller.suspendedAt = currentUtc()
    }

    override suspend fun activateShop(
        shopId: String,
        actorId: String?,
        actorEmail: String?,
        actorRole: String?,
    ) = setShopStatus(
        shopId,
        "SHOP_ACTIVATE",
        actorId,
        actorEmail,
        actorRole,
        requiredCurrent = setOf(ShopStatus.SUSPENDED),
        target = ShopStatus.APPROVED,
    ) { seller ->
        seller.status = ShopStatus.APPROVED
        seller.suspendedAt = null
    }
}
