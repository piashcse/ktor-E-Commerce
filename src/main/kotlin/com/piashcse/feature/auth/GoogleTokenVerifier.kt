package com.piashcse.feature.auth

import com.piashcse.utils.validator.ValidationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

data class SocialIdentity(
    val email: String,
    val subject: String,
    val name: String? = null,
)

interface SocialHttpTransport {
    suspend fun get(
        url: String,
        headers: Map<String, String>,
    ): SocialHttpResponse
}

data class SocialHttpResponse(
    val status: Int,
    val body: String,
)

class JdkSocialHttpTransport(
    private val timeout: Duration = Duration.ofSeconds(10),
) : SocialHttpTransport {
    private val client = HttpClient.newBuilder().connectTimeout(timeout).build()

    override suspend fun get(
        url: String,
        headers: Map<String, String>,
    ): SocialHttpResponse =
        withContext(Dispatchers.IO) {
            val request =
                HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .apply { headers.forEach { (key, value) -> header(key, value) } }
                    .GET()
                    .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            SocialHttpResponse(response.statusCode(), response.body())
        }
}

/** Verifies a Google ID token through the tokeninfo endpoint. */
class GoogleTokenVerifier(
    private val clientId: String,
    private val transport: SocialHttpTransport = JdkSocialHttpTransport(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun verify(idToken: String): SocialIdentity {
        if (clientId.isBlank()) throw ValidationException("Google login is not configured")
        if (idToken.isBlank()) throw ValidationException("idToken is required")
        val encoded = URLEncoder.encode(idToken, StandardCharsets.UTF_8)
        val response =
            transport.get(
                "https://oauth2.googleapis.com/tokeninfo?id_token=$encoded",
                mapOf("Accept" to "application/json"),
            )
        if (response.status !in 200..299) throw ValidationException("Google token verification failed")
        val root =
            runCatching { json.parseToJsonElement(response.body).jsonObject }.getOrNull()
                ?: throw ValidationException("Invalid Google token response")
        val audience = root.string("aud")
        if (audience != clientId) throw ValidationException("Google token audience mismatch")
        val verified = root.string("email_verified")?.toBooleanStrictOrNull() ?: false
        if (!verified) throw ValidationException("Google account email is not verified")
        val expires = root.string("exp")?.toLongOrNull() ?: 0
        if (expires <= clock() / 1000) throw ValidationException("Google token has expired")
        val email =
            root.string("email")?.lowercase()
                ?: throw ValidationException("Google token has no email")
        val subject = root.string("sub") ?: throw ValidationException("Google token has no subject")
        return SocialIdentity(email = email, subject = subject, name = root.string("name"))
    }

    private fun kotlinx.serialization.json.JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
}
