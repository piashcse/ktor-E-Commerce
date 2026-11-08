package com.piashcse.feature.tax

import com.piashcse.constants.AppConstants
import com.piashcse.database.entities.ProductCategoryTable
import com.piashcse.database.entities.TaxRuleDAO
import com.piashcse.database.entities.TaxRuleTable
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal

@Serializable
data class TaxRuleRequest(
    val country: String,
    val categoryId: String? = null,
    @Contextual val rate: BigDecimal,
    val isActive: Boolean = true,
) {
    init {
        require(country.isNotBlank()) { "country is required" }
        require(rate >= BigDecimal.ZERO && rate <= BigDecimal.ONE) {
            "Tax rate must be between 0 and 1 (for example 0.05 = 5%)"
        }
    }
}

@Serializable
data class TaxRuleResponse(
    val id: String,
    val country: String,
    val categoryId: String?,
    val rate: String,
    val isActive: Boolean,
)

/**
 * Resolves a tax rate for a country/category. Resolution order:
 * category-specific rule → country default rule → application default (5%).
 */
object TaxService {
    fun resolve(
        country: String,
        categoryId: String?,
    ): BigDecimal {
        val normalized = country.uppercase()
        val specific =
            categoryId?.let {
                TaxRuleDAO.find {
                    (TaxRuleTable.country eq normalized) and
                        (TaxRuleTable.categoryId eq it.entityID(ProductCategoryTable)) and
                        (TaxRuleTable.isActive eq true)
                }.firstOrNull()
            }
        val fallback =
            TaxRuleDAO.find {
                (TaxRuleTable.country eq normalized) and
                    (TaxRuleTable.categoryId.isNull()) and
                    (TaxRuleTable.isActive eq true)
            }.firstOrNull()
        return specific?.rate ?: fallback?.rate ?: Money.of(AppConstants.DEFAULT_TAX_PERCENTAGE)
    }

    suspend fun list(): List<TaxRuleResponse> =
        query {
            TaxRuleTable.selectAll()
                .orderBy(TaxRuleTable.country to org.jetbrains.exposed.v1.core.SortOrder.ASC)
                .map { TaxRuleDAO.wrapRow(it).toResponse() }
        }

    suspend fun upsert(request: TaxRuleRequest): TaxRuleResponse =
        query {
            val country = request.country.uppercase()
            val categoryId = request.categoryId?.takeIf { it.isNotBlank() }
            if (categoryId != null) {
                ProductCategoryTable.selectAll()
                    .where { ProductCategoryTable.id eq categoryId.entityID(ProductCategoryTable) }
                    .count()
                    .let { if (it == 0L) throw ValidationException("Category not found") }
            }
            val existing =
                TaxRuleDAO.find {
                    (TaxRuleTable.country eq country) and
                        if (categoryId == null) {
                            TaxRuleTable.categoryId.isNull()
                        } else {
                            TaxRuleTable.categoryId eq categoryId.entityID(ProductCategoryTable)
                        }
                }.firstOrNull()

            val dao =
                existing?.apply {
                    rate = request.rate
                    isActive = request.isActive
                } ?: TaxRuleDAO.new {
                    this.country = country
                    this.categoryId = categoryId?.entityID(ProductCategoryTable)
                    this.rate = request.rate
                    this.isActive = request.isActive
                }
            dao.toResponse()
        }

    suspend fun delete(id: String): Boolean =
        query {
            val rule = TaxRuleDAO.findById(id) ?: return@query false
            rule.delete()
            true
        }

    private fun TaxRuleDAO.toResponse() =
        TaxRuleResponse(
            id = id.value,
            country = country,
            categoryId = categoryId?.value,
            rate = rate.toPlainString(),
            isActive = isActive,
        )
}
