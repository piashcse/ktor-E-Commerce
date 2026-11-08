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
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductImageDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.WishListDAO
import com.piashcse.database.entities.WishListTable
import com.piashcse.mapper.toProductResponse
import com.piashcse.model.response.ProductResponse
import com.piashcse.service.Cache
import com.piashcse.service.CacheService
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq

@Serializable
data class RecommendationResponse(
    val strategy: String,
    val sourceProductId: String? = null,
    val items: List<ProductResponse>,
)

data class RecommendationCandidate(
    val productId: String,
    val score: Int,
    val totalSales: Int,
    val rating: Double,
)

/** Deterministic ranking so results are easy to test and explain. */
object RecommendationRanking {
    fun rank(
        candidates: List<RecommendationCandidate>,
        limit: Int,
    ): List<String> =
        candidates
            .distinctBy { it.productId }
            .sortedWith(
                compareByDescending<RecommendationCandidate> { it.score }
                    .thenByDescending { it.totalSales }
                    .thenByDescending { it.rating }
                    .thenBy { it.productId },
            ).take(limit.coerceIn(1, MAX_LIMIT))
            .map { it.productId }

    const val MAX_LIMIT = 50
}

class RecommendationService(
    private val cache: Cache = CacheService.cache,
) {
    suspend fun related(
        productId: String,
        limit: Int,
    ): RecommendationResponse =
        cached("recommendations:related:$productId:$limit") {
            query {
                val target = requireProduct(productId)
                val candidates =
                    validProducts(
                        ProductDAO.find {
                            (ProductTable.categoryId eq target.categoryId) and (ProductTable.id neq target.id)
                        }.toList(),
                    )
                val ranked = rankCandidates(target, candidates, limit)
                val products = loadOrdered(ranked)
                if (products.isEmpty()) fallbackResponse(productId, "related", limit) else response("related", productId, products)
            }
        }

    suspend fun alsoBought(
        productId: String,
        limit: Int,
    ): RecommendationResponse =
        cached("recommendations:also-bought:$productId:$limit") {
            val local =
                query {
                    val target = requireProduct(productId)
                    val orderIds =
                        OrderItemDAO.find { OrderItemTable.productId eq target.id }
                            .map { it.orderId }
                            .distinct()
                    val paidOrderIds =
                        if (orderIds.isEmpty()) {
                            emptyList()
                        } else {
                            OrderDAO.find {
                                (OrderTable.id inList orderIds) and
                                    (OrderTable.paymentStatus eq PaymentStatus.COMPLETED) and
                                    (OrderTable.status neq OrderStatus.CANCELED)
                            }.map { it.id }
                        }
                    val counts =
                        if (paidOrderIds.isEmpty()) {
                            emptyMap()
                        } else {
                            OrderItemDAO.find {
                                (OrderItemTable.orderId inList paidOrderIds) and (OrderItemTable.productId neq target.id)
                            }.groupingBy { it.productId.value }.eachCount()
                        }
                    val candidates =
                        if (counts.isEmpty()) {
                            emptyList()
                        } else {
                            validProducts(
                                ProductDAO.find {
                                    ProductTable.id inList counts.keys.map { it.entityID(ProductTable) }
                                }.toList(),
                            )
                        }
                    val ranked =
                        RecommendationRanking.rank(
                            candidates.map {
                                RecommendationCandidate(
                                    productId = it.id.value,
                                    score = counts[it.id.value] ?: 0,
                                    totalSales = it.totalSales,
                                    rating = it.rating.toDouble(),
                                )
                            },
                            limit,
                        )
                    val products = loadOrdered(ranked)
                    if (products.isEmpty()) null else response("also-bought", productId, products)
                }
            local ?: related(productId, limit).copy(strategy = "also-bought")
        }

    suspend fun personalized(
        userId: String,
        limit: Int,
    ): RecommendationResponse =
        cached("recommendations:user:$userId:$limit") {
            query {
                val orderIds = OrderDAO.find { OrderTable.userId eq userId.entityID(UserTable) }.map { it.id }
                val purchased =
                    if (orderIds.isEmpty()) {
                        emptySet()
                    } else {
                        OrderItemDAO.find { OrderItemTable.orderId inList orderIds }
                            .map { it.productId.value }
                            .toSet()
                    }
                val cartIds = CartItemDAO.find { CartItemTable.userId eq userId }.map { it.productId.value }.toSet()
                val wishlistIds = WishListDAO.find { WishListTable.userId eq userId }.map { it.productId.value }.toSet()
                val signalIds = (purchased + cartIds + wishlistIds).filter { it.isNotBlank() }.distinct()
                val weights =
                    if (signalIds.isEmpty()) {
                        emptyMap()
                    } else {
                        ProductDAO.find { ProductTable.id inList signalIds.map { it.entityID(ProductTable) } }
                            .groupingBy { it.categoryId.value }
                            .eachCount()
                    }
                val excluded = purchased + cartIds + wishlistIds
                val candidates =
                    if (weights.isEmpty()) {
                        emptyList()
                    } else {
                        validProducts(
                            ProductDAO.find {
                                ProductTable.categoryId inList weights.keys.map { it.entityID(ProductTable) }
                            }.toList(),
                        ).filterNot { it.id.value in excluded }
                    }
                val ranked =
                    RecommendationRanking.rank(
                        candidates.map {
                            RecommendationCandidate(
                                productId = it.id.value,
                                score = weights[it.categoryId.value] ?: 0,
                                totalSales = it.totalSales,
                                rating = it.rating.toDouble(),
                            )
                        },
                        limit,
                    )
                val products = loadOrdered(ranked).toMutableList()
                if (products.size < limit) {
                    val seen = products.map { it.id }.toSet()
                    products += bestSellerProducts(limit).filterNot { it.id in seen }.take(limit - products.size)
                }
                if (products.isEmpty()) {
                    response("best-sellers", null, bestSellerProducts(limit))
                } else {
                    response("personalized", null, products)
                }
            }
        }

    suspend fun bestSellers(limit: Int): RecommendationResponse =
        cached("recommendations:best-sellers:$limit") {
            query { response("best-sellers", null, bestSellerProducts(limit)) }
        }

    private fun bestSellerProducts(limit: Int): List<ProductResponse> {
        val products =
            validProducts(
                ProductDAO.find { ProductTable.status eq ProductStatus.ACTIVE }.toList(),
            )
        val ranked =
            RecommendationRanking.rank(
                products.map {
                    RecommendationCandidate(
                        productId = it.id.value,
                        score = if (it.bestSeller) 1 else 0,
                        totalSales = it.totalSales,
                        rating = it.rating.toDouble(),
                    )
                },
                limit,
            )
        return loadOrdered(ranked)
    }

    private fun requireProduct(productId: String): ProductDAO =
        ProductDAO.findById(productId)?.takeIf { it.deletedAt == null && it.status == ProductStatus.ACTIVE }
            ?: productId.throwNotFound("Product")

    private fun rankCandidates(
        target: ProductDAO,
        candidates: List<ProductDAO>,
        limit: Int,
    ): List<String> =
        RecommendationRanking.rank(
            candidates.map {
                RecommendationCandidate(
                    productId = it.id.value,
                    score = if (it.subCategoryId?.value == target.subCategoryId?.value) 2 else 1,
                    totalSales = it.totalSales,
                    rating = it.rating.toDouble(),
                )
            },
            limit,
        )

    private fun fallbackResponse(
        productId: String,
        strategy: String,
        limit: Int,
    ): RecommendationResponse = response(strategy, productId, bestSellerProducts(limit))

    private fun loadOrdered(ids: List<String>): List<ProductResponse> {
        if (ids.isEmpty()) return emptyList()
        val products =
            ProductDAO.find { ProductTable.id inList ids.map { it.entityID(ProductTable) } }
                .associateBy { it.id.value }
        val ordered = ids.mapNotNull { products[it] }
        val images = ProductImageDAO.imagesForProducts(ordered.map { it.id })
        return ordered.map { it.toProductResponse(images[it.id.value]) }
    }

    private fun validProducts(products: List<ProductDAO>): List<ProductDAO> {
        val active =
            products.filter {
                it.status == ProductStatus.ACTIVE && it.deletedAt == null
            }
        val shopIds = active.mapNotNull { it.shopId?.value }.distinct()
        if (shopIds.isEmpty()) return active
        val approved =
            ShopDAO.find {
                (ShopTable.id inList shopIds.map { it.entityID(ShopTable) }) and
                    (ShopTable.status eq ShopStatus.APPROVED)
            }.map { it.id.value }.toSet()
        return active.filter { product -> product.shopId == null || product.shopId?.value in approved }
    }

    private fun response(
        strategy: String,
        productId: String?,
        products: List<ProductResponse>,
    ) = RecommendationResponse(strategy, productId, products)

    private suspend fun cached(
        key: String,
        block: suspend () -> RecommendationResponse,
    ): RecommendationResponse {
        cache.get<RecommendationResponse>(key)?.let { return it }
        return block().also { cache.set(key, it) }
    }
}
