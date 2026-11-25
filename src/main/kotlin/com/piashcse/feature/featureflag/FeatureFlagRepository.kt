package com.piashcse.feature.featureflag

import com.piashcse.database.entities.FeatureFlagDAO
import com.piashcse.database.entities.FeatureFlagTable
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

interface FeatureFlagRepository {
    suspend fun isEnabled(key: String): Boolean

    suspend fun list(): List<FeatureFlagResponse>

    suspend fun upsert(
        key: String,
        request: FeatureFlagUpdateRequest,
    ): FeatureFlagResponse
}

class FeatureFlagRepositoryImpl : FeatureFlagRepository {
    override suspend fun isEnabled(key: String): Boolean =
        query {
            FeatureFlagDAO.find { FeatureFlagTable.key eq key }.firstOrNull()?.enabled ?: false
        }

    override suspend fun list(): List<FeatureFlagResponse> =
        query {
            FeatureFlagTable.selectAll()
                .orderBy(FeatureFlagTable.key to SortOrder.ASC)
                .map { FeatureFlagDAO.wrapRow(it).toResponse() }
        }

    override suspend fun upsert(
        key: String,
        request: FeatureFlagUpdateRequest,
    ): FeatureFlagResponse =
        query {
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
