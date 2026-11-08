package com.piashcse.service

/**
 * Central catalog cache invalidation. Product/variant/review/inventory writes
 * all invalidate the same derived feeds, so the patterns live in one place.
 */
object CatalogCache {
    private const val PRODUCTS = "products:.*"
    private const val RECOMMENDATIONS = "recommendations:.*"

    suspend fun invalidate(cache: Cache = CacheService.cache) {
        cache.invalidatePattern(PRODUCTS)
        cache.invalidatePattern(RECOMMENDATIONS)
    }
}
