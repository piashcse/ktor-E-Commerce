@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.featureflag

import com.piashcse.constants.Permission
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.requirePermission
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Super-admin feature flag management. */
fun Route.featureFlagRoutes() {
    val featureFlags: FeatureFlags by inject()
    requirePermission(Permission.FEATURE_FLAG_MANAGE) {
        get {
            call.respondOk(featureFlags.list())
        }.describe {
            summary = "List feature flags"
            responses { ok<List<FeatureFlagResponse>>("Flags") }
        }

        put("{key}") {
            val key = call.requirePathParameter("key")
            call.respondOk(featureFlags.set(key, call.receive<FeatureFlagUpdateRequest>()))
        }.describe {
            summary = "Set feature flag"
            requestBody { jsonBody<FeatureFlagUpdateRequest>() }
            responses { ok<FeatureFlagResponse>("Updated flag") }
        }
    }
}
