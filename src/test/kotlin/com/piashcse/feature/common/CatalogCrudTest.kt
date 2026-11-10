package com.piashcse.feature.common

import com.piashcse.utils.validator.ConflictException
import com.piashcse.utils.validator.NotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CatalogCrudTest {
    @Test
    fun `create throws a resource-specific conflict`() {
        val error =
            assertFailsWith<ConflictException> {
                CatalogCrud.createUniqueByName<String, String>(
                    name = "Nike",
                    validationLabel = "Brand",
                    conflictLabel = "Brand",
                    findExisting = { "existing" },
                    create = { "new" },
                    toResponse = { it },
                )
            }
        assertEquals("Brand already exists", error.message)
    }

    @Test
    fun `rename throws a readable not-found message`() {
        val error =
            assertFailsWith<NotFoundException> {
                CatalogCrud.renameById<String, String>(
                    id = "missing",
                    name = "Nike",
                    validationLabel = "Brand",
                    notFoundLabel = "Brand",
                    findById = { null },
                    rename = { _, _ -> },
                    toResponse = { it },
                )
            }
        assertEquals("Brand not found", error.message)
    }

    @Test
    fun `delete runs the guard before deleting`() {
        var deleted = false
        assertFailsWith<ConflictException> {
            CatalogCrud.deleteById(
                id = "b-1",
                notFoundLabel = "Brand",
                findById = { "brand" },
                guard = { throw ConflictException("still referenced") },
                delete = { deleted = true },
            )
        }
        assertEquals(false, deleted)
    }
}
