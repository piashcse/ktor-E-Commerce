package com.example.utils.extension

import com.example.models.user.JwtTokenBody
import com.example.utils.AppConstants
import com.papsign.ktor.openapigen.model.Described
import com.papsign.ktor.openapigen.model.security.HttpSecurityScheme
import com.papsign.ktor.openapigen.model.security.SecuritySchemeModel
import com.papsign.ktor.openapigen.model.security.SecuritySchemeType
import com.papsign.ktor.openapigen.modules.providers.AuthProvider
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import io.ktor.server.application.*
import io.ktor.server.auth.*

import io.ktor.util.pipeline.*

class JwtAuthProvider(private vararg val role: String?) : AuthProvider<JwtTokenBody> {
    enum class Scopes(override val description: String) : Described

    override val security: Iterable<Iterable<AuthProvider.Security<*>>> = listOf(
        listOf(
            AuthProvider.Security(
                SecuritySchemeModel(
                    referenceName = "ktor.io",
                    type = SecuritySchemeType.http,
                    scheme = HttpSecurityScheme.bearer,
                    bearerFormat = "JWT",
                    name = "Authentication"
                ), emptyList<Scopes>()
            )
        )
    )

    override suspend fun getAuth(pipeline: PipelineContext<Unit, ApplicationCall>): JwtTokenBody =
        pipeline.context.principal() ?: throw Exception("No JWTPrincipal!")

    override fun apply(route: NormalOpenAPIRoute): OpenAPIAuthenticatedRoute<JwtTokenBody> {
        val authenticatedKtorRoute = route.ktorRoute.authenticate(*role) {}
        return OpenAPIAuthenticatedRoute(authenticatedKtorRoute, route.provider.child(), this)
    }
}

inline fun NormalOpenAPIRoute.authenticateWithJwt(
    vararg roleManagement: String?, route: OpenAPIAuthenticatedRoute<JwtTokenBody>.() -> Unit
): OpenAPIAuthenticatedRoute<JwtTokenBody> = JwtAuthProvider(*roleManagement).apply(this).apply(route)