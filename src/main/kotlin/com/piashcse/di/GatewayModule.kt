package com.piashcse.di

import com.piashcse.config.DotEnvConfig
import com.piashcse.feature.payment.ManualPaymentGateway
import com.piashcse.feature.payment.PaymentGateway
import com.piashcse.feature.payment.PaymentRepository
import com.piashcse.feature.payment.PaymentRepositoryImpl
import com.piashcse.feature.payment.StripePaymentGateway
import com.piashcse.feature.shipping.CarrierGateway
import com.piashcse.feature.shipping.ManualCarrierGateway
import com.piashcse.feature.shipping.ShipmentService
import com.piashcse.feature.shipping.ShippoCarrierGateway
import com.piashcse.feature.webhook.WebhookAdminService
import org.koin.dsl.module

/** External provider adapters and the services that depend on them. */
val gatewayModule =
    module {
        single<CarrierGateway> {
            if (DotEnvConfig.shippingProvider == "shippo") {
                require(DotEnvConfig.shippoApiToken.isNotBlank()) {
                    "SHIPPO_API_TOKEN must be set when SHIPPING_PROVIDER=shippo"
                }
                ShippoCarrierGateway(
                    apiToken = DotEnvConfig.shippoApiToken,
                    webhookSecret = DotEnvConfig.carrierWebhookSecret,
                    baseUrl = DotEnvConfig.shippoBaseUrl,
                )
            } else {
                ManualCarrierGateway()
            }
        }
        single { ShipmentService(get()) }
        single<PaymentGateway> {
            if (DotEnvConfig.paymentProvider == "stripe") {
                require(DotEnvConfig.stripeSecretKey.isNotBlank()) {
                    "STRIPE_SECRET_KEY must be set when PAYMENT_PROVIDER=stripe"
                }
                require(DotEnvConfig.stripeWebhookSecret.isNotBlank()) {
                    "STRIPE_WEBHOOK_SECRET must be set when PAYMENT_PROVIDER=stripe"
                }
                StripePaymentGateway(
                    secretKey = DotEnvConfig.stripeSecretKey,
                    webhookSecret = DotEnvConfig.stripeWebhookSecret,
                    baseUrl = DotEnvConfig.stripeBaseUrl,
                    webhookToleranceSeconds = DotEnvConfig.stripeWebhookToleranceSeconds,
                )
            } else {
                ManualPaymentGateway()
            }
        }
        single<PaymentRepository> { PaymentRepositoryImpl(get(), get(), get(), get(), get(), get()) }
        single { WebhookAdminService(get(), get()) }
    }
