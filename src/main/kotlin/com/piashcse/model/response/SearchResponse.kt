package com.piashcse.model.response

import com.piashcse.utils.common.PaginationMetadata
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val products: List<ProductResponse>,
    val metadata: PaginationMetadata,
    val facets: SearchFacets,
)

@Serializable
data class SearchFacets(
    val categories: List<FacetCount>,
    val brands: List<FacetCount>,
)

@Serializable
data class FacetCount(
    val id: String,
    val name: String,
    val count: Long,
)
