package com.piashcse.repository.base

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Generic CRUD base for simple reference entities (brands, categories,
 * sub-categories, etc.). Subclasses provide the entity's DAO class, table and
 * the mapping to a response DTO; the standard create/list/update/delete
 * operations are inherited, eliminating duplicated repository boilerplate.
 */
abstract class BaseCrudRepository<DAO : BaseEntity, RESP>(
    private val entityClass: BaseEntityClass<DAO>,
    private val table: BaseIdTable,
    private val entityLabel: String,
) {

    protected abstract fun DAO.toResponse(): RESP

    /** Creates a new entity using the given initializer. */
    protected suspend fun create(init: DAO.() -> Unit): RESP = query {
        entityClass.new(init).toResponse()
    }

    /** Returns a paginated list of all entities. */
    protected suspend fun getAll(limit: Int, offset: Int): PaginatedResponse<RESP> = query {
        table.selectAll().toPaginatedResponse(limit, offset) { entityClass.wrapRow(it).toResponse() }
    }

    /** Returns a non-paginated list of all entities. */
    protected suspend fun listAll(): List<RESP> = query {
        entityClass.all().map { it.toResponse() }
    }

    /** Returns a paginated list filtered by the given predicate. */
    protected suspend fun getAll(
        limit: Int,
        offset: Int,
        filter: () -> Op<Boolean>,
    ): PaginatedResponse<RESP> = query {
        val filtered = entityClass.find(filter)
        val data = filtered.limit(limit).offset(offset.toLong()).map { it.toResponse() }
        PaginatedResponse.of(data, filtered.count(), limit, offset)
    }

    /** Finds an entity by id or throws not-found. */
    protected suspend fun findByIdOrThrow(id: String): DAO = query {
        entityClass.findById(id) ?: id.throwNotFound(entityLabel)
    }

    /** Updates an existing entity and returns its response. */
    protected suspend fun update(id: String, block: DAO.() -> Unit): RESP = query {
        val entity = entityClass.findById(id) ?: id.throwNotFound(entityLabel)
        entity.block()
        entity.toResponse()
    }

    /** Deletes an entity by id and returns its id. */
    protected suspend fun delete(id: String): String = query {
        val entity = entityClass.findById(id) ?: id.throwNotFound(entityLabel)
        entity.delete()
        id
    }

    /** Returns true if an entity matching the predicate exists. */
    protected suspend fun exists(predicate: () -> Op<Boolean>): Boolean = query {
        entityClass.find(predicate).firstOrNull() != null
    }
}
