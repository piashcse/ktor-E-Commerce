package com.piashcse.feature.shop

import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.ShopCategoryTable
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopTable
import com.piashcse.event.EventBus
import com.piashcse.mapper.toShopResponse
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.model.response.ShopResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ConflictException
import com.piashcse.utils.validator.NotFoundException
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class ShopRepositoryImpl : ShopRepository {
    override suspend fun createShop(
        userId: String,
        shopRequest: ShopRequest,
    ): ShopResponse =
        query {
            val seller =
                findSellerByUserId(userId)
                    ?: throw NotFoundException(Message.Errors.SELLER_REQUIRED)

            val existingShop =
                ShopDAO.find {
                    ShopTable.userId eq userId
                }.firstOrNull()

            if (existingShop != null) {
                throw ConflictException(Message.Shops.ALREADY_EXISTS)
            }

            val shop =
                ShopDAO.new {
                    this.userId = userId.entityID(ShopTable)
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

            shop.toShopResponse()
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

            shop.toShopResponse()
        }

    override suspend fun getShopById(shopId: String): ShopResponse? =
        query {
            val shop = ShopDAO.findById(shopId)
            shop?.toShopResponse()
        }

    override suspend fun getShopsByUser(
        userId: String,
        limit: Int,
        offset: Int,
    ) = shopPaginatedQuery(limit, offset) { andWhere { ShopTable.userId eq userId } }

    override suspend fun getShops(
        status: String?,
        category: String?,
        limit: Int,
        offset: Int,
    ) = shopPaginatedQuery(limit, offset, ShopTable.createdAt to SortOrder.DESC) {
        andWhere { ShopTable.status neq ShopStatus.REJECTED }
        andWhere { ShopTable.status neq ShopStatus.SUSPENDED }
        status?.let {
            val statusEnum =
                try {
                    ShopStatus.valueOf(it.uppercase())
                } catch (e: IllegalArgumentException) {
                    throw NotFoundException(Message.Shops.invalidStatus(it))
                }
            andWhere { ShopTable.status eq statusEnum }
        }
        category?.let { andWhere { ShopTable.categoryId eq it.entityID(ShopCategoryTable) } }
    }

    override suspend fun getShopsByCategory(
        categoryId: String,
        limit: Int,
        offset: Int,
    ) = shopPaginatedQuery(limit, offset) {
        andWhere { ShopTable.categoryId eq categoryId }
        andWhere { ShopTable.status neq ShopStatus.REJECTED }
        andWhere { ShopTable.status neq ShopStatus.SUSPENDED }
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
    ) = shopPaginatedQuery(limit, offset) { andWhere { ShopTable.status eq status } }

    private suspend fun shopPaginatedQuery(
        limit: Int,
        offset: Int,
        orderBy: Pair<Column<*>, SortOrder>? = null,
        filter: Query.() -> Unit,
    ): PaginatedResponse<ShopResponse> =
        query {
            val q = ShopTable.selectAll()
            q.filter()
            q.also { orderBy?.let { (col, dir) -> q.orderBy(col to dir) } }
                .toPaginatedResponse(limit, offset) { ShopDAO.wrapRow(it).toShopResponse() }
        }

    private suspend fun setShopStatus(
        shopId: String,
        action: String,
        actorId: String?,
        actorEmail: String?,
        actorRole: String?,
        update: ShopDAO.() -> Unit,
    ): ShopResponse {
        val response =
            query {
                val shop = ShopDAO.findById(shopId) ?: shopId.throwNotFound("ShopResponse")
                shop.update()
                shop.toShopResponse()
            }
        if (actorId != null) {
            EventBus.publishAdminAction(Triple(actorId, actorEmail.orEmpty(), actorRole ?: "ADMIN"), action, "SHOP", shopId)
        }
        return response
    }

    override suspend fun approveShop(
        shopId: String,
        actorId: String?,
        actorEmail: String?,
        actorRole: String?,
    ) = setShopStatus(shopId, "SHOP_APPROVE", actorId, actorEmail, actorRole) { status = ShopStatus.APPROVED }

    override suspend fun rejectShop(
        shopId: String,
        actorId: String?,
        actorEmail: String?,
        actorRole: String?,
    ) = setShopStatus(shopId, "SHOP_REJECT", actorId, actorEmail, actorRole) { status = ShopStatus.REJECTED }

    override suspend fun suspendShop(
        shopId: String,
        actorId: String?,
        actorEmail: String?,
        actorRole: String?,
    ) = setShopStatus(shopId, "SHOP_SUSPEND", actorId, actorEmail, actorRole) { status = ShopStatus.SUSPENDED }

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
    ) { if (status == ShopStatus.SUSPENDED) status = ShopStatus.APPROVED }
}
