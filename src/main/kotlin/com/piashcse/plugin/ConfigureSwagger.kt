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
                    license = OpenApiInfo.License(name = "Apache-2.0"),
                )

            tag(ApiTags.AUTH, "Registration, login, OTP, refresh, logout and password reset")
            tag(ApiTags.PROFILE, "Customer profile and account details")
            tag(ApiTags.SHOP, "Marketplace shops and seller onboarding")
            tag(ApiTags.SHOP_CATEGORY, "Shop category taxonomy")
            tag(ApiTags.BRAND, "Product brands")
            tag(ApiTags.PRODUCT_CATEGORY, "Top-level catalog categories")
            tag(ApiTags.PRODUCT_SUB_CATEGORY, "Catalog subcategories")
            tag(ApiTags.PRODUCT, "Catalog, variants, options and recommendations")
            tag(ApiTags.REVIEW, "Product reviews and rating votes")
            tag(ApiTags.WISHLIST, "Saved products")
            tag(ApiTags.CART, "Customer cart and variants")
            tag(ApiTags.CHECKOUT, "Shipping addresses, totals, guest checkout and order placement")
            tag(ApiTags.ORDER, "Order history, tracking, status and cancellation")
            tag(ApiTags.PAYMENT, "Payment initiation, confirmation and admin reconciliation")
            tag(ApiTags.COUPON, "Coupon validation and administration")
            tag(ApiTags.SHIPPING_METHOD, "Available delivery methods and prices")
            tag(ApiTags.SHIPMENT, "Carrier rates, labels, parcels and tracking webhooks")
            tag(ApiTags.INVENTORY, "Warehouse and variant-level stock")
            tag(ApiTags.WAREHOUSE, "Seller physical locations")
            tag(ApiTags.REFUND, "Refund requests and lifecycle")
            tag(ApiTags.RMA, "Returns, exchanges and replacement orders")
            tag(ApiTags.NOTIFICATION, "Inbox, announcements and per-event channel preferences")
            tag(ApiTags.RECOMMENDATION, "Related, co-purchase and personalized feeds")
            tag(ApiTags.KYC, "Seller identity/business verification")
            tag(ApiTags.PAYOUT, "Seller balances, requests, statements and CSV export")
            tag(ApiTags.TAX, "Country/category tax rules")
            tag(ApiTags.POLICY, "Policy documents")
            tag(ApiTags.CONSENT, "Policy consent history")
            tag(ApiTags.PRIVACY, "GDPR export and erasure")
            tag(ApiTags.API_KEYS, "Scoped self-service API keys")
            tag(ApiTags.FEATURE_FLAGS, "Runtime feature switches")
            tag(ApiTags.OUTBOX, "Transactional outbox health and dead-letter replay")
            tag(ApiTags.HEALTH, "Liveness and readiness probes")
            tag(ApiTags.DASHBOARD, "Admin and seller metrics")
            tag(ApiTags.AUDIT_LOG, "Administrative audit trail")
            tag(ApiTags.FX, "Currency rates")
        }
    }
}
