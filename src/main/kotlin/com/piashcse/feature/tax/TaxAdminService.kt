package com.piashcse.feature.tax

import com.piashcse.database.entities.ProductCategoryTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.TaxClassDAO
import com.piashcse.database.entities.TaxClassTable
import com.piashcse.database.entities.TaxExemptionDAO
import com.piashcse.database.entities.TaxRuleDAO
import com.piashcse.database.entities.TaxRuleTable
import com.piashcse.database.entities.TaxZoneDAO
import com.piashcse.database.entities.TaxZoneTable
import com.piashcse.database.entities.UserTable
import com.piashcse.utils.common.DateTimes
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.valiktor.functions.isNotBlank

/** Tax rule, class, zone, exemption and product-assignment administration. */
internal object TaxAdminService {
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
            val zoneId = request.zoneId?.takeIf { it.isNotBlank() }
            val taxClassId = request.taxClassId?.takeIf { it.isNotBlank() }
            requireCategory(categoryId)
            requireZone(zoneId)
            requireTaxClass(taxClassId)

            val existing =
                TaxRuleDAO.find { TaxRuleTable.country eq country }
                    .firstOrNull {
                        it.categoryId?.value == categoryId &&
                            it.zoneId?.value == zoneId &&
                            it.taxClassId?.value == taxClassId
                    }
            val dao =
                existing?.apply {
                    rate = request.rate
                    isInclusive = request.isInclusive
                    priority = request.priority
                    isActive = request.isActive
                } ?: TaxRuleDAO.new {
                    this.country = country
                    this.categoryId = categoryId?.entityID(ProductCategoryTable)
                    this.zoneId = zoneId?.entityID(TaxZoneTable)
                    this.taxClassId = taxClassId?.entityID(TaxClassTable)
                    this.rate = request.rate
                    this.isInclusive = request.isInclusive
                    this.priority = request.priority
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

    suspend fun listClasses(): List<TaxClassResponse> =
        query { TaxClassDAO.all().orderBy(TaxClassTable.name to SortOrder.ASC).map { it.toResponse() } }

    suspend fun createClass(request: TaxClassRequest): TaxClassResponse =
        query {
            if (TaxClassDAO.find { TaxClassTable.code eq request.code }.any()) {
                throw ValidationException("Tax class code already exists")
            }
            TaxClassDAO.new {
                name = request.name
                code = request.code
                rate = request.rate
                isInclusive = request.isInclusive
                isActive = request.isActive
            }.toResponse()
        }

    suspend fun updateClass(
        id: String,
        request: TaxClassRequest,
    ): TaxClassResponse =
        query {
            val taxClass = TaxClassDAO.findById(id) ?: id.throwNotFound("Tax class")
            taxClass.apply {
                name = request.name
                code = request.code
                rate = request.rate
                isInclusive = request.isInclusive
                isActive = request.isActive
            }.toResponse()
        }

    suspend fun deleteClass(id: String): Boolean =
        query {
            val taxClass = TaxClassDAO.findById(id) ?: return@query false
            taxClass.delete()
            true
        }

    suspend fun listZones(): List<TaxZoneResponse> =
        query { TaxZoneDAO.all().orderBy(TaxZoneTable.country to SortOrder.ASC).map { it.toResponse() } }

    suspend fun createZone(request: TaxZoneRequest): TaxZoneResponse =
        query {
            TaxZoneDAO.new {
                name = request.name
                country = request.country.uppercase()
                region = request.region?.takeIf { it.isNotBlank() }
                isNexus = request.isNexus
                priority = request.priority
                isActive = request.isActive
            }.toResponse()
        }

    suspend fun updateZone(
        id: String,
        request: TaxZoneRequest,
    ): TaxZoneResponse =
        query {
            val zone = TaxZoneDAO.findById(id) ?: id.throwNotFound("Tax zone")
            zone.apply {
                name = request.name
                country = request.country.uppercase()
                region = request.region?.takeIf { it.isNotBlank() }
                isNexus = request.isNexus
                priority = request.priority
                isActive = request.isActive
            }.toResponse()
        }

    suspend fun deleteZone(id: String): Boolean =
        query {
            val zone = TaxZoneDAO.findById(id) ?: return@query false
            zone.delete()
            true
        }

    suspend fun listExemptions(): List<TaxExemptionResponse> = query { TaxExemptionDAO.all().map { it.toResponse() } }

    suspend fun createExemption(request: TaxExemptionRequest): TaxExemptionResponse =
        query {
            request.userId?.takeIf { it.isNotBlank() }?.let {
                if (UserTable.selectAll().where { UserTable.id eq it.entityID(UserTable) }.count() == 0L) {
                    throw ValidationException("User not found")
                }
            }
            TaxExemptionDAO.new {
                userId = request.userId?.takeIf { it.isNotBlank() }?.entityID(UserTable)
                businessTaxId = request.businessTaxId?.takeIf { it.isNotBlank() }
                country = request.country?.takeIf { it.isNotBlank() }?.uppercase()
                reason = request.reason
                certificateUrl = request.certificateUrl
                isActive = request.isActive
                expiresAt = DateTimes.parseFlexibleOrNull(request.expiresAt, "expiresAt")
            }.toResponse()
        }

    suspend fun deleteExemption(id: String): Boolean =
        query {
            val exemption = TaxExemptionDAO.findById(id) ?: return@query false
            exemption.delete()
            true
        }

    suspend fun assignProductTaxClass(
        productId: String,
        taxClassId: String?,
    ) {
        query {
            val product = ProductDAO.findById(productId) ?: productId.throwNotFound("Product")
            val normalizedClass = taxClassId?.takeIf { it.isNotBlank() }
            requireTaxClass(normalizedClass)
            product.taxClassId = normalizedClass?.entityID(TaxClassTable)
        }
    }

    private fun requireCategory(categoryId: String?) {
        if (categoryId == null) return
        if (ProductCategoryTable.selectAll().where {
                ProductCategoryTable.id eq categoryId.entityID(ProductCategoryTable)
            }.count() == 0L
        ) {
            throw ValidationException("Category not found")
        }
    }

    private fun requireZone(zoneId: String?) {
        if (zoneId == null) return
        if (!TaxZoneDAO.find { TaxZoneTable.id eq zoneId.entityID(TaxZoneTable) }.any()) {
            throw ValidationException("Tax zone not found")
        }
    }

    private fun requireTaxClass(taxClassId: String?) {
        if (taxClassId == null) return
        if (!TaxClassDAO.find { TaxClassTable.id eq taxClassId.entityID(TaxClassTable) }.any()) {
            throw ValidationException("Tax class not found")
        }
    }

    private fun TaxRuleDAO.toResponse() =
        TaxRuleResponse(
            id = id.value,
            country = country,
            categoryId = categoryId?.value,
            zoneId = zoneId?.value,
            taxClassId = taxClassId?.value,
            rate = rate.toPlainString(),
            isInclusive = isInclusive,
            priority = priority,
            isActive = isActive,
        )

    private fun TaxClassDAO.toResponse() =
        TaxClassResponse(
            id = id.value,
            name = name,
            code = code,
            rate = rate.toPlainString(),
            isInclusive = isInclusive,
            isActive = isActive,
        )

    private fun TaxZoneDAO.toResponse() =
        TaxZoneResponse(
            id = id.value,
            name = name,
            country = country,
            region = region,
            isNexus = isNexus,
            priority = priority,
            isActive = isActive,
        )

    private fun TaxExemptionDAO.toResponse() =
        TaxExemptionResponse(
            id = id.value,
            userId = userId?.value,
            businessTaxId = businessTaxId,
            country = country,
            reason = reason,
            certificateUrl = certificateUrl,
            isActive = isActive,
            expiresAt = expiresAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
        )
}
