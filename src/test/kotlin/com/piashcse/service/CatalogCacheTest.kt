package com.piashcse.service

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatalogCacheTest {
    @Test
    fun `invalidates catalog and recommendation caches only`() =
        runBlocking {
            val cache = MemoryCache()
            cache.set("products:detail:1", "product")
            cache.set("products:list:1", "list")
            cache.set("recommendations:user:u:10", "recs")
            cache.set("orders:1", "order")

            CatalogCache.invalidate(cache)

            assertNull(cache.get<String>("products:detail:1"))
            assertNull(cache.get<String>("products:list:1"))
            assertNull(cache.get<String>("recommendations:user:u:10"))
            assertEquals("order", cache.get<String>("orders:1"))
        }
}
