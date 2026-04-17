package com.piashcse.feature.shop

import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.*
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.model.response.Shop
import com.piashcse.utils.ConflictException
import com.piashcse.utils.NotFoundException
import com.piashcse.utils.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.throwNotFound
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class ShopService : ShopRepository {
    /**
     * Creates a new shop for a seller.
     *
     * @param userId The user ID creating the shop.
     * @param shopRequest The shop details to create.
     * @return The created shop.
     */
    override suspend fun createShop(userId: String, shopRequest: ShopRequest): Shop = query {
        val user = UserDAO.find { UserTable.id eq userId }.singleOrNull()
            ?: userId.throwNotFound("User")

        // Verify that the user is a seller by checking for a corresponding seller record
        val seller = SellerDAO.find { SellerTable.userId eq userId }.singleOrNull()
            ?: throw NotFoundException(Message.Errors.SELLER_REQUIRED)

        val existingShop = ShopDAO.find {
            ShopTable.userId eq userId
        }.singleOrNull()

        if (existingShop != null) {
            // If user already has a shop, throw an error or update the existing one
            // For this implementation, we'll throw an exception
            throw ConflictException(Message.Shops.ALREADY_EXISTS)
        }

        val shop = ShopDAO.new {
            this.userId = EntityID(userId, ShopTable)
            categoryId = EntityID(shopRequest.categoryId, ShopTable)
            name = shopRequest.name
            description = shopRequest.description
            address = shopRequest.address
            phone = shopRequest.phone
            email = shopRequest.email
            logo = shopRequest.logo
            coverImage = shopRequest.coverImage
            status = ShopStatus.PENDING // Initially pending approval
        }

        // Update the seller record to link to the new shop
        seller.shopId = shop.id

        shop.shopResponse()
    }

    /**
     * Updates an existing shop.
     *
     * @param userId The user ID updating the shop.
     * @param shopId The shop ID to update.
     * @param shopRequest The shop details to update.
     * @return The updated shop.
     */
    override suspend fun updateShop(userId: String, shopId: String, shopRequest: UpdateShopRequest): Shop = query {
        val shop = ShopDAO.find { ShopTable.userId eq userId and (ShopTable.id eq shopId) }.singleOrNull()
            ?: shopId.throwNotFound("Shop")

        shop.apply {
            name = shopRequest.name ?: name
            description = shopRequest.description ?: description
            address = shopRequest.address ?: address
            phone = shopRequest.phone ?: phone
            email = shopRequest.email ?: email
            logo = shopRequest.logo ?: logo
            coverImage = shopRequest.coverImage ?: coverImage
        }

        shop.shopResponse()
    }

    /**
     * Gets a shop by ID.
     *
     * @param shopId The shop ID to retrieve.
     * @return The shop details.
     */
    override suspend fun getShopById(shopId: String): Shop? = query {
        val shop = ShopDAO.find { ShopTable.id eq shopId }.singleOrNull()
        shop?.shopResponse()
    }

    /**
     * Gets shops by user ID (seller).
     *
     * @param userId The user ID to retrieve shops for.
     * @return A list of shops for the user.
     */
    override suspend fun getShopsByUser(userId: String, limit: Int, offset: Int): PaginatedResponse<Shop> = query {
        ShopTable.selectAll().andWhere { ShopTable.userId eq userId }
            .toPaginatedResponse(limit, offset) {
                ShopDAO.wrapRow(it).shopResponse()
            }
    }

    /**
     * Gets all shops with optional filtering.
     *
     * @param status Optional status to filter by.
     * @param category Optional category to filter by.
     * @param limit Number of shops to return.
     * @return A list of shops matching the criteria.
     */
    override suspend fun getShops(status: String?, category: String?, limit: Int, offset: Int): PaginatedResponse<Shop> = query {
        // Validate status parameter
        val statusEnum = if (status != null) {
            try {
                ShopStatus.valueOf(status.uppercase())
            } catch (e: IllegalArgumentException) {
                throw NotFoundException(Message.Shops.invalidStatus(status))
            }
        } else {
            null
        }

        val query = ShopTable.selectAll()

        // Exclude rejected and suspended shops
        query.andWhere { ShopTable.status neq ShopStatus.REJECTED }
        query.andWhere { ShopTable.status neq ShopStatus.SUSPENDED }

        if (statusEnum != null) {
            query.andWhere { ShopTable.status eq statusEnum }
        }
        if (category != null) {
            query.andWhere { ShopTable.categoryId eq EntityID(category, ShopCategoryTable) }
        }

        query.orderBy(ShopTable.createdAt to SortOrder.DESC).toPaginatedResponse(limit, offset) {
            ShopDAO.wrapRow(it).shopResponse()
        }
    }

    /**
     * Gets shops by category.
     *
     * @param categoryId The category ID to filter by.
     * @return A list of shops in the category.
     */
    override suspend fun getShopsByCategory(categoryId: String, limit: Int, offset: Int): PaginatedResponse<Shop> = query {
        ShopTable.selectAll().andWhere { 
            ShopTable.categoryId eq categoryId and (ShopTable.status neq ShopStatus.REJECTED) and (ShopTable.status neq ShopStatus.SUSPENDED)
        }.toPaginatedResponse(limit, offset) {
            ShopDAO.wrapRow(it).shopResponse()
        }
    }

    /**
     * Gets featured shops.
     *
     * @return A list of featured shops.
     */
    override suspend fun getFeaturedShops(limit: Int, offset: Int): PaginatedResponse<Shop> = query {
        // For now, just return shops with highest ratings, in the future this could be more complex
        ShopTable.selectAll().andWhere { ShopTable.status eq ShopStatus.APPROVED }
            .orderBy(ShopTable.rating to SortOrder.DESC)
            .toPaginatedResponse(limit, offset) {
                ShopDAO.wrapRow(it).shopResponse()
            }
    }

    /**
     * Gets shops by status.
     *
     * @param status The status to filter by.
     * @return A list of shops with the specified status.
     */
    override suspend fun getShopsByStatus(status: ShopStatus, limit: Int, offset: Int): PaginatedResponse<Shop> = query {
        ShopTable.selectAll().andWhere { ShopTable.status eq status }
            .toPaginatedResponse(limit, offset) {
                ShopDAO.wrapRow(it).shopResponse()
            }
    }

    /**
     * Approves a shop application.
     *
     * @param shopId The shop ID to approve.
     * @return The updated shop.
     */
    override suspend fun approveShop(shopId: String): Shop = query {
        val shop = ShopDAO.find { ShopTable.id eq shopId }.singleOrNull()
            ?: shopId.throwNotFound("Shop")

        shop.apply {
            status = ShopStatus.APPROVED
            // Note: createdAt should remain unchanged, only updatedAt will be changed automatically by the base class
        }

        shop.shopResponse()
    }

    /**
     * Rejects a shop application.
     *
     * @param shopId The shop ID to reject.
     * @return The updated shop.
     */
    override suspend fun rejectShop(shopId: String): Shop = query {
        val shop = ShopDAO.find { ShopTable.id eq shopId }.singleOrNull()
            ?: shopId.throwNotFound("Shop")

        shop.apply {
            status = ShopStatus.REJECTED
        }

        shop.shopResponse()
    }

    /**
     * Suspends a shop.
     *
     * @param shopId The shop ID to suspend.
     * @return The updated shop.
     */
    override suspend fun suspendShop(shopId: String): Shop = query {
        val shop = ShopDAO.find { ShopTable.id eq shopId }.singleOrNull()
            ?: shopId.throwNotFound("Shop")

        shop.apply {
            status = ShopStatus.SUSPENDED
        }

        shop.shopResponse()
    }

    /**
     * Activates a suspended shop.
     *
     * @param shopId The shop ID to activate.
     * @return The updated shop.
     */
    override suspend fun activateShop(shopId: String): Shop = query {
        val shop = ShopDAO.find { ShopTable.id eq shopId }.singleOrNull()
            ?: shopId.throwNotFound("Shop")

        shop.apply {
            if (shop.status == ShopStatus.SUSPENDED) {
                status = ShopStatus.APPROVED
            }
        }

        shop.shopResponse()
    }
}