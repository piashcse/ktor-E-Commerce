@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.subscription

import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Public subscription plans. */
fun Route.subscriptionPlanRoutes() {
    val service: SubscriptionService by inject()
    get {
        call.respondOk(service.plans(call.request.queryParameters["productId"]))
    }.describe { summary = "List active subscription plans" }
}

/** Customer subscriptions and invoices. */
fun Route.subscriptionRoutes() {
    val service: SubscriptionService by inject()
    requireRole {
        get {
            call.respondOk(service.mySubscriptions(call.currentUserId))
        }.describe { summary = "List my subscriptions" }

        get("{id}/invoices") {
            call.respondOk(service.invoicesForUser(call.currentUserId, call.requirePathParameter("id")))
        }.describe { summary = "List invoices for my subscription" }

        writeRateLimit {
            post {
                call.respondCreated(service.subscribe(call.currentUserId, call.receive<SubscribeRequest>()))
            }.describe {
                summary = "Subscribe to a plan"
                requestBody { jsonBody<SubscribeRequest>() }
                responses { created<SubscriptionResponse>("Subscription created") }
            }

            post("{id}/cancel") {
                call.respondOk(service.cancel(call.currentUserId, call.requirePathParameter("id")))
            }.describe { summary = "Cancel at period end" }
        }
    }
}

/** Admin subscription-plan CRUD. */
fun Route.subscriptionPlanAdminRoutes() {
    val service: SubscriptionService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.listPlans(limit, offset))
    }.describe { summary = "List subscription plans (admin)" }

    adminWriteRateLimit {
        post {
            call.respondCreated(service.createPlan(call.receive<SubscriptionPlanRequest>(), call.currentUserId))
        }.describe {
            summary = "Create a subscription plan"
            requestBody { jsonBody<SubscriptionPlanRequest>() }
            responses { created<SubscriptionPlanResponse>("Plan created") }
        }

        put("{id}") {
            call.respondOk(
                service.updatePlan(call.requirePathParameter("id"), call.receive<SubscriptionPlanRequest>(), call.currentUserId),
            )
        }.describe {
            summary = "Update a subscription plan"
            requestBody { jsonBody<SubscriptionPlanRequest>() }
            responses { ok<SubscriptionPlanResponse>("Plan updated") }
        }

        delete("{id}") {
            val id = call.requirePathParameter("id")
            if (!service.deletePlan(id, call.currentUserId)) throw NotFoundException("Subscription plan not found")
            call.respondOk(MessageResponse("Subscription plan deactivated"))
        }.describe { summary = "Deactivate a subscription plan" }
    }
}

/** Admin subscription oversight and manual billing run. */
fun Route.subscriptionAdminRoutes() {
    val service: SubscriptionService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.adminSubscriptions(limit, offset))
    }.describe { summary = "List all subscriptions" }

    adminWriteRateLimit {
        post("run-billing") {
            call.respondOk(service.runBilling(actorUserId = call.currentUserId))
        }.describe {
            summary = "Create renewal invoices for due subscriptions"
            description = "Creates pending invoices, marks subscriptions PAST_DUE and cancels at period end"
        }

        post("{id}/activate") {
            call.respondOk(
                service.activate(
                    call.requirePathParameter("id"),
                    call.receive<ActivateSubscriptionRequest>().paymentReference,
                    call.currentUserId,
                ),
            )
        }.describe {
            summary = "Admin: mark a pending subscription invoice paid and activate"
            requestBody { jsonBody<ActivateSubscriptionRequest>() }
            responses { ok<SubscriptionResponse>("Subscription activated") }
        }
    }
}

/** Customer digital library. */
fun Route.digitalLibraryRoutes() {
    val deliveries: DigitalDeliveryService by inject()

    get("asset") {
        val token =
            call.request.queryParameters["token"]
                ?: throw ValidationException("token is required")
        val expires =
            call.request.queryParameters["expires"]?.toLongOrNull()
                ?: throw ValidationException("expires is required")
        val signature =
            call.request.queryParameters["signature"]
                ?: throw ValidationException("signature is required")
        call.respondRedirect(deliveries.asset(token, expires, signature), permanent = false)
    }.describe {
        summary = "Resolve a short-lived digital download link"
        description = "Validates the signed expiry and redirects to the asset"
    }

    requireRole {
        get {
            call.respondOk(deliveries.library(call.currentUserId))
        }.describe { summary = "List my digital purchases" }

        get("{token}/download") {
            call.respondOk(deliveries.download(call.requirePathParameter("token"), call.currentUserId))
        }.describe { summary = "Redeem a digital download" }
    }
}
