package com.piashcse.feature.featureflag

import com.piashcse.database.entities.FeatureFlagDAO
import com.piashcse.database.entities.FeatureFlagTable
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.query
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

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

class FeatureFlags {
    suspend fun isEnabled(key: String): Boolean =
        query {
            FeatureFlagDAO.find { FeatureFlagTable.key eq key }.firstOrNull()?.enabled ?: false
        }

    suspend fun list(): List<FeatureFlagResponse> =
        query {
            FeatureFlagTable.selectAll()
                .orderBy(FeatureFlagTable.key to org.jetbrains.exposed.v1.core.SortOrder.ASC)
                .map { FeatureFlagDAO.wrapRow(it).toResponse() }
        }

    suspend fun set(
        key: String,
        request: FeatureFlagUpdateRequest,
    ): FeatureFlagResponse =
        query {
            if (key.isBlank()) throw ValidationException("feature flag key is required")
            val dao =
                FeatureFlagDAO.find { FeatureFlagTable.key eq key }.firstOrNull()
                    ?: FeatureFlagDAO.new {
                        this.key = key
                        enabled = request.enabled
                        description = request.description
                    }
            dao.enabled = request.enabled
            request.description?.let { dao.description = it }
            dao.toResponse()
        }

    private fun FeatureFlagDAO.toResponse() =
        FeatureFlagResponse(
            key = key,
            enabled = enabled,
            description = description,
            updatedAt = updatedAt?.toUtcString(),
        )
}
