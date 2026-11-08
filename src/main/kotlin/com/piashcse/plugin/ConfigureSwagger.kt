package com.piashcse.plugin

import com.piashcse.constants.AppConstants
import io.ktor.openapi.*
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.registerBearerAuthSecurityScheme

fun Application.configureSwagger() {
    // Make the two bearer providers explicit and human-readable in Swagger UI.
    registerBearerAuthSecurityScheme(
        name = AppConstants.Authentication.JWT_AUTHENTICATOR,
        description = "JWT access token from POST /api/v1/auth/login. Send as: Authorization: Bearer <accessToken>.",
        bearerFormat = "JWT",
    )
    registerBearerAuthSecurityScheme(
        name = AppConstants.Authentication.API_KEY_AUTHENTICATOR,
        description = "Scoped API key from POST /api/v1/api-keys. Send as: Authorization: Bearer <apiKey>.",
        bearerFormat = "API key",
    )

    routing {
        swaggerUI(path = "swagger") {
            info =
                OpenApiInfo(
                    title = "Ktor E-Commerce API",
                    version = "1.0.0",
                    description =
                        "Complete e-commerce API: auth, catalog, variants, recommendations, cart, checkout, " +
                            "orders, payments, refunds, RMA, notifications, seller KYC/payouts and admin tools.\n\n" +
                            "Authenticated operations accept either the **jwt-auth** bearer token or a scoped " +
                            "**api-key** bearer token. Get a JWT from `POST /api/v1/auth/login`.\n\n" +
                            "Human-readable guides: https://piashcse.github.io/ktor-ecommerce/",
                    termsOfService = "https://piashcse.github.io/",
                    contact = OpenApiInfo.Contact(name = "Mehedi Hassan Piash", email = "piash599@gmail.com"),
                    license = OpenApiInfo.License(name = "MIT"),
                )

            tag("Auth", "Registration, login, OTP, refresh, logout and password reset")
            tag("Profile", "Customer profile and account details")
            tag("Shop", "Marketplace shops and seller onboarding")
            tag("Shop Category", "Shop category taxonomy")
            tag("Brand", "Product brands")
            tag("Product Category", "Top-level catalog categories")
            tag("Product Sub Category", "Catalog subcategories")
            tag("Product", "Catalog, variants, options and recommendations")
            tag("Review", "Product reviews and rating votes")
            tag("Wishlist", "Saved products")
            tag("Cart", "Customer cart and variants")
            tag("Checkout", "Shipping addresses, totals, guest checkout and order placement")
            tag("Order", "Order history, tracking, status and cancellation")
            tag("Payment", "Payment initiation, confirmation and admin reconciliation")
            tag("Coupon", "Coupon validation and administration")
            tag("Shipping Method", "Available delivery methods and prices")
            tag("Inventory", "Warehouse and variant-level stock")
            tag("Warehouse", "Seller physical locations")
            tag("Refund", "Refund requests and lifecycle")
            tag("RMA", "Returns, exchanges and replacement orders")
            tag("Notification", "Inbox, announcements and per-event channel preferences")
            tag("Recommendation", "Related, co-purchase and personalized feeds")
            tag("KYC", "Seller identity/business verification")
            tag("Payout", "Seller balances, requests, statements and CSV export")
            tag("Tax", "Country/category tax rules")
            tag("Policy", "Policy documents")
            tag("Consent", "Policy consent history")
            tag("Privacy", "GDPR export and erasure")
            tag("API Keys", "Scoped self-service API keys")
            tag("Feature Flags", "Runtime feature switches")
            tag("Outbox", "Transactional outbox health and dead-letter replay")
            tag("Health", "Liveness and readiness probes")
            tag("Dashboard", "Admin and seller metrics")
            tag("Audit Log", "Administrative audit trail")
            tag("FX", "Currency rates")
        }
    }
}
