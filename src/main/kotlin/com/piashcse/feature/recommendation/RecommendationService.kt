package com.piashcse.feature.recommendation

import com.piashcse.constants.ProductStatus
import com.piashcse.model.response.ProductResponse
import com.piashcse.service.Cache
import com.piashcse.service.CacheService
import com.piashcse.utils.extension.throwNotFound
import kotlinx.serialization.Serializable

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
    private val repository: RecommendationRepository,
    private val cache: Cache = CacheService.cache,
) {
    suspend fun related(
        productId: String,
        limit: Int,
    ): RecommendationResponse =
        cached("recommendations:related:$productId:$limit") {
            val target = requireProduct(productId)
            val candidates = validProducts(repository.findByCategory(target.categoryId, productId))
            val ranked = rankCandidates(target, candidates, limit)
            val products = repository.loadResponses(ranked)
            if (products.isEmpty()) fallbackResponse(productId, "related", limit) else response("related", productId, products)
        }

    suspend fun alsoBought(
        productId: String,
        limit: Int,
    ): RecommendationResponse =
        cached("recommendations:also-bought:$productId:$limit") {
            val target = requireProduct(productId)
            val orderIds = repository.orderIdsForProduct(target.id)
            val paidOrderIds = repository.paidOrderIds(orderIds)
            val counts = repository.coPurchaseCounts(paidOrderIds, target.id)
            val candidates = validProducts(repository.findByIds(counts.keys))
            val ranked =
                RecommendationRanking.rank(
                    candidates.map {
                        RecommendationCandidate(
                            productId = it.id,
                            score = counts[it.id] ?: 0,
                            totalSales = it.totalSales,
                            rating = it.rating,
                        )
                    },
                    limit,
                )
            val products = repository.loadResponses(ranked)
            if (products.isEmpty()) {
                related(
                    productId,
                    limit,
                ).copy(strategy = "also-bought")
            } else {
                response("also-bought", productId, products)
            }
        }

    suspend fun personalized(
        userId: String,
        limit: Int,
    ): RecommendationResponse =
        cached("recommendations:user:$userId:$limit") {
            val purchased = repository.purchasedProductIds(userId)
            val cartIds = repository.cartProductIds(userId)
            val wishlistIds = repository.wishlistProductIds(userId)
            val signalIds = (purchased + cartIds + wishlistIds).filter { it.isNotBlank() }.distinct()
            val weights = repository.findByIds(signalIds).groupingBy { it.categoryId }.eachCount()
            val excluded = purchased + cartIds + wishlistIds
            val candidates =
                validProducts(repository.findByCategories(weights.keys))
                    .filterNot { it.id in excluded }
            val ranked =
                RecommendationRanking.rank(
                    candidates.map {
                        RecommendationCandidate(
                            productId = it.id,
                            score = weights[it.categoryId] ?: 0,
                            totalSales = it.totalSales,
                            rating = it.rating,
                        )
                    },
                    limit,
                )
            val products = repository.loadResponses(ranked).toMutableList()
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

    suspend fun bestSellers(limit: Int): RecommendationResponse =
        cached("recommendations:best-sellers:$limit") {
            response("best-sellers", null, bestSellerProducts(limit))
        }

    private suspend fun bestSellerProducts(limit: Int): List<ProductResponse> {
        val products = validProducts(repository.activeProducts())
        val ranked =
            RecommendationRanking.rank(
                products.map {
                    RecommendationCandidate(
                        productId = it.id,
                        score = if (it.bestSeller) 1 else 0,
                        totalSales = it.totalSales,
                        rating = it.rating,
                    )
                },
                limit,
            )
        return repository.loadResponses(ranked)
    }

    private suspend fun requireProduct(productId: String): RecommendationProduct =
        repository.findActive(productId) ?: productId.throwNotFound("Product")

    private fun rankCandidates(
        target: RecommendationProduct,
        candidates: List<RecommendationProduct>,
        limit: Int,
    ): List<String> =
        RecommendationRanking.rank(
            candidates.map {
                RecommendationCandidate(
                    productId = it.id,
                    score = if (it.subCategoryId == target.subCategoryId) 2 else 1,
                    totalSales = it.totalSales,
                    rating = it.rating,
                )
            },
            limit,
        )

    private suspend fun fallbackResponse(
        productId: String,
        strategy: String,
        limit: Int,
    ): RecommendationResponse = response(strategy, productId, bestSellerProducts(limit))

    private suspend fun validProducts(products: List<RecommendationProduct>): List<RecommendationProduct> {
        val active = products.filter { it.status == ProductStatus.ACTIVE && it.deletedAt == null }
        val shopIds = active.mapNotNull { it.shopId }.distinct()
        if (shopIds.isEmpty()) return active
        val approved = repository.approvedShopIds(shopIds)
        return active.filter { product -> product.shopId == null || product.shopId in approved }
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
