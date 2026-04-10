package com.piashcse.feature.auth

import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.currentUserId
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines routes for token management: refresh, logout, logout-all.
 *
 * @param tokenService The service handling token operations.
 */
fun Route.tokenRoutes(tokenService: TokenService) {
    route("/auth") {

        /**
         * @tag Auth
         * @description Refresh an access token using a valid refresh token.
         * The old refresh token is revoked and a new token pair is issued (rotation).
         * @operationId refreshToken
         * @body RefreshTokenRequest { "refreshToken": "string" }
         * @response 200 New token pair issued successfully
         * @response 401 Invalid, expired, or revoked refresh token
         */
        post("refresh") {
            val body = call.receive<RefreshTokenRequest>()
            val ipAddress = call.request.local.remoteHost
            val userAgent = call.request.headers["User-Agent"]

            val tokens = tokenService.refreshAccessToken(
                body.refreshToken,
                ipAddress,
                userAgent
            )

            call.respond(ApiResponse.success(tokens, HttpStatusCode.OK))
        }

        /**
         * @tag Auth
         * @description Logout: revoke the current refresh token.
         * @operationId logout
         * @body RefreshTokenRequest { "refreshToken": "string" }
         * @response 200 Logged out successfully
         */
        post("logout") {
            val body = call.receive<RefreshTokenRequest>()
            tokenService.revokeRefreshToken(body.refreshToken)
            call.respond(ApiResponse.success("Logged out successfully", HttpStatusCode.OK))
        }

        /**
         * @tag Auth
         * @description Logout from all devices: revoke all refresh tokens for the authenticated user.
         * @operationId logoutAll
         * @response 200 Logged out from all devices successfully
         * @security jwtToken
         */
        post("logout-all") {
            val userId = call.currentUserId
            tokenService.revokeAllUserTokens(userId)
            call.respond(ApiResponse.success("Logged out from all devices", HttpStatusCode.OK))
        }
    }
}

/**
 * Request body for token refresh and logout operations.
 */
data class RefreshTokenRequest(
    val refreshToken: String
)
