package com.piashcse.utils.extension

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.utils.validator.ForbiddenException

/**
 * Verifies that the resource is owned by the specified user ID.
 * Throws ForbiddenException if the user is not the owner.
 */
fun <T : BaseEntity> T.verifyOwnership(expectedUserId: String, resourceName: String, getUserId: (T) -> String): T {
    if (getUserId(this) != expectedUserId) {
        throw ForbiddenException("You do not own this $resourceName")
    }
    return this
}
