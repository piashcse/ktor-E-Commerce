package com.piashcse.feature.shop

import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.model.response.Shop
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import java.time.LocalDateTime

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
            ?: throw userId.notFoundException()

        // Verify that the user is a seller by checking for a corresponding seller record
        val seller = SellerDAO.find { SellerTable.userId eq userId }.singleOrNull()
            ?: throw "User is not registered as a seller".notFoundException()

        val existingShop = ShopDAO.find {
            ShopTable.userId eq userId
        }.singleOrNull()

        if (existingShop != null) {
            // If user already has a shop, throw an error or update the existing one
            // For this implementation, we'll throw an exception
            throw "User already has a shop".notFoundException()
        }

        val shop = ShopDAO.new {
            this.userId = org.jetbrains.exposed.v1.core.dao.id.EntityID(userId, ShopTable)
            categoryId = org.jetbrains.exposed.v1.core.dao.id.EntityID(shopRequest.categoryId, ShopTable)
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
            ?: throw shopId.notFoundException()

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
    override suspend fun getShopsByUser(userId: String): List<Shop> = query {
        ShopDAO.find { ShopTable.userId eq userId }.map { it.shopResponse() }
    }

    /**
     * Gets all shops with optional filtering.
     *
     * @param status Optional status to filter by.
     * @param category Optional category to filter by.
     * @param limit Number of shops to return.
     * @return A list of shops matching the criteria.
     */
    override suspend fun getShops(status: String?, category: String?, limit: Int): List<Shop> = query {
        val query = ShopDAO.all()

        val filteredQuery = if (status != null) {
            query.filter { it.status.name == status }
        } else {
            query
        }.filter { 
            if (category != null) it.categoryId.value == category else true
        }.filter { 
            it.status != ShopStatus.REJECTED && it.status != ShopStatus.SUSPENDED
        }

        filteredQuery.take(limit).map { it.shopResponse() }
    }

    /**
     * Gets shops by category.
     *
     * @param categoryId The category ID to filter by.
     * @return A list of shops in the category.
     */
    override suspend fun getShopsByCategory(categoryId: String): List<Shop> = query {
        ShopDAO.find { ShopTable.categoryId eq categoryId }.filter { 
            it.status != ShopStatus.REJECTED && it.status != ShopStatus.SUSPENDED
        }.map { it.shopResponse() }
    }

    /**
     * Gets featured shops.
     *
     * @return A list of featured shops.
     */
    override suspend fun getFeaturedShops(): List<Shop> = query {
        // For now, just return shops with highest ratings, in the future this could be more complex
        ShopDAO.find { 
            ShopTable.status eq ShopStatus.APPROVED 
        }.sortedByDescending { it.rating }.take(10).map { it.shopResponse() }
    }

    /**
     * Gets shops by status.
     *
     * @param status The status to filter by.
     * @return A list of shops with the specified status.
     */
    override suspend fun getShopsByStatus(status: ShopStatus): List<Shop> = query {
        ShopDAO.find { ShopTable.status eq status }.map { it.shopResponse() }
    }

    /**
     * Approves a shop application.
     *
     * @param shopId The shop ID to approve.
     * @return The updated shop.
     */
    override suspend fun approveShop(shopId: String): Shop = query {
        val shop = ShopDAO.find { ShopTable.id eq shopId }.singleOrNull()
            ?: throw shopId.notFoundException()

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
            ?: throw shopId.notFoundException()

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
            ?: throw shopId.notFoundException()

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
            ?: throw shopId.notFoundException()

        shop.apply {
            if (shop.status == ShopStatus.SUSPENDED) {
                status = ShopStatus.APPROVED
            }
        }

        shop.shopResponse()
    }
}