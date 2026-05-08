package com.piashcse.plugin

import com.piashcse.model.request.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*

fun Application.configureRequestValidation() {
    install(RequestValidation) {
        validate<LoginRequest> {
            it.validation()
            ValidationResult.Valid
        }
        validate<RegisterRequest> {
            it.validation()
            ValidationResult.Valid
        }
        validate<ProductSearchRequest> {
            it.validation()
            ValidationResult.Valid
        }
        validate<ProductCategoryRequest> {
            it.validation()
            ValidationResult.Valid
        }
        validate<ProductSubCategoryRequest> {
            it.validation()
            ValidationResult.Valid
        }
        validate<ShopRequest> {
            it.validation()
            ValidationResult.Valid
        }
        validate<ShopCategoryRequest> {
            it.validation()
            ValidationResult.Valid
        }
        validate<BrandRequest> {
            it.validation()
            ValidationResult.Valid
        }
        validate<ProductRequest> {
            it.validation()
            ValidationResult.Valid
        }
        validate<ShippingAddressRequest> {
            it.validation()
            ValidationResult.Valid
        }
        validate<ShippingMethodRequest> {
            it.validation()
            ValidationResult.Valid
        }
        validate<OrderRequest> {
            it.validation()
            ValidationResult.Valid
        }
        validate<CartRequest> {
            it.validation()
            ValidationResult.Valid
        }
        validate<WishListRequest> {
            it.validation()
            ValidationResult.Valid
        }
        validate<PaymentRequest> {
            it.validation()
            ValidationResult.Valid
        }
    }
}
