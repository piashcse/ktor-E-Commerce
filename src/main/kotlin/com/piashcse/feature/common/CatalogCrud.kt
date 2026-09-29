package com.piashcse.feature.common

import com.piashcse.utils.extension.requireValidName
import com.piashcse.utils.extension.throwConflict
import com.piashcse.utils.extension.throwNotFound

/**
 * Shared single-name catalog CRUD — Brand / ProductCategory / ShopCategory /
 * ProductSubCategory create+rename+delete paths.
 *
 * Behavior-preserving: validation labels, conflict/not-found resource strings,
 * reference guards and response mappings stay at the call sites; only the
 * find-or-throw / find-or-conflict plumbing is folded here.
 */
object CatalogCrud {
    fun <D, R> createUniqueByName(
        name: String,
        validationLabel: String?,
        conflictLabel: String,
        findExisting: () -> D?,
        create: () -> D,
        toResponse: (D) -> R,
    ): R {
        if (validationLabel != null) name.requireValidName(validationLabel)
        findExisting()?.let { throw name.throwConflict(conflictLabel) }
        return toResponse(create())
    }

    fun <D, R> renameById(
        id: String,
        name: String,
        validationLabel: String?,
        notFoundLabel: String,
        findById: (String) -> D?,
        rename: (D, String) -> Unit,
        toResponse: (D) -> R,
    ): R {
        if (validationLabel != null) name.requireValidName(validationLabel)
        val entity = findById(id) ?: id.throwNotFound(notFoundLabel)
        rename(entity, name)
        return toResponse(entity)
    }

    fun <D> deleteById(
        id: String,
        notFoundLabel: String,
        findById: (String) -> D?,
        guard: ((D) -> Unit)? = null,
        delete: (D) -> Unit,
    ): String {
        val entity = findById(id) ?: id.throwNotFound(notFoundLabel)
        guard?.invoke(entity)
        delete(entity)
        return id
    }
}
