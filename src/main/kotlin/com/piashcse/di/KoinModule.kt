package com.piashcse.di

import com.piashcse.controller.*
import org.koin.dsl.module

val controllerModule = module {
    single { BrandController() }
    single { CartController() }
    single { OrderController() }
    single { OrderController() }
    single { ProductController() }
    single { ProductCategoryController() }
    single { ProductSubCategoryController() }
    single { ShippingController() }
    single { ShopController() }
    single { ShopCategoryController() }
    single { AuthController() }
    single { ProfileController() }
    single { WishListController() }
    single { PaymentController() }
    single { ReviewRatingController() }
    single { PolicyController() }
    single { ConsentController() }
}