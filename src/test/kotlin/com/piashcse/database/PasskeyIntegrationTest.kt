package com.piashcse.database

import com.piashcse.constants.UserType
import com.piashcse.database.entities.UserDAO
import com.piashcse.feature.auth.AuthRepositoryImpl
import com.piashcse.feature.auth.PasskeyLoginOptionsRequest
import com.piashcse.feature.auth.PasskeyLoginRequest
import com.piashcse.feature.auth.PasskeyRegisterRequest
import com.piashcse.feature.auth.PasskeyService
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Assume
import org.testcontainers.containers.PostgreSQLContainer
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** WebAuthn passkey registration and assertion against PostgreSQL. */
class PasskeyIntegrationTest {
    private fun envDb(): Triple<String, String, String>? {
        val host = System.getenv("DB_HOST")?.takeIf { it.isNotBlank() } ?: return null
        val port = System.getenv("DB_PORT") ?: "5432"
        val name = System.getenv("DB_NAME") ?: "ktor-ecommerce"
        val user = System.getenv("DB_USER") ?: "postgres"
        val password = System.getenv("DB_PASSWORD") ?: "postgres"
        return Triple("jdbc:postgresql://$host:$port/$name", user, password)
    }

    private fun dockerAvailable(): Boolean =
        runCatching {
            ProcessBuilder("docker", "info").redirectErrorStream(true).start().waitFor() == 0
        }.getOrDefault(false)

    @Test
    fun `passkey registration and login`() {
        val external = envDb()
        if (external != null) {
            runPasskeyFlow(external.first, external.second, external.third)
            return
        }
        if (System.getenv("CI") != null) {
            assertTrue(dockerAvailable(), "DB_* environment or Docker is required in CI")
        } else {
            Assume.assumeTrue("Docker daemon or DB_* environment required", dockerAvailable())
        }
        val pg = PostgreSQLContainer("postgres:16-alpine")
        pg.start()
        try {
            runPasskeyFlow(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private fun runPasskeyFlow(
        jdbcUrl: String,
        user: String,
        dbPassword: String,
    ) {
        Flyway.configure()
            .dataSource(jdbcUrl, user, dbPassword)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        Database.connect(jdbcUrl, user = user, password = dbPassword)

        val suffix = UUID.randomUUID().toString().take(8)
        lateinit var userId: String
        lateinit var email: String
        transaction {
            val customer =
                UserDAO.new {
                    this.email = "passkey-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                    isActive = true
                }
            userId = customer.id.value
            email = customer.email
        }

        val service = PasskeyService(AuthRepositoryImpl(), "localhost", "Ktor E-Commerce", "http://localhost:3000")
        val keyPair = generateKeyPair()
        val credentialId = "cred-$suffix"

        val options = kotlinx.coroutines.runBlocking { service.registerOptions(userId) }
        val registerClientData =
            clientData(
                type = "webauthn.create",
                challenge = options.challenge,
            )
        val registered =
            kotlinx.coroutines.runBlocking {
                service.register(
                    userId,
                    PasskeyRegisterRequest(
                        credentialId = credentialId,
                        clientDataJson = base64Url(registerClientData.toByteArray()),
                        publicKey = Base64.getEncoder().encodeToString(keyPair.public.encoded),
                        name = "Test key",
                    ),
                )
            }
        assertEquals(credentialId, registered.credentialId)
        assertEquals(1, kotlinx.coroutines.runBlocking { service.list(userId) }.size)

        val loginOptions =
            kotlinx.coroutines.runBlocking {
                service.loginOptions(PasskeyLoginOptionsRequest(email, "customer"))
            }
        assertTrue(loginOptions.allowCredentials.any { it.id == credentialId })
        val loginClientData =
            clientData(
                type = "webauthn.get",
                challenge = loginOptions.challenge,
            )
        val authData = ByteArray(37).also { it[32] = 0x01 }
        val signature =
            sign(
                keyPair,
                authData + MessageDigest.getInstance("SHA-256").digest(loginClientData.toByteArray()),
            )
        val login =
            kotlinx.coroutines.runBlocking {
                service.login(
                    PasskeyLoginRequest(
                        email = email,
                        userType = "customer",
                        credentialId = credentialId,
                        clientDataJson = base64Url(loginClientData.toByteArray()),
                        authenticatorData = base64Url(authData),
                        signature = base64Url(signature),
                    ),
                )
            }
        assertTrue(login.accessToken.isNotBlank())
        assertTrue(login.refreshToken.isNotBlank())
        assertEquals(credentialId, kotlinx.coroutines.runBlocking { service.list(userId) }.single().credentialId)
    }

    private fun clientData(
        type: String,
        challenge: String,
    ): String = """{"type":"$type","challenge":"$challenge","origin":"http://localhost:3000"}"""

    private fun generateKeyPair(): KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()

    private fun sign(
        keyPair: KeyPair,
        data: ByteArray,
    ): ByteArray =
        Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(data)
            sign()
        }

    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
