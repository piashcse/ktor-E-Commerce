package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class SuggestionItem(
    val id: String,
    val text: String,
    /** PRODUCT, CATEGORY or BRAND */
    val type: String,
)

@Serializable
data class SearchSuggestionsResponse(
    val query: String,
    val suggestions: List<SuggestionItem>,
)
