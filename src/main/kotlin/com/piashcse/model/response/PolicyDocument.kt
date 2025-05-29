package com.piashcse.model.response

/**
 * Response model for policy documents
 */
data class PolicyDocument(
    val id: String,
    val title: String,
    val type: String,
    val content: String,
    val version: String,
    val effectiveDate: String,
    val isActive: Boolean,
)
