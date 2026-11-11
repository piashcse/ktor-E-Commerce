package com.piashcse.feature.i18n

import com.piashcse.constants.Localization
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTranslationDAO
import com.piashcse.database.entities.ProductTranslationTable
import com.piashcse.model.response.ProductResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll

@Serializable
data class ProductTranslationRequest(
    val name: String,
    val description: String,
) {
    init {
        if (name.isBlank()) throw FieldValidationException(listOf(FieldViolation("name", "Name is required")))
        if (description.isBlank()) {
            throw FieldValidationException(listOf(FieldViolation("description", "Description is required")))
        }
    }
}

@Serializable
data class ProductTranslationResponse(
    val productId: String,
    val locale: String,
    val name: String,
    val description: String,
)

/** Product content translations with the default product row as the fallback. */
class ProductTranslationService {
    suspend fun list(productId: String): List<ProductTranslationResponse> =
        query {
            ensureProduct(productId)
            ProductTranslationTable.selectAll()
                .where { ProductTranslationTable.productId eq productId.entityID(com.piashcse.database.entities.ProductTable) }
                .map { ProductTranslationDAO.wrapRow(it).toResponse() }
        }

    suspend fun upsert(
        productId: String,
        locale: String,
        request: ProductTranslationRequest,
    ): ProductTranslationResponse =
        query {
            ensureProduct(productId)
            val normalized = Localization.requireValid(locale)
            val existing =
                ProductTranslationDAO.find {
                    (ProductTranslationTable.productId eq productId.entityID(com.piashcse.database.entities.ProductTable)) and
                        (ProductTranslationTable.locale eq normalized)
                }.firstOrNull()
            val dao =
                existing?.apply {
                    name = request.name
                    description = request.description
                } ?: ProductTranslationDAO.new {
                    this.productId = productId.entityID(com.piashcse.database.entities.ProductTable)
                    this.locale = normalized
                    this.name = request.name
                    this.description = request.description
                }
            dao.toResponse()
        }

    suspend fun delete(
        productId: String,
        locale: String,
    ): Boolean =
        query {
            val normalized = Localization.requireValid(locale)
            val existing =
                ProductTranslationDAO.find {
                    (ProductTranslationTable.productId eq productId.entityID(com.piashcse.database.entities.ProductTable)) and
                        (ProductTranslationTable.locale eq normalized)
                }.firstOrNull() ?: return@query false
            existing.delete()
            true
        }

    /** Applies a translation when available; returns the original response otherwise. */
    suspend fun localize(
        response: ProductResponse,
        locale: String,
    ): ProductResponse {
        if (Localization.normalize(locale) == Localization.DEFAULT_LOCALE) return response
        val translation = translationFor(response.id, locale) ?: return response
        return response.copy(name = translation.name, description = translation.description)
    }

    suspend fun localizeAll(
        responses: List<ProductResponse>,
        locale: String,
    ): List<ProductResponse> {
        val normalized = Localization.normalize(locale)
        if (normalized == Localization.DEFAULT_LOCALE || responses.isEmpty()) return responses
        val translations = translationsFor(responses.map { it.id }, normalized)
        return responses.map { response ->
            translations[response.id]?.let { response.copy(name = it.name, description = it.description) } ?: response
        }
    }

    suspend fun localize(
        page: PaginatedResponse<ProductResponse>,
        locale: String,
    ): PaginatedResponse<ProductResponse> = page.copy(data = localizeAll(page.data, locale))

    private suspend fun translationFor(
        productId: String,
        locale: String,
    ): ProductTranslationResponse? =
        query {
            ProductTranslationDAO.find {
                (ProductTranslationTable.productId eq productId.entityID(com.piashcse.database.entities.ProductTable)) and
                    (ProductTranslationTable.locale eq Localization.normalize(locale))
            }.firstOrNull()?.toResponse()
        }

    private suspend fun translationsFor(
        productIds: List<String>,
        locale: String,
    ): Map<String, ProductTranslationResponse> =
        query {
            ProductTranslationTable.selectAll()
                .where {
                    (
                        ProductTranslationTable.productId inList
                            productIds.map { it.entityID(com.piashcse.database.entities.ProductTable) }
                    ) and
                        (ProductTranslationTable.locale eq locale)
                }.associate { row ->
                    val translation = ProductTranslationDAO.wrapRow(row)
                    translation.productId.value to translation.toResponse()
                }
        }

    private fun ensureProduct(productId: String) {
        ProductDAO.findById(productId) ?: productId.throwNotFound("Product")
    }

    private fun ProductTranslationDAO.toResponse() =
        ProductTranslationResponse(
            productId = productId.value,
            locale = locale,
            name = name,
            description = description,
        )
}
