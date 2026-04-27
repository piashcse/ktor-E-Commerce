package com.piashcse.model.response

import kotlinx.serialization.Serializable

/**
 * Response model for policy documents
 */
@Serializable
data class PolicyDocumentResponse(
    val id: String,
    val title: String,
    val type: String,
    val content: String,
    val version: String,
    val effectiveDate: String,
    val isActive: Boolean,
)
