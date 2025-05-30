package com.piashcse.di

import com.piashcse.feature.auth.AuthService
import com.piashcse.feature.brand.BrandService
import com.piashcse.feature.cart.CartService
import com.piashcse.feature.consent.ConsentService
import com.piashcse.feature.order.OrderService
import com.piashcse.feature.payment.PaymentService
import com.piashcse.feature.policy.PolicyService
import com.piashcse.feature.product.ProductService
import com.piashcse.feature.product_category.ProductCategoryService
import com.piashcse.feature.product_sub_category.ProductSubCategoryService
import com.piashcse.feature.profile.ProfileService
import com.piashcse.feature.review_rating.ReviewRatingService
import com.piashcse.feature.shipping.ShippingService
import com.piashcse.feature.shop.ShopService
import com.piashcse.feature.shop_category.ShopCategoryService
import com.piashcse.feature.wishlist.WishListService
import org.koin.dsl.module

val controllerModule = module {
    single { BrandService() }
    single { CartService() }
    single { OrderService() }
    single { OrderService() }
    single { ProductService() }
    single { ProductCategoryService() }
    single { ProductSubCategoryService() }
    single { ShippingService() }
    single { ShopService() }
    single { ShopCategoryService() }
    single { AuthService() }
    single { ProfileService() }
    single { WishListService() }
    single { PaymentService() }
    single { ReviewRatingService() }
    single { PolicyService() }
    single { ConsentService() }
}