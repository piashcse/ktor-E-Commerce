package com.piashcse.feature.warehouse

import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.WarehouseDAO
import com.piashcse.database.entities.WarehouseTable
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import java.math.BigDecimal

@Serializable
data class WarehouseRequest(
    val name: String,
    val code: String,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    @Contextual val latitude: BigDecimal? = null,
    @Contextual val longitude: BigDecimal? = null,
    val phone: String? = null,
    val openingHours: String? = null,
    val supportsPickup: Boolean = false,
    val supportsPos: Boolean = false,
)

@Serializable
data class WarehouseResponse(
    val id: String,
    val shopId: String,
    val name: String,
    val code: String,
    val address: String?,
    val city: String?,
    val country: String?,
    val isActive: Boolean,
    val latitude: String? = null,
    val longitude: String? = null,
    val phone: String? = null,
    val openingHours: String? = null,
    val supportsPickup: Boolean = false,
    val supportsPos: Boolean = false,
    val createdAt: String,
)

class WarehouseRepository {
    suspend fun list(sellerUserId: String): List<WarehouseResponse> =
        query {
            val shopId = requireSellerShopId(sellerUserId)
            WarehouseDAO.find { WarehouseTable.shopId eq shopId }
                .orderBy(WarehouseTable.createdAt to org.jetbrains.exposed.v1.core.SortOrder.ASC)
                .map { it.toResponse() }
        }

    suspend fun create(
        sellerUserId: String,
        request: WarehouseRequest,
    ): WarehouseResponse =
        query {
            if (request.name.isBlank() || request.code.isBlank()) {
                throw ValidationException("Warehouse name and code are required")
            }
            val shopId = requireSellerShopId(sellerUserId)
            val duplicate =
                WarehouseDAO.find {
                    (WarehouseTable.shopId eq shopId) and (WarehouseTable.code eq request.code.uppercase())
                }.firstOrNull()
            if (duplicate != null) throw ValidationException("Warehouse code already exists")
            WarehouseDAO.new {
                this.shopId = shopId.entityID(ShopTable)
                name = request.name
                code = request.code.uppercase()
                address = request.address
                city = request.city
                this.country = request.country?.uppercase()
                latitude = request.latitude
                longitude = request.longitude
                phone = request.phone
                openingHours = request.openingHours
                supportsPickup = request.supportsPickup
                supportsPos = request.supportsPos
            }.toResponse()
        }

    suspend fun update(
        sellerUserId: String,
        warehouseId: String,
        request: WarehouseRequest,
    ): WarehouseResponse =
        query {
            val shopId = requireSellerShopId(sellerUserId)
            val warehouse =
                WarehouseDAO.find {
                    (WarehouseTable.id eq warehouseId.entityID(WarehouseTable)) and
                        (WarehouseTable.shopId eq shopId)
                }.firstOrNull() ?: warehouseId.throwNotFound("Warehouse")
            warehouse.name = request.name
            warehouse.code = request.code.uppercase()
            warehouse.address = request.address
            warehouse.city = request.city
            warehouse.country = request.country?.uppercase()
            warehouse.latitude = request.latitude
            warehouse.longitude = request.longitude
            warehouse.phone = request.phone
            warehouse.openingHours = request.openingHours
            warehouse.supportsPickup = request.supportsPickup
            warehouse.supportsPos = request.supportsPos
            warehouse.toResponse()
        }

    suspend fun deactivate(
        sellerUserId: String,
        warehouseId: String,
    ): Boolean =
        query {
            val shopId = requireSellerShopId(sellerUserId)
            val warehouse =
                WarehouseDAO.find {
                    (WarehouseTable.id eq warehouseId.entityID(WarehouseTable)) and
                        (WarehouseTable.shopId eq shopId)
                }.firstOrNull() ?: warehouseId.throwNotFound("Warehouse")
            val others =
                WarehouseDAO.find {
                    (WarehouseTable.shopId eq shopId) and
                        (WarehouseTable.id neq warehouse.id) and
                        (WarehouseTable.isActive eq true)
                }.count()
            if (others == 0L) throw ValidationException("A shop must keep at least one active warehouse")
            warehouse.isActive = false
            true
        }

    suspend fun ensureDefault(shopId: String): WarehouseDAO =
        query {
            WarehouseDAO.find { WarehouseTable.shopId eq shopId.entityID(ShopTable) }
                .firstOrNull()
                ?: WarehouseDAO.new {
                    this.shopId = shopId.entityID(ShopTable)
                    name = "Default"
                    code = "DEFAULT"
                }
        }

    private fun requireSellerShopId(userId: String): String {
        val seller = requireSellerByUserId(userId)
        return seller.shopId?.value ?: throw ForbiddenException("Seller has no shop")
    }

    private fun WarehouseDAO.toResponse() =
        WarehouseResponse(
            id = id.value,
            shopId = shopId.value,
            name = name,
            code = code,
            address = address,
            city = city,
            country = country,
            isActive = isActive,
            latitude = latitude?.toPlainString(),
            longitude = longitude?.toPlainString(),
            phone = phone,
            openingHours = openingHours,
            supportsPickup = supportsPickup,
            supportsPos = supportsPos,
            createdAt = createdAt.toUtcString(),
        )
}
