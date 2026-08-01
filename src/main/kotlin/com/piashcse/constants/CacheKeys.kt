package com.piashcse.constants

/**
 * Cache key scheme for the application's caches. All cache entries are
 * invalidated together using the PRODUCTS_PATTERN wildcard.
 */
object CacheKeys {
    private const val PRODUCTS_PREFIX = "products:"

    const val PRODUCTS_PATTERN = "$PRODUCTS_PREFIX.*"

    object Products {
        const val DETAIL_PREFIX = "${PRODUCTS_PREFIX}detail:"
        const val FEATURED = "${PRODUCTS_PREFIX}featured"
        const val BEST_SELLING = "${PRODUCTS_PREFIX}best-selling"
        const val HOT_DEALS = "${PRODUCTS_PREFIX}hot-deals"

        fun detail(productId: String): String = "$DETAIL_PREFIX$productId"
    }
}
