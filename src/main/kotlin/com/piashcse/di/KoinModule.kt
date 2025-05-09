package com.piashcse.di

import com.piashcse.modules.auth.AuthService
import com.piashcse.modules.brand.BrandService
import com.piashcse.modules.cart.CartService
import com.piashcse.modules.consent.ConsentService
import com.piashcse.modules.order.OrderService
import com.piashcse.modules.payment.PaymentService
import com.piashcse.modules.policy.PolicyService
import com.piashcse.modules.product.ProductService
import com.piashcse.modules.productcategory.ProductCategoryService
import com.piashcse.modules.productsubcategory.ProductSubCategoryService
import com.piashcse.modules.profile.ProfileService
import com.piashcse.modules.review_rating.ReviewRatingService
import com.piashcse.modules.shipping.ShippingService
import com.piashcse.modules.shop.ShopService
import com.piashcse.modules.shopcategory.ShopCategoryService
import com.piashcse.modules.wishlist.controller.WishListController
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
    single { WishListController() }
    single { PaymentService() }
    single { ReviewRatingService() }
    single { PolicyService() }
    single { ConsentService() }
}