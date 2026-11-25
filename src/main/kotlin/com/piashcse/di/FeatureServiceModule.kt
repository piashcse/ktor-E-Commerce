package com.piashcse.di

import com.piashcse.event.subscriber.NotificationSubscriber
import com.piashcse.feature.analytics.AnalyticsService
import com.piashcse.feature.apikey.ApiKeyService
import com.piashcse.feature.b2b.B2bService
import com.piashcse.feature.checkout.GuestCheckoutService
import com.piashcse.feature.cms.CmsService
import com.piashcse.feature.featureflag.FeatureFlagRepository
import com.piashcse.feature.featureflag.FeatureFlagRepositoryImpl
import com.piashcse.feature.featureflag.FeatureFlags
import com.piashcse.feature.fraud.ChargebackService
import com.piashcse.feature.fraud.FraudService
import com.piashcse.feature.i18n.I18nService
import com.piashcse.feature.i18n.ProductTranslationService
import com.piashcse.feature.invoice.InvoiceService
import com.piashcse.feature.kyc.KycService
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.omnichannel.OmnichannelService
import com.piashcse.feature.outbox.OutboxService
import com.piashcse.feature.privacy.PrivacyService
import com.piashcse.feature.product.ProductCatalogService
import com.piashcse.feature.product.ProductCrudService
import com.piashcse.feature.product.ProductVariantService
import com.piashcse.feature.profile.ProfileService
import com.piashcse.feature.promotion.PromotionService
import com.piashcse.feature.rbac.RbacService
import com.piashcse.feature.rbac.ShopMembershipService
import com.piashcse.feature.recommendation.RecommendationService
import com.piashcse.feature.rma.RmaService
import com.piashcse.feature.seller.CommissionService
import com.piashcse.feature.seller.SellerToolsService
import com.piashcse.feature.stockalert.StockAlertService
import com.piashcse.feature.subscription.SubscriptionBillingService
import com.piashcse.feature.subscription.SubscriptionService
import com.piashcse.feature.support.SupportAdminService
import com.piashcse.feature.support.SupportService
import com.piashcse.feature.tax.TaxService
import com.piashcse.feature.user.AdminUserService
import com.piashcse.service.DigitalDeliveryService
import com.piashcse.service.FxService
import com.piashcse.service.UploadService
import com.piashcse.service.payment.SubscriptionBillingPort
import org.koin.dsl.module

/** Feature/use-case services. */
val featureServiceModule =
    module {
        single { NotificationSubscriber(get()) }
        single { LoyaltyService() }
        single { I18nService() }
        single { AnalyticsService() }
        single { CommissionService() }
        single { SellerToolsService() }
        single { DigitalDeliveryService() }
        single { SubscriptionBillingService(get(), get()) }
        single<SubscriptionBillingPort> { get<SubscriptionBillingService>() }
        single { SubscriptionService(get(), get()) }
        single { B2bService(get(), get()) }
        single { OmnichannelService(get(), get(), get()) }
        single { CmsService() }
        single { SupportService() }
        single { SupportAdminService() }
        single { ProductTranslationService() }
        single { FraudService() }
        single { ChargebackService() }
        single { ProfileService(get()) }
        single { PrivacyService() }
        single { ApiKeyService() }
        single { RbacService() }
        single { ShopMembershipService() }
        single { GuestCheckoutService(get(), get(), get(), get()) }
        single { RmaService() }
        single { KycService() }
        single { AdminUserService() }
        single { InvoiceService() }
        single { StockAlertService() }
        single { PromotionService() }
        single { FxService() }
        single { TaxService() }
        single<FeatureFlagRepository> { FeatureFlagRepositoryImpl() }
        single { FeatureFlags(get()) }
        single { OutboxService() }

        // Catalog services
        single { ProductCatalogService(get()) }
        single { ProductCrudService(get()) }
        single { ProductVariantService() }
        single { RecommendationService() }

        single { UploadService }
    }
