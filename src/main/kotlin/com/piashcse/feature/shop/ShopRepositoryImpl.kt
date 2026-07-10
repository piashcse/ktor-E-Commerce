package com.piashcse.feature.shop

import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.ShopCategoryTable
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopTable
import com.piashcse.mapper.toShopResponse
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.model.response.ShopResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ConflictException
import com.piashcse.utils.validator.NotFoundException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class ShopRepositoryImpl : ShopRepository {
    override suspend fun createShop(
        userId: String,
        shopRequest: ShopRequest,
    ): ShopResponse =
        query {
            val seller = findSellerByUserId(userId)
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
                    this.userId = EntityID(userId, ShopTable)
                    categoryId = EntityID(shopRequest.categoryId, ShopCategoryTable)
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
    ): PaginatedResponse<ShopResponse> =
        query {
            ShopTable.selectAll().andWhere { ShopTable.userId eq userId }
                .toPaginatedResponse(limit, offset) {
                    ShopDAO.wrapRow(it).toShopResponse()
                }
        }

    override suspend fun getShops(
        status: String?,
        category: String?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ShopResponse> =
        query {
            val statusEnum =
                if (status != null) {
                    try {
                        ShopStatus.valueOf(status.uppercase())
                    } catch (e: IllegalArgumentException) {
                        throw NotFoundException(Message.Shops.invalidStatus(status))
                    }
                } else {
                    null
                }

            val q = ShopTable.selectAll()

            q.andWhere { ShopTable.status neq ShopStatus.REJECTED }
            q.andWhere { ShopTable.status neq ShopStatus.SUSPENDED }

            if (statusEnum != null) {
                q.andWhere { ShopTable.status eq statusEnum }
            }
            if (category != null) {
                q.andWhere { ShopTable.categoryId eq EntityID(category, ShopCategoryTable) }
            }

            q.orderBy(ShopTable.createdAt to SortOrder.DESC).toPaginatedResponse(limit, offset) {
                ShopDAO.wrapRow(it).toShopResponse()
            }
        }

    override suspend fun getShopsByCategory(
        categoryId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ShopResponse> =
        query {
            ShopTable.selectAll().andWhere {
                ShopTable.categoryId eq categoryId and (ShopTable.status neq ShopStatus.REJECTED) and (ShopTable.status neq ShopStatus.SUSPENDED)
            }.toPaginatedResponse(limit, offset) {
                ShopDAO.wrapRow(it).toShopResponse()
            }
        }

    override suspend fun getFeaturedShops(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ShopResponse> =
        query {
            ShopTable.selectAll().andWhere { ShopTable.status eq ShopStatus.APPROVED }
                .orderBy(ShopTable.rating to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    ShopDAO.wrapRow(it).toShopResponse()
                }
        }

    override suspend fun getShopsByStatus(
        status: ShopStatus,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ShopResponse> =
        query {
            ShopTable.selectAll().andWhere { ShopTable.status eq status }
                .toPaginatedResponse(limit, offset) {
                    ShopDAO.wrapRow(it).toShopResponse()
                }
        }

    private suspend fun setShopStatus(shopId: String, update: ShopDAO.() -> Unit): ShopResponse = query {
        val shop = ShopDAO.findById(shopId) ?: shopId.throwNotFound("ShopResponse")
        shop.update()
        shop.toShopResponse()
    }

    override suspend fun approveShop(shopId: String) = setShopStatus(shopId) { status = ShopStatus.APPROVED }

    override suspend fun rejectShop(shopId: String) = setShopStatus(shopId) { status = ShopStatus.REJECTED }

    override suspend fun suspendShop(shopId: String) = setShopStatus(shopId) { status = ShopStatus.SUSPENDED }

    override suspend fun activateShop(shopId: String) = setShopStatus(shopId) { if (status == ShopStatus.SUSPENDED) status = ShopStatus.APPROVED }
}
