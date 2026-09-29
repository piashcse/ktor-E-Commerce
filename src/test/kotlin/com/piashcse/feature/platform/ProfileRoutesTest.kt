package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.profile.ProfileRepository
import com.piashcse.feature.profile.ProfileService
import com.piashcse.feature.profile.profileRoutes
import com.piashcse.model.response.UserProfileResponse
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ProfileRoutesTest {
    private val repo: ProfileRepository = mockk()

    private fun sample() =
        UserProfileResponse(
            userId = "user-1",
            image = null,
            firstName = "John",
            lastName = "Doe",
            mobile = null,
            faxNumber = null,
            streetAddress = null,
            city = null,
            identificationType = null,
            identificationNo = null,
            occupation = null,
            postCode = null,
            gender = null,
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(
                module {
                    single<ProfileRepository> { repo }
                    single { ProfileService(get()) }
                },
            )
            routing { route("/api/v1/profile") { profileRoutes() } }
        }
    }

    @Test
    fun `get profile happy path returns 200`() =
        testApplication {
            coEvery { repo.getProfile("user-1") } returns sample()
            setup()
            val res =
                client.get("/api/v1/profile") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "John")
        }

    @Test
    fun `get profile without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/profile")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `update profile happy path returns 200`() =
        testApplication {
            coEvery { repo.updateProfile("user-1", any()) } returns sample().copy(firstName = "Jane")
            setup()
            val res =
                client.put("/api/v1/profile?firstName=Jane&lastName=Doe") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Jane")
        }

    @Test
    fun `update profile without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.put("/api/v1/profile?firstName=Jane") {
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `image-upload without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/profile/image-upload") {
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
