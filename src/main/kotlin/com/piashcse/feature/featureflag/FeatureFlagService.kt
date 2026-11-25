package com.piashcse.feature.featureflag

import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable

@Serializable
data class FeatureFlagResponse(
    val key: String,
    val enabled: Boolean,
    val description: String?,
    val updatedAt: String?,
)

@Serializable
data class FeatureFlagUpdateRequest(
    val enabled: Boolean,
    val description: String? = null,
)

class FeatureFlags(
    private val repository: FeatureFlagRepository,
) {
    suspend fun isEnabled(key: String): Boolean = repository.isEnabled(key)

    suspend fun list(): List<FeatureFlagResponse> = repository.list()

    suspend fun set(
        key: String,
        request: FeatureFlagUpdateRequest,
    ): FeatureFlagResponse {
        if (key.isBlank()) throw ValidationException("feature flag key is required")
        return repository.upsert(key, request)
    }
}
