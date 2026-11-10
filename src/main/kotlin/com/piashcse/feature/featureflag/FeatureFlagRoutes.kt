package com.piashcse.feature.featureflag

import com.piashcse.constants.Permission
import com.piashcse.plugin.requirePermission
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.ValidationException
import io.ktor.server.request.receive
import io.ktor.server.routing.*

/** Super-admin feature flag management. */
fun Route.featureFlagRoutes() {
    requirePermission(Permission.FEATURE_FLAG_MANAGE) {
        get {
            call.respondOk(FeatureFlags.list())
        }

        put("{key}") {
            val key =
                call.parameters["key"]?.takeIf { it.isNotBlank() }
                    ?: throw ValidationException("key is required")
            call.respondOk(FeatureFlags.set(key, call.receive<FeatureFlagUpdateRequest>()))
        }
    }
}
