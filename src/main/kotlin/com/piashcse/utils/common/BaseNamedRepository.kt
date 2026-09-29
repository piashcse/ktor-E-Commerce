package com.piashcse.utils.common

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwConflict
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq

/**
 * Generic CRUD base to deduplicate Brand / Category / ShopCategory / SubCategory repos.
 * Concrete repos only supply table hooks + mappers.
 */
abstract class BaseNamedRepository<DAO : BaseEntity, Response>(
    private val entityClass: BaseEntityClass<DAO>,
    private val resourceName: String,
) {
    protected abstract fun DAO.setNameValue(name: String)
    protected abstract fun DAO.getNameValue(): String
    protected abstract fun DAO.toResponse(): Response

    protected open fun validateName(name: String) {
        if (name.isBlank()) throw ValidationException("$resourceName name must not be blank")
        if (name.length > 255) throw ValidationException("$resourceName name must be <= 255 chars")
    }

    suspend fun create(name: String): Response = query {
        validateName(name)
        val existing = entityClass.find { nameColumn() eq name }.firstOrNull()
        if (existing != null) name.throwConflict(resourceName)
        entityClass.new { setNameValue(name) }.toResponse()
    }

    suspend fun getById(id: String): Response = query {
        (entityClass.findById(id) ?: id.throwNotFound(resourceName)).toResponse()
    }

    // Subclasses expose paginated lists with their own table since Exposed needs typed columns.
    // This base covers create/get/update/delete single-row ops.
    suspend fun update(id: String, name: String): Response = query {
        validateName(name)
        val entity = entityClass.findById(id) ?: id.throwNotFound(resourceName)
        entity.setNameValue(name)
        entity.toResponse()
    }

    suspend fun delete(id: String): Boolean = query {
        val entity = entityClass.findById(id) ?: id.throwNotFound(resourceName)
        entity.delete()
        true
    }

    // Overridden per-repo because Exposed Column references are table-specific.
    protected abstract fun nameColumn(): org.jetbrains.exposed.v1.core.Column<String>
}
