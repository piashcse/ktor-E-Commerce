package com.piashcse.feature.recommendation

import kotlin.test.Test
import kotlin.test.assertEquals

class RecommendationRankingTest {
    @Test
    fun `higher score wins`() {
        val ranked =
            RecommendationRanking.rank(
                listOf(
                    RecommendationCandidate("low", 1, 100, 5.0),
                    RecommendationCandidate("high", 3, 0, 0.0),
                ),
                limit = 10,
            )
        assertEquals(listOf("high", "low"), ranked)
    }

    @Test
    fun `sales then rating break ties`() {
        val ranked =
            RecommendationRanking.rank(
                listOf(
                    RecommendationCandidate("a", 1, 10, 4.0),
                    RecommendationCandidate("b", 1, 20, 3.0),
                    RecommendationCandidate("c", 1, 20, 4.9),
                ),
                limit = 10,
            )
        assertEquals(listOf("c", "b", "a"), ranked)
    }

    @Test
    fun `duplicates are removed and limit applied`() {
        val ranked =
            RecommendationRanking.rank(
                listOf(
                    RecommendationCandidate("a", 2, 0, 0.0),
                    RecommendationCandidate("a", 9, 0, 0.0),
                    RecommendationCandidate("b", 1, 0, 0.0),
                    RecommendationCandidate("c", 1, 0, 0.0),
                ),
                limit = 2,
            )
        assertEquals(listOf("a", "b"), ranked)
    }

    @Test
    fun `limit is clamped to a positive range`() {
        val ranked =
            RecommendationRanking.rank(
                listOf(RecommendationCandidate("a", 1, 0, 0.0), RecommendationCandidate("b", 1, 0, 0.0)),
                limit = 0,
            )
        assertEquals(listOf("a"), ranked)
    }
}
