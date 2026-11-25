package com.piashcse.feature.recommendation

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.ProductStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.CartItemDAO
import com.piashcse.database.entities.CartItemTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.OrderItemTable
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.ProductCategoryTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductImageDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.WishlistDAO
import com.piashcse.database.entities.WishlistTable
import com.piashcse.database.entities.stockByProductIds
import com.piashcse.mapper.toProductResponse
import com.piashcse.model.response.ProductResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq

data class RecommendationProduct(
    val id: String,
    val categoryId: String,
    val subCategoryId: String?,
    val totalSales: Int,
    val rating: Double,
    val bestSeller: Boolean,
    val shopId: String?,
    val status: ProductStatus,
    val deletedAt: java.time.LocalDateTime?,
)

interface RecommendationRepository {
    suspend fun findActive(productId: String): RecommendationProduct?

    suspend fun findByCategory(
        categoryId: String,
        excludeProductId: String,
    ): List<RecommendationProduct>

    suspend fun findByIds(ids: Collection<String>): List<RecommendationProduct>

    suspend fun findByCategories(categoryIds: Collection<String>): List<RecommendationProduct>

    suspend fun activeProducts(): List<RecommendationProduct>

    suspend fun approvedShopIds(shopIds: Collection<String>): Set<String>

    suspend fun orderIdsForProduct(productId: String): List<String>

    suspend fun paidOrderIds(orderIds: Collection<String>): List<String>

    suspend fun coPurchaseCounts(
        orderIds: Collection<String>,
        excludeProductId: String,
    ): Map<String, Int>

    suspend fun purchasedProductIds(userId: String): Set<String>

    suspend fun cartProductIds(userId: String): Set<String>

    suspend fun wishlistProductIds(userId: String): Set<String>

    suspend fun loadResponses(ids: List<String>): List<ProductResponse>
}

class RecommendationRepositoryImpl : RecommendationRepository {
    override suspend fun findActive(productId: String): RecommendationProduct? =
        query {
            ProductDAO.findById(productId)
                ?.takeIf { it.deletedAt == null && it.status == ProductStatus.ACTIVE }
                ?.toRecommendationProduct()
        }

    override suspend fun findByCategory(
        categoryId: String,
        excludeProductId: String,
    ): List<RecommendationProduct> =
        query {
            ProductDAO.find {
                (ProductTable.categoryId eq categoryId.entityID(ProductCategoryTable)) and
                    (ProductTable.id neq excludeProductId.entityID(ProductTable))
            }.map { it.toRecommendationProduct() }
        }

    override suspend fun findByIds(ids: Collection<String>): List<RecommendationProduct> =
        query {
            if (ids.isEmpty()) {
                emptyList()
            } else {
                ProductDAO.find { ProductTable.id inList ids.map { it.entityID(ProductTable) } }
                    .map { it.toRecommendationProduct() }
            }
        }

    override suspend fun findByCategories(categoryIds: Collection<String>): List<RecommendationProduct> =
        query {
            if (categoryIds.isEmpty()) {
                emptyList()
            } else {
                ProductDAO.find { ProductTable.categoryId inList categoryIds.map { it.entityID(ProductCategoryTable) } }
                    .map { it.toRecommendationProduct() }
            }
        }

    override suspend fun activeProducts(): List<RecommendationProduct> =
        query {
            ProductDAO.find { ProductTable.status eq ProductStatus.ACTIVE }
                .map { it.toRecommendationProduct() }
        }

    override suspend fun approvedShopIds(shopIds: Collection<String>): Set<String> =
        query {
            if (shopIds.isEmpty()) {
                emptySet()
            } else {
                ShopDAO.find {
                    (ShopTable.id inList shopIds.map { it.entityID(ShopTable) }) and
                        (ShopTable.status eq ShopStatus.APPROVED)
                }.map { it.id.value }.toSet()
            }
        }

    override suspend fun orderIdsForProduct(productId: String): List<String> =
        query {
            OrderItemDAO.find { OrderItemTable.productId eq productId.entityID(ProductTable) }
                .map { it.orderId.value }
                .distinct()
        }

    override suspend fun paidOrderIds(orderIds: Collection<String>): List<String> =
        query {
            if (orderIds.isEmpty()) {
                emptyList()
            } else {
                OrderDAO.find {
                    (OrderTable.id inList orderIds.map { it.entityID(OrderTable) }) and
                        (OrderTable.paymentStatus eq PaymentStatus.COMPLETED) and
                        (OrderTable.status neq OrderStatus.CANCELED)
                }.map { it.id.value }
            }
        }

    override suspend fun coPurchaseCounts(
        orderIds: Collection<String>,
        excludeProductId: String,
    ): Map<String, Int> =
        query {
            if (orderIds.isEmpty()) {
                emptyMap()
            } else {
                OrderItemDAO.find {
                    (OrderItemTable.orderId inList orderIds.map { it.entityID(OrderTable) }) and
                        (OrderItemTable.productId neq excludeProductId.entityID(ProductTable))
                }.groupingBy { it.productId.value }.eachCount()
            }
        }

    override suspend fun purchasedProductIds(userId: String): Set<String> =
        query {
            val orderIds = OrderDAO.find { OrderTable.userId eq userId.entityID(UserTable) }.map { it.id }
            if (orderIds.isEmpty()) {
                emptySet()
            } else {
                OrderItemDAO.find { OrderItemTable.orderId inList orderIds }
                    .map { it.productId.value }
                    .toSet()
            }
        }

    override suspend fun cartProductIds(userId: String): Set<String> =
        query {
            CartItemDAO.find { CartItemTable.userId eq userId.entityID(UserTable) }
                .map { it.productId.value }
                .toSet()
        }

    override suspend fun wishlistProductIds(userId: String): Set<String> =
        query {
            WishlistDAO.find { WishlistTable.userId eq userId.entityID(UserTable) }
                .map { it.productId.value }
                .toSet()
        }

    override suspend fun loadResponses(ids: List<String>): List<ProductResponse> =
        query {
            if (ids.isEmpty()) return@query emptyList()
            val products =
                ProductDAO.find { ProductTable.id inList ids.map { it.entityID(ProductTable) } }
                    .associateBy { it.id.value }
            val ordered = ids.mapNotNull { products[it] }
            val images = ProductImageDAO.imagesForProducts(ordered.map { it.id })
            val stock = stockByProductIds(ordered.map { it.id.value })
            ordered.map { it.toProductResponse(images[it.id.value], stockQuantity = stock[it.id.value] ?: 0) }
        }

    private fun ProductDAO.toRecommendationProduct() =
        RecommendationProduct(
            id = id.value,
            categoryId = categoryId.value,
            subCategoryId = subCategoryId?.value,
            totalSales = totalSales,
            rating = rating.toDouble(),
            bestSeller = bestSeller,
            shopId = shopId?.value,
            status = status,
            deletedAt = deletedAt,
        )
}
