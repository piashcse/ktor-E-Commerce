# Ktor E-Commerce — Development Roadmap

> A phased, implementation-focused roadmap for building out the Ktor e-commerce platform. Use this document to guide AI-assisted development. Each phase is self-contained and builds on the previous one.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Current Architecture](#current-architecture)
- [Phase 1: Critical Bugs & Security Fixes](#phase-1-critical-bugs--security-fixes)
- [Phase 2: Missing Core E-Commerce Logic](#phase-2-missing-core-e-commerce-logic)
- [Phase 3: Validation & API Design](#phase-3-validation--api-design)
- [Phase 4: Database Schema Fixes](#phase-4-database-schema-fixes)
- [Phase 5: Architecture & Code Quality](#phase-5-architecture--code-quality)
- [Implementation Checklist](#implementation-checklist)

---

## Project Overview

**Stack:** Ktor 3.4.1 · Kotlin 2.3.10 · Exposed 1.1.1 · PostgreSQL · Koin 4.2.0 · JWT (auth0) · BCrypt · Flyway · Valiktor · Gson

**Architecture:** Clean/Onion — feature modules with Routes → Service → Repository layers

**Entry Point:** `src/main/kotlin/com/piashcse/Application.kt`

---

## Current Architecture

```
com/piashcse/
├── Application.kt
├── config/
│   └── DotEnvConfig.kt
├── constants/
│   ├── AppConstants.kt
│   ├── Enums.kt              # OrderStatus, PaymentStatus, ProductStatus, ShopStatus, InventoryStatus, UserType
│   └── Message.kt
├── database/
│   ├── ConfigureDataBase.kt
│   └── entities/             # Exposed DAO tables (18 tables)
├── di/
│   └── KoinModule.kt         # Koin DI registration
├── feature/                  # Feature modules (Routes + Service + Repository)
│   ├── auth/
│   ├── brand/
│   ├── cart/
│   ├── consent/
│   ├── inventory/
│   ├── order/
│   ├── payment/
│   ├── policy/
│   ├── product/
│   ├── product_category/
│   ├── product_sub_category/
│   ├── profile/
│   ├── review_rating/
│   ├── shipping/
│   ├── shop/
│   ├── shop_category/
│   └── wishlist/
├── model/
│   ├── request/              # 26 request DTOs
│   └── response/             # Response DTOs
├── plugin/
│   ├── ConfigureBasic.kt     # CORS, ContentNegotiation (Gson), CallLogging
│   ├── ConfigureRouting.kt   # Route registration
│   ├── ConfigureSwagger.kt
│   ├── ConfigureStatusPage.kt
│   └── CongfigureAuth.kt     # JWT auth install
└── utils/
    ├── Utils.kt              # Email, OTP
    ├── RoleBasedAuth.kt      # Role hierarchy
    └── extension/CommonExtension.kt
```

### Current Endpoints Summary

| Feature | Endpoints | Auth |
|---------|-----------|------|
| Auth | 9 endpoints (login, register, OTP, forget/reset password, change-password, change-user-type, activate/deactivate) | Mix |
| Profile | 3 endpoints (get, update, image-upload) | Any authenticated |
| Shop | 11 endpoints (CRUD, approve/reject/suspend/activate, public listing) | Seller/Admin/Customer |
| Product | 10 endpoints (CRUD, search, filter, image-upload) | Seller/Admin/Public |
| Cart | 5 endpoints (add, get, update, remove, clear) | Customer |
| Wishlist | 4 endpoints (add, get, remove, check) | Customer |
| Order | 3 endpoints (create, get, status update) | Customer/Seller/Admin |
| Payment | 2 endpoints (create, get by id) | Customer |
| Shipping | 4 endpoints (CRUD) | Customer |
| Inventory | 5 endpoints (CRUD, stock operation, low-stock) | Seller/Admin |
| Review/Rating | 4 endpoints (CRUD) | Customer/Public |
| Policy | 5 endpoints | Admin/Public |
| Policy Consent | 2 endpoints | Customer/Admin |
| Brand | 4 endpoints | Admin/Customer |
| Product Category | 4 endpoints | Admin/Public |
| Product SubCategory | 4 endpoints | Admin/Public |
| Shop Category | 4 endpoints | Admin |

---

## Phase 1: Critical Bugs & Security Fixes

> **Goal:** Fix confirmed runtime bugs and security vulnerabilities. These cause data corruption, security risks, or crashes.

---

### 1.1 Fix `EntityID` Table References in ProductService

**File:** `src/main/kotlin/com/piashcse/feature/product/ProductService.kt`

**Problem:** All `EntityID` constructors use `ProductTable` as the reference table, even for fields that reference other tables (userId, categoryId, subCategoryId, brandId, shopId). This causes FK lookups to fail or return wrong data.

**Current code (lines ~55-59 in `createProduct`):**
```kotlin
userId = EntityID(userId, ProductTable),
categoryId = EntityID(productRequest.categoryId, ProductTable),
subCategoryId = productRequest.subCategoryId?.let { EntityID(it, ProductTable) },
brandId = productRequest.brandId?.let { EntityID(it, ProductTable) },
shopId = shopId?.let { EntityID(it, ProductTable) },
```

**Fix:**
```kotlin
import com.piashcse.database.entities.User.UserTable
import com.piashcse.database.entities.ProductCategory.ProductCategoryTable
import com.piashcse.database.entities.ProductSubCategory.ProductSubCategoryTable
import com.piashcse.database.entities.Brand.BrandTable
import com.piashcse.database.entities.Shop.ShopTable

// In createProduct:
userId = EntityID(userId, UserTable),
categoryId = EntityID(productRequest.categoryId, ProductCategoryTable),
subCategoryId = productRequest.subCategoryId?.let { EntityID(it, ProductSubCategoryTable) },
brandId = productRequest.brandId?.let { EntityID(it, BrandTable) },
shopId = shopId?.let { EntityID(it, ShopTable) },

// In updateProduct (lines ~140-145):
productRequest.categoryId?.let { EntityID(it, ProductCategoryTable) },
productRequest.subCategoryId?.let { EntityID(it, ProductSubCategoryTable) },
productRequest.brandId?.let { EntityID(it, BrandTable) },
productRequest.shopId?.let { EntityID(it, ShopTable) },
```

**Verify:** Check imports at the top of ProductService.kt and update all EntityID usages.

---

### 1.2 Fix Duplicate DELETE Route in ProductRoutes

**File:** `src/main/kotlin/com/piashcse/feature/product/ProductRoutes.kt`

**Problem:** `DELETE /product/{id}` is registered twice — once in the SELLER block (line ~177) and once in the ADMIN block (line ~193). Ktor resolves the first matching route, so the admin handler is unreachable.

**Current structure:**
```kotlin
authenticate(RoleManagement.SELLER.role) {
    delete("{id}") { /* seller delete handler */ }
}
authenticate(RoleManagement.ADMIN.role) {
    delete("{id}") { /* admin delete handler — NEVER REACHED */ }
}
```

**Fix:** Merge into a single handler that checks ownership, OR move admin routes to a separate `/admin` namespace.

**Option A — Single handler with ownership check:**
```kotlin
authenticate(RoleManagement.SELLER.role, RoleManagement.ADMIN.role, RoleManagement.SUPER_ADMIN.role) {
    delete("{id}") {
        val id = call.parameters["id"] ?: return@delete call.requiredParameters("Product ID is required")
        val userId = call.currentUserId() ?: return@delete call.respond(
            HttpStatusCode.Unauthorized, ApiResponse.failure("Unauthorized")
        )
        val userType = call.getUserType() // helper to get current user type

        val result = if (userType == UserType.ADMIN || userType == UserType.SUPER_ADMIN) {
            productService.deleteProductByAdmin(id)
        } else {
            productService.deleteProduct(id, userId)
        }

        result.fold(
            onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse.success("Product deleted successfully")) },
            onFailure = { call.respond(HttpStatusCode.BadRequest, ApiResponse.failure(it.message ?: "Failed to delete product")) }
        )
    }
}
```

**Option B — Separate admin namespace (preferred for Phase 5):**
Keep seller routes as-is, add admin routes under `/admin/products/{id}`.

---

### 1.3 Fix `searchProduct` Memory Explosion

**File:** `src/main/kotlin/com/piashcse/feature/product/ProductService.kt`

**Problem:** `searchProduct` (lines ~291-304) calls `ProductDAO.all().toList()` which loads every product into memory, then filters in Kotlin.

**Current code:**
```kotlin
fun searchProduct(productQuery: ProductSearchRequest): List<ProductDAO> {
    var products = ProductDAO.all().toList()
    if (!productQuery.name.isNullOrEmpty()) {
        products = products.filter { it.name.contains(productQuery.name, ignoreCase = true) }
    }
    // ... more filtering
    return products
}
```

**Fix — Use SQL-level filtering:**
```kotlin
fun searchProduct(productQuery: ProductSearchRequest): List<ProductDAO> {
    val query = Op.build {
        ProductTable.status eq ProductStatus.ACTIVATED.name
    }.let { baseWhere ->
        var where = baseWhere
        if (!productQuery.name.isNullOrEmpty()) {
            where = where and ProductTable.name.lowerCase() like "%${productQuery.name.lowercase()}%"
        }
        if (!productQuery.categoryId.isNullOrEmpty()) {
            where = where and (ProductTable.categoryId eq EntityID(productQuery.categoryId, ProductCategoryTable))
        }
        if (productQuery.minPrice != null) {
            where = where and (ProductTable.price greaterEq productQuery.minPrice)
        }
        if (productQuery.maxPrice != null) {
            where = where and (ProductTable.price lessEq productQuery.maxPrice)
        }
        where
    }

    return ProductDAO.find { query }
        .limit(productQuery.limit ?: 20)
        .toList()
}
```

---

### 1.4 Fix `adjustWhere` Filter Logic Bug

**File:** `src/main/kotlin/com/piashcse/feature/product/ProductService.kt`

**Problem:** `adjustWhere` **replaces** the existing WHERE clause. When multiple filters are provided (e.g., categoryId AND brandId), only the last one applies.

**Current code (in `getProducts`):**
```kotlin
var query = ProductTable.selectAll()
if (categoryId != null) {
    query = query.adjustWhere { ProductTable.categoryId eq EntityID(categoryId, ProductTable) } // REPLACED next
}
if (subCategoryId != null) {
    query = query.adjustWhere { ProductTable.subCategoryId eq EntityID(subCategoryId, ProductTable) } // REPLACED next
}
if (brandId != null) {
    query = query.adjustWhere { ProductTable.brandId eq EntityID(brandId, ProductTable) } // Only this survives
}
```

**Fix — Build WHERE clause incrementally with `andWhere`:**
```kotlin
fun getProducts(
    limit: Int,
    maxPrice: Long?,
    minPrice: Long?,
    categoryId: String?,
    subCategoryId: String?,
    brandId: String?,
    offset: Int = 0
): List<ProductWithShopName> {
    var condition: Op<Boolean> = ProductTable.status eq ProductStatus.ACTIVATED.name

    if (categoryId != null) {
        condition = condition and (ProductTable.categoryId eq EntityID(categoryId, ProductCategoryTable))
    }
    if (subCategoryId != null) {
        condition = condition and (ProductTable.subCategoryId eq EntityID(subCategoryId, ProductSubCategoryTable))
    }
    if (brandId != null) {
        condition = condition and (ProductTable.brandId eq EntityID(brandId, BrandTable))
    }
    if (minPrice != null) {
        condition = condition and (ProductTable.price greaterEq minPrice)
    }
    if (maxPrice != null) {
        condition = condition and (ProductTable.price lessEq maxPrice)
    }

    return transaction {
        ProductDAO.find { condition }
            .limit(limit, offset.toLong())
            .map { /* mapping logic */ }
    }
}
```

---

### 1.5 Fix `getShops` Memory Explosion

**File:** `src/main/kotlin/com/piashcse/feature/shop/ShopService.kt`

**Problem:** `getShops` (lines ~120-135) calls `ShopDAO.all()` then filters in memory.

**Fix:**
```kotlin
fun getShops(status: ShopStatus?, limit: Int, offset: Int = 0): List<ShopDAO> {
    val condition = if (status != null) {
        ShopTable.status eq status.name
    } else {
        Op.TRUE
    }
    return ShopDAO.find { condition }
        .limit(limit, offset.toLong())
        .toList()
}
```

---

### 1.6 Fix `stockQuantity` Defaulting to 0 in Update

**File:** `src/main/kotlin/com/piashcse/feature/product/ProductRoutes.kt`

**Problem:** Line ~151: `stockQuantity = call.parameters["stockQuantity"]?.toInt() ?: 0` — omitting stockQuantity sets it to 0, wiping stock data.

**Fix:**
```kotlin
stockQuantity = call.parameters["stockQuantity"]?.toInt(), // nullable, let service handle default
```

Then in `ProductService.updateProduct`:
```kotlin
stockQuantity?.let { product.stockQuantity = it } // only update if provided
```

---

### 1.7 Fix Image Upload Null Cast & Missing Response

**File:** `src/main/kotlin/com/piashcse/feature/product/ProductRoutes.kt`

**Problem:**
1. `part.originalFileName as String` crashes when filename is null
2. No response sent when no file part is received
3. No file type validation

**Fix:**
```kotlin
post("image-upload") {
    val multipart = call.receiveMultipart()
    val uploadedFiles = mutableListOf<String>()

    multipart.forEachPart { part ->
        if (part is PartData.FileItem) {
            val fileName = part.originalFileName
                ?.takeIf { it.isNotBlank() }
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse.failure("File name is required"))
                    return@forEachPart
                }

            val extension = fileName.substringAfterLast('.', "").lowercase()
            if (extension !in listOf("jpg", "jpeg", "png", "webp", "gif")) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.failure("Invalid file type. Allowed: jpg, jpeg, png, webp, gif"))
                return@forEachPart
            }

            val uniqueFileName = "${UUID.randomUUID()}.$extension"
            val file = File("$PRODUCT_IMAGE_FOLDER/$uniqueFileName")
            file.writeBytes(part.streamProvider().readBytes())
            uploadedFiles.add(uniqueFileName)
        }
        part.dispose()
    }

    if (uploadedFiles.isEmpty()) {
        call.respond(HttpStatusCode.BadRequest, ApiResponse.failure("No file uploaded"))
    } else {
        call.respond(HttpStatusCode.OK, ApiResponse.success(uploadedFiles, "Image(s) uploaded successfully"))
    }
}
```

Also add `maxContentLength` to application.conf:
```hocon
ktor {
    deployment {
        maxContentLength = 10485760  // 10MB
    }
}
```

---

### 1.8 Fix Inventory Concurrency Race Condition

**File:** `src/main/kotlin/com/piashcse/feature/inventory/InventoryService.kt`

**Problem:** READ → CALC → WRITE pattern causes lost updates under concurrent requests.

**Current code:**
```kotlin
val newStock = when(operation) {
    "add" -> inventory.stockQuantity + quantity
    "subtract" -> inventory.stockQuantity - quantity
    "set" -> quantity
}
inventory.stockQuantity = newStock
```

**Fix — Use atomic SQL:**
```kotlin
fun updateStock(entityId: EntityID<String>, quantity: Int, operation: String, userId: String): Result<Int> = query {
    try {
        val inventory = InventoryDAO.findById(entityId)
            ?: return@query Result.failure("Inventory not found".inventoryNotFoundException())

        // Verify ownership — seller must own the product
        val product = ProductDAO.findById(inventory.productId.value)
            ?: return@query Result.failure("Product not found".notFoundException())

        if (product.userId.value != userId) {
            return@query Result.failure("Unauthorized: You don't own this product".unauthorizedAccessException())
        }

        when (operation) {
            "add" -> {
                require(quantity > 0) { "Quantity must be positive for add operation" }
                InventoryTable.update({ InventoryTable.id eq entityId }) {
                    it[stockQuantity] = InventoryTable.stockQuantity.plus(quantity)
                }
            }
            "subtract" -> {
                require(quantity > 0) { "Quantity must be positive for subtract operation" }
                val rowsUpdated = InventoryTable.update({
                    (InventoryTable.id eq entityId) and (InventoryTable.stockQuantity greaterEq quantity)
                }) {
                    it[stockQuantity] = InventoryTable.stockQuantity.minus(quantity)
                }
                if (rowsUpdated == 0) {
                    return@query Result.failure("Insufficient stock quantity")
                }
            }
            "set" -> {
                require(quantity >= 0) { "Quantity cannot be negative for set operation" }
                InventoryTable.update({ InventoryTable.id eq entityId }) {
                    it[stockQuantity] = quantity
                }
            }
            else -> return@query Result.failure("Invalid operation: $operation. Use add, subtract, or set")
        }

        // Update status based on new stock level
        val updated = InventoryDAO.findById(entityId) ?: return@query Result.failure("Failed to fetch updated inventory")
        updated.status = when {
            updated.stockQuantity == 0 -> InventoryStatus.OUT_OF_STOCK.name
            updated.stockQuantity <= (updated.minimumStockLevel ?: 10) -> InventoryStatus.LOW_STOCK.name
            else -> InventoryStatus.IN_STOCK.name
        }

        Result.success(updated.stockQuantity)
    } catch (e: Exception) {
        Result.failure(e.message ?: "Failed to update stock")
    }
}
```

---

### 1.9 Add Refresh Token System

**New table:** `refresh_token`
```sql
CREATE TABLE IF NOT EXISTS refresh_token (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL REFERENCES "user"(id),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);
CREATE INDEX idx_refresh_token_hash ON refresh_token(token_hash);
```

**New entity:** `src/main/kotlin/com/piashcse/database/entities/RefreshToken.kt`
```kotlin
package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import java.time.Instant
import java.util.UUID

object RefreshTokenTable : BaseIdTable("refresh_token") {
    val userId = varchar("user_id", 50).references(User.UserTable.id)
    val tokenHash = varchar("token_hash", 255).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val revokedAt = timestamp("revoked_at").nullable()
}

class RefreshToken(id: EntityID<String>) : BaseEntity(id) {
    companion object : BaseEntityClass<RefreshToken>(RefreshTokenTable)

    var userId by RefreshTokenTable.userId
    var tokenHash by RefreshTokenTable.tokenHash
    var expiresAt by RefreshTokenTable.expiresAt
    var revokedAt by RefreshTokenTable.revokedAt

    val isExpired: Boolean get() = expiresAt.isBefore(Instant.now())
    val isRevoked: Boolean get() = revokedAt != null
    val isValid: Boolean get() = !isExpired && !isRevoked
}
```

**New repository interface:** `src/main/kotlin/com/piashcse/feature/auth/RefreshTokenRepository.kt`
```kotlin
package com.piashcse.feature.auth

import org.jetbrains.exposed.dao.id.EntityID
import java.time.Instant

interface RefreshTokenRepository {
    suspend fun createRefreshToken(userId: String, tokenHash: String, expiresAt: Instant): Boolean
    suspend fun validateRefreshToken(tokenHash: String): Boolean
    suspend fun revokeRefreshToken(tokenHash: String): Boolean
    suspend fun revokeAllUserTokens(userId: String): Boolean
    suspend fun getRefreshToken(tokenHash: String): com.piashcse.database.entities.RefreshToken?
    suspend fun cleanupExpiredTokens(): Int
}
```

**New service methods in AuthService:**
```kotlin
// Generate refresh token
fun generateTokens(userId: String, email: String, userType: String): Pair<String, String> {
    val accessToken = JwtConfig.tokenProvider(JwtTokenRequest(userId, email, userType))
    val refreshToken = UUID.randomUUID().toString()
    val tokenHash = hashRefreshToken(refreshToken)
    val expiresAt = Instant.now().plusSeconds(7L * 24 * 60 * 60) // 7 days

    refreshTokenRepository.createRefreshToken(userId, tokenHash, expiresAt)

    return accessToken to refreshToken
}

fun hashRefreshToken(token: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
}

// Refresh token endpoint
fun refreshAccessToken(refreshToken: String): Result<TokenPair> {
    val tokenHash = hashRefreshToken(refreshToken)
    val storedToken = refreshTokenRepository.getRefreshToken(tokenHash)
        ?: return Result.failure("Invalid refresh token".notFoundException())

    if (!storedToken.isValid) {
        refreshTokenRepository.revokeRefreshToken(tokenHash)
        return Result.failure("Refresh token expired or revoked".unauthorizedAccessException())
    }

    val user = userRepository.getUserById(storedToken.userId.value)
        ?: return Result.failure("User not found".notFoundException())

    // Revoke old token, issue new pair
    refreshTokenRepository.revokeRefreshToken(tokenHash)
    val (newAccessToken, newRefreshToken) = generateTokens(user.id.value, user.email, user.userType)

    return Result.success(TokenPair(newAccessToken, newRefreshToken))
}
```

**New endpoints in AuthRoutes:**
```kotlin
// POST /auth/refresh-token
post("refresh-token") {
    val request = call.receive<RefreshTokenRequest>()
    request.refreshToken.validation()

    authService.refreshAccessToken(request.refreshToken).fold(
        onSuccess = { tokenPair ->
            call.respond(HttpStatusCode.OK, ApiResponse.success(tokenPair, "Token refreshed successfully"))
        },
        onFailure = {
            call.respond(HttpStatusCode.Unauthorized, ApiResponse.failure(it.message ?: "Invalid refresh token"))
        }
    )
}

// POST /auth/logout
post("logout") {
    val userId = call.currentUserId() ?: return@post call.respond(
        HttpStatusCode.Unauthorized, ApiResponse.failure("Unauthorized")
    )
    val request = call.receive<LogoutRequest>()

    if (request.refreshToken.isNotBlank()) {
        authService.logout(userId, request.refreshToken)
    }

    call.respond(HttpStatusCode.OK, ApiResponse.success("Logged out successfully"))
}
```

**New request models:**
```kotlin
// src/main/kotlin/com/piashcse/model/request/RefreshTokenRequest.kt
data class RefreshTokenRequest(val refreshToken: String) {
    fun RefreshTokenRequest.validation() {
        validate(this) {
            validate(RefreshTokenRequest::refreshToken).isRequired()
        }
    }
}

// src/main/kotlin/com/piashcse/model/request/LogoutRequest.kt
data class LogoutRequest(val refreshToken: String = "")
```

**Update login/register to return refresh token:**
```kotlin
// In AuthService.login and AuthService.register:
// Change from: Result.success(JwtTokenRequest(...))
// Change to:
val (accessToken, refreshToken) = generateTokens(user.id.value, user.email, user.userType)
Result.success(TokenPair(accessToken, refreshToken))

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 86400 // 24 hours in seconds
)
```

---

### 1.10 Add Rate Limiting on Auth Endpoints

**Add dependency to `build.gradle.kts`:**
```kotlin
implementation(libs.ktor.server.rate.limit)
```

**Create plugin:** `src/main/kotlin/com/piashcse/plugin/ConfigureRateLimit.kt`
```kotlin
package com.piashcse.plugin

import io.ktor.server.application.*
import io.ktor.server.rate.limit.*
import io.ktor.http.*

fun Application.configureRateLimiting() {
    install(RateLimit) {
        register(RateLimitName("auth")) {
            rateLimiter(limit = 5, refillPeriod = java.time.Duration.ofMinutes(10))
        }
        register(RateLimitName("general")) {
            rateLimiter(limit = 100, refillPeriod = java.time.Duration.ofMinutes(1))
        }
        requestKey { call ->
            call.request.origin.remoteAddress
        }
        modifyResponse { call, rateLimiter ->
            call.response.header("X-RateLimit-Remaining", rateLimiter.remaining.toString())
        }
    }
}
```

**Apply to auth routes:**
```kotlin
rateLimit(RateLimitName("auth")) {
    post("login") { ... }
    post("register") { ... }
    post("forget-password") { ... }
    post("reset-password") { ... }
}
```

---

### 1.11 Convert Password Reset to POST Body

**Current:** `GET /auth/forget-password?email=&userType=`

**Fix:** Change to POST with JSON body.

**New endpoint:**
```kotlin
// POST /auth/forget-password
post("forget-password") {
    val request = call.receive<ForgetPasswordRequest>()
    request.validation()

    authService.requestPasswordReset(request.email, request.userType).fold(
        onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse.success("OTP sent to your email")) },
        onFailure = { call.respond(HttpStatusCode.BadRequest, ApiResponse.failure(it.message ?: "Failed to request password reset")) }
    )
}

// POST /auth/reset-password
post("reset-password") {
    val request = call.receive<ResetPasswordRequest>()
    request.validation()

    authService.resetPassword(request.email, request.otp, request.newPassword, request.userType).fold(
        onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse.success("Password reset successfully")) },
        onFailure = { call.respond(HttpStatusCode.BadRequest, ApiResponse.failure(it.message ?: "Failed to reset password")) }
    )
}
```

**New request models:**
```kotlin
data class ForgetPasswordRequest(
    val email: String,
    val userType: String
) {
    fun ForgetPasswordRequest.validation() {
        validate(this) {
            validate(ForgetPasswordRequest::email).isRequired().hasValidEmailFormat()
            validate(ForgetPasswordRequest::userType).isRequired()
        }
    }
}

data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val newPassword: String,
    val userType: String
) {
    fun ResetPasswordRequest.validation() {
        validate(this) {
            validate(ResetPasswordRequest::email).isRequired()
            validate(ResetPasswordRequest::otp).isRequired()
            validate(ResetPasswordRequest::newPassword).isRequired().hasMinSize(8)
            validate(ResetPasswordRequest::userType).isRequired()
        }
    }
}
```

---

### 1.12 Add Password Strength Validation

**File:** `src/main/kotlin/com/piashcse/feature/auth/AuthService.kt`

Add validation in `register` and `resetPassword`:
```kotlin
private fun validatePasswordStrength(password: String) {
    if (password.length < 8) {
        throw "Password must be at least 8 characters long".validationException()
    }
    if (!password.any { it.isUpperCase() }) {
        throw "Password must contain at least one uppercase letter".validationException()
    }
    if (!password.any { it.isLowerCase() }) {
        throw "Password must contain at least one lowercase letter".validationException()
    }
    if (!password.any { it.isDigit() }) {
        throw "Password must contain at least one digit".validationException()
    }
    if (!password.any { !it.isLetterOrDigit() }) {
        throw "Password must contain at least one special character".validationException()
    }
}
```

Call it in `register()` and `resetPassword()` before hashing.

---

### 1.13 Add Login Attempt Tracking

**New table:**
```sql
CREATE TABLE IF NOT EXISTS login_attempt (
    id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    attempt_count INT DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(email, user_type)
);
```

**Logic in AuthService.login:**
```kotlin
fun login(loginRequest: LoginRequest, ipAddress: String): Result<TokenPair> {
    // Check if account is locked
    val attempt = loginAttemptRepository.getAttempt(loginRequest.email, loginRequest.userType)
    if (attempt?.lockedUntil?.isAfter(Instant.now()) == true) {
        return Result.failure("Account locked. Try again later".unauthorizedAccessException())
    }

    val user = userRepository.getUserByEmailAndType(loginRequest.email, loginRequest.userType)
        ?: return Result.failure("Invalid email or password".notFoundException()).also {
            loginAttemptRepository.recordFailedAttempt(loginRequest.email, loginRequest.userType, ipAddress)
        }

    if (!BCrypt.verify(loginRequest.password, user.password)) {
        loginAttemptRepository.recordFailedAttempt(loginRequest.email, loginRequest.userType, ipAddress)
        val attempts = loginAttemptRepository.getAttemptCount(loginRequest.email, loginRequest.userType)
        val remaining = 5 - attempts
        if (attempts >= 5) {
            loginAttemptRepository.lockAccount(loginRequest.email, loginRequest.userType, Duration.ofMinutes(30))
            return Result.failure("Account locked due to too many failed attempts. Try again in 30 minutes".unauthorizedAccessException())
        }
        return Result.failure("Invalid email or password. $remaining attempts remaining".notFoundException())
    }

    // Success — reset attempts
    loginAttemptRepository.resetAttempts(loginRequest.email, loginRequest.userType)
    user.lastLoginAt = Instant.now()

    val (accessToken, refreshToken) = generateTokens(user.id.value, user.email, user.userType)
    return Result.success(TokenPair(accessToken, refreshToken))
}
```

---

### 1.14 Fix CORS Configuration

**File:** `src/main/kotlin/com/piashcse/plugin/ConfigureBasic.kt`

**Current:** Likely `anyHost = true` (wildcard `*`)

**Fix:**
```kotlin
install(CORS) {
    val allowedOrigins = DotEnvConfig.allowedOrigins.split(",")
    allowedOrigins.forEach { origin ->
        host(origin.trim().removePrefix("https://").removePrefix("http://"))
    }
    allowCredentials = true
    allowNonSimpleContentTypes = true
    listOf(
        HttpMethod.Put,
        HttpMethod.Post,
        HttpMethod.Delete,
        HttpMethod.Patch,
        HttpMethod.Options
    ).forEach { allowMethod(it) }
    allowHeader(HttpMethod.Headers.ContentType)
    allowHeader(HttpMethod.Headers.Authorization)
    allowHeader("X-Requested-With")
    exposeHeader("X-Request-ID")
    maxAge = Duration.ofDays(1)
}
```

**Add to `.env`:**
```
ALLOWED_ORIGINS=http://localhost:3000,https://yourdomain.com
```

---

### Phase 1 Completion Checklist

```
[ ] 1.1  EntityID table references fixed in ProductService
[ ] 1.2  Duplicate DELETE route fixed in ProductRoutes
[ ] 1.3  searchProduct uses SQL-level filtering
[ ] 1.4  adjustWhere replaced with proper andWhere chain
[ ] 1.5  getShops uses SQL-level filtering
[ ] 1.6  stockQuantity no longer defaults to 0
[ ] 1.7  Image upload null-safe with file type validation
[ ] 1.8  Inventory concurrency fixed with atomic SQL
[ ] 1.9  Refresh token system implemented (table + entity + endpoints)
[ ] 1.10 Rate limiting on auth endpoints
[ ] 1.11 Password reset uses POST with body
[ ] 1.12 Password strength validation
[ ] 1.13 Login attempt tracking + lockout
[ ] 1.14 CORS restricted to configured origins
```

---

## Phase 2: Missing Core E-Commerce Logic

> **Goal:** Implement essential business logic that is missing from the current implementation.

---

### 2.1 Add Cart Summary Endpoint

**New endpoint:** `GET /cart/summary`

**New response model:**
```kotlin
data class CartSummaryResponse(
    val items: List<CartItemResponse>,
    val subtotal: Long,
    val estimatedTax: Long,
    val itemCount: Int
)

data class CartItemResponse(
    val productId: String,
    val productName: String,
    val price: Long,
    val quantity: Int,
    val image: String?,
    val stockQuantity: Int,
    val shopId: String?,
    val shopName: String?
)
```

**Service method:**
```kotlin
fun getCartSummary(userId: String): Result<CartSummaryResponse> = query {
    val cartItems = CartItemDAO.find { CartItemTable.userId eq EntityID(userId, UserTable) }.toList()

    val items = cartItems.mapNotNull { cartItem ->
        val product = ProductDAO.findById(cartItem.productId.value) ?: return@mapNotNull null
        val shop = product.shopId?.value?.let { ShopDAO.findById(it) }

        CartItemResponse(
            productId = product.id.value,
            productName = product.name,
            price = product.discountPrice ?: product.price,
            quantity = cartItem.quantity,
            image = product.images?.firstOrNull(),
            stockQuantity = product.stockQuantity,
            shopId = shop?.id?.value,
            shopName = shop?.name
        )
    }

    val subtotal = items.sumOf { it.price * it.quantity }
    val estimatedTax = (subtotal * 0.1).toLong() // Configurable tax rate

    Result.success(CartSummaryResponse(items, subtotal, estimatedTax, items.size))
}
```

---

### 2.2 Add Stock & Price Validation at Checkout

**File:** `src/main/kotlin/com/piashcse/feature/order/OrderService.kt`

**Problem:** `createOrder` currently uses client-provided prices and does not check stock.

**Fix in `createOrder`:**
```kotlin
fun createOrder(orderRequest: OrderRequest, userId: String): Result<OrderResponse> = query {
    // Validate each product in the order
    val validatedItems = mutableListOf<OrderItemData>()
    var calculatedSubtotal = 0L

    for (item in orderRequest.items) {
        val product = ProductDAO.findById(EntityID(item.productId, ProductTable))
            ?: return@query Result.failure("Product ${item.productId} not found".notFoundException())

        // Validate stock
        if (product.stockQuantity < item.quantity) {
            return@query Result.failure("Product ${product.name} has only ${product.stockQuantity} in stock".validationException())
        }

        // Use DB price, not client price
        val unitPrice = product.discountPrice ?: product.price
        val itemTotal = unitPrice * item.quantity
        calculatedSubtotal += itemTotal

        validatedItems.add(OrderItemData(product, item.quantity, unitPrice, itemTotal))
    }

    // Validate total matches
    if (orderRequest.total != calculatedSubtotal + orderRequest.shippingCost + orderRequest.taxAmount - orderRequest.discountAmount) {
        return@query Result.failure("Order total mismatch. Expected: $calculatedSubtotal".validationException())
    }

    // ... proceed with order creation using validatedItems
}
```

---

### 2.3 Clear Cart After Successful Order

**In OrderService.createOrder, after order is persisted:**
```kotlin
// Clear cart items for this user
CartItemDAO.find { CartItemTable.userId eq EntityID(userId, UserTable) }.forEach { it.delete() }
```

---

### 2.4 Add Idempotency Key to Orders

**Add column to `order` table:**
```sql
ALTER TABLE "order" ADD COLUMN idempotency_key VARCHAR(100) UNIQUE;
CREATE INDEX idx_order_idempotency ON "order"(idempotency_key);
```

**In OrderRoutes:**
```kotlin
post {
    val idempotencyKey = call.request.header("Idempotency-Key")
    val orderRequest = call.receive<OrderRequest>()

    // Check for duplicate
    if (idempotencyKey != null) {
        val existing = orderRepository.getOrderIdempotencyKey(idempotencyKey)
        if (existing != null) {
            return@post call.respond(HttpStatusCode.OK, ApiResponse.success(existing, "Order already processed"))
        }
    }

    val result = orderService.createOrder(orderRequest, userId, idempotencyKey)
    // ...
}
```

---

### 2.5 Generate Human-Readable Order Numbers

**Current:** UUID-based order numbers

**Fix:** Use a sequence-based format: `ORD-YYYYMMDD-XXXX`

**In OrderService.createOrder:**
```kotlin
val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) // 20260411
val count = orderRepository.getOrderCountForDate(today) + 1
val orderNumber = "ORD-$today-${count.toString().padStart(4, '0')}"
```

**Add repository method:**
```kotlin
suspend fun getOrderCountForDate(datePrefix: String): Int = query {
    OrderDAO.find {
        OrderTable.orderNumber like "ORD-$datePrefix%"
    }.count().toInt()
}
```

---

### 2.6 Add Order Cancellation

**New endpoint:** `POST /order/{id}/cancel`

**New request model:**
```kotlin
data class CancelOrderRequest(val reason: String) {
    fun CancelOrderRequest.validation() {
        validate(this) {
            validate(CancelOrderRequest::reason).isRequired().hasMaxLength(500)
        }
    }
}
```

**Service method:**
```kotlin
fun cancelOrder(orderId: String, userId: String, reason: String, userType: UserType): Result<OrderResponse> = query {
    val order = OrderDAO.findById(EntityID(orderId, OrderTable))
        ?: return@query Result.failure("Order not found".notFoundException())

    // Permission check
    if (userType == UserType.CUSTOMER && order.userId.value != userId) {
        return@query Result.failure("Unauthorized".unauthorizedAccessException())
    }

    // Status check — only PENDING or CONFIRMED can be cancelled
    if (order.status !in listOf(OrderStatus.PENDING.name, OrderStatus.CONFIRMED.name)) {
        return@query Result.failure("Order cannot be cancelled in ${order.status} status".validationException())
    }

    val oldStatus = order.status
    order.status = OrderStatus.CANCELED.name
    order.canceledDate = Instant.now()
    order.notes = reason

    // Record status history
    OrderStatusHistory.new {
        this.orderId = order.id
        this.oldStatus = oldStatus
        this.newStatus = OrderStatus.CANCELED.name
        this.changedBy = EntityID(userId, UserTable)
        this.notes = reason
    }

    // Restore stock
    OrderItemDAO.find { OrderItemTable.orderId eq order.id }.forEach { orderItem ->
        ProductDAO.findById(orderItem.productId.value)?.let { product ->
            product.stockQuantity += orderItem.quantity
        }
    }

    Result.success(order.toResponse())
}
```

---

### 2.7 Add Return/Refund Request Flow

**New tables:**
```sql
CREATE TABLE IF NOT EXISTS return_request (
    id VARCHAR(50) PRIMARY KEY,
    order_item_id VARCHAR(50) NOT NULL REFERENCES order_item(id),
    user_id VARCHAR(50) NOT NULL REFERENCES "user"(id),
    reason TEXT NOT NULL,
    images JSONB,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, REFUNDED, SHIPPED
    refund_amount BIGINT,
    refund_method VARCHAR(50),
    tracking_number VARCHAR(100),
    requested_at TIMESTAMP DEFAULT NOW(),
    resolved_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_return_request_order_item ON return_request(order_item_id);
CREATE INDEX idx_return_request_user ON return_request(user_id);
CREATE INDEX idx_return_request_status ON return_request(status);
```

**New enum in `Enums.kt`:**
```kotlin
enum class ReturnStatus {
    PENDING, APPROVED, REJECTED, REFUNDED, SHIPPED
}
```

**Endpoints:**
```
POST   /order/{orderId}/return          # Customer: request return for order
GET    /order/{orderId}/returns          # Customer/Seller: list returns for order
GET    /returns/{returnId}               # Get return detail
PUT    /returns/{returnId}/status        # Seller/Admin: approve/reject return
POST   /returns/{returnId}/ship          # Customer: mark as shipped (with tracking)
```

---

### 2.8 Add Seller Order Listing

**New endpoint:** `GET /order/seller`

**Service method:**
```kotlin
fun getSellerOrders(userId: String, limit: Int, offset: Int, status: String?): Result<List<OrderResponse>> = query {
    val seller = SellerDAO.find { SellerTable.userId eq EntityID(userId, UserTable) }.firstOrNull()
        ?: return@query Result.failure("Seller profile not found".notFoundException())

    val shopId = seller.shopId ?: return@query Result.failure("No shop associated with seller".notFoundException())

    val condition = OrderTable.shopId eq shopId
    val finalCondition = if (status != null) {
        condition and (OrderTable.status eq status)
    } else condition

    val orders = OrderDAO.find { finalCondition }
        .orderBy(OrderTable.createdAt to SortOrder.DESC)
        .limit(limit, offset.toLong())
        .toList()

    Result.success(orders.map { it.toResponse() })
}
```

---

### 2.9 Add Admin Order Listing

**New endpoint:** `GET /order/admin?status=&startDate=&endDate=&page=&limit=`

**Service method:**
```kotlin
fun getAdminOrders(
    limit: Int,
    offset: Int,
    status: String?,
    startDate: Instant?,
    endDate: Instant?
): Result<Pair<List<OrderResponse>, Long>> = query {
    var condition: Op<Boolean> = Op.TRUE

    status?.let { condition = condition and (OrderTable.status eq it) }
    startDate?.let { condition = condition and (OrderTable.createdAt greaterEq it) }
    endDate?.let { condition = condition and (OrderTable.createdAt lessEq it) }

    val totalCount = OrderDAO.find { condition }.count()
    val orders = OrderDAO.find { condition }
        .orderBy(OrderTable.createdAt to SortOrder.DESC)
        .limit(limit, offset.toLong())
        .toList()

    Result.success(orders.map { it.toResponse() } to totalCount)
}
```

---

### 2.10 Add Payment Order Lookup

**New endpoint:** `GET /payment/order/{orderId}`

**Service method:**
```kotlin
fun getPaymentsByOrderId(orderId: String): Result<List<PaymentResponse>> = query {
    val payments = PaymentDAO.find { PaymentTable.orderId eq EntityID(orderId, OrderTable) }
        .orderBy(PaymentTable.createdAt to SortOrder.DESC)
        .toList()

    Result.success(payments.map { it.toResponse() })
}
```

---

### 2.11 Add Payment Validation

**In PaymentService.createPayment:**
```kotlin
fun createPayment(paymentRequest: PaymentRequest, userId: String): Result<PaymentResponse> = query {
    val order = OrderDAO.findById(EntityID(paymentRequest.orderId, OrderTable))
        ?: return@query Result.failure("Order not found".notFoundException())

    if (order.userId.value != userId) {
        return@query Result.failure("Unauthorized".unauthorizedAccessException())
    }

    // Validate amount matches order total
    if (paymentRequest.amount != order.total) {
        return@query Result.failure("Payment amount (${paymentRequest.amount}) does not match order total (${order.total})".validationException())
    }

    // Check if already fully paid
    val existingPayments = PaymentDAO.find {
        (PaymentTable.orderId eq order.id) and (PaymentTable.status eq PaymentStatus.COMPLETED.name)
    }.toList()

    val paidAmount = existingPayments.sumOf { it.amount }
    if (paidAmount >= order.total) {
        return@query Result.failure("Order already fully paid".validationException())
    }

    // Create payment
    val payment = PaymentDAO.new {
        this.orderId = EntityID(paymentRequest.orderId, OrderTable)
        this.userId = EntityID(userId, UserTable)
        this.amount = paymentRequest.amount
        this.status = paymentRequest.status
        this.paymentMethod = paymentRequest.paymentMethod
        this.transactionId = paymentRequest.transactionId
    }

    // Update order payment status if fully paid
    if (paidAmount + paymentRequest.amount >= order.total) {
        order.paymentStatus = PaymentStatus.PAID.name
    }

    Result.success(payment.toResponse())
}
```

---

### 2.12 Resolve Dual Stock Quantity Problem

**Problem:** Both `product.stock_quantity` and `inventory.stock_quantity` exist and can diverge.

**Solution:** Make `product.stock_quantity` a computed field derived from inventory, OR keep it as the source of truth for products without inventory records.

**Approach — Inventory is source of truth when it exists:**

Add a helper in ProductService:
```kotlin
fun getEffectiveStockQuantity(product: ProductDAO): Int = query {
    val inventory = InventoryDAO.find {
        InventoryTable.productId eq product.id
    }.firstOrNull()

    inventory?.stockQuantity ?: product.stockQuantity
}
```

Update all stock checks to use `getEffectiveStockQuantity()` instead of `product.stockQuantity`.

**Long-term:** Remove `product.stock_quantity` column once all products have inventory records. Add a migration:
```sql
-- One-time sync
UPDATE product p
SET stock_quantity = i.stock_quantity
FROM inventory i
WHERE p.id = i.product_id AND p.stock_quantity != i.stock_quantity;
```

---

### Phase 2 Completion Checklist

```
[ ] 2.1  Cart summary endpoint (GET /cart/summary)
[ ] 2.2  Stock validation at checkout
[ ] 2.3  Price validation from DB at checkout
[ ] 2.4  Cart cleared after successful order
[ ] 2.5  Idempotency key support for orders
[ ] 2.6  Human-readable order numbers (ORD-YYYYMMDD-XXXX)
[ ] 2.7  Order cancellation endpoint + logic
[ ] 2.8  Return/refund request flow (new tables + endpoints)
[ ] 2.9  Order status history tracking
[ ] 2.10 Seller order listing endpoint
[ ] 2.11 Admin order listing with filters
[ ] 2.12 Payment order lookup endpoint
[ ] 2.13 Payment amount validation against order total
[ ] 2.14 Dual stock quantity resolved
```

---

## Phase 3: Validation & API Design

> **Goal:** Standardize API contracts, enforce validation, and add proper pagination.

---

### 3.1 Enforce Valiktor Validation on All Request Models

**Rule:** Every route that receives a request body MUST call `.validation()` before passing to service.

**Pattern to apply in every route:**
```kotlin
post {
    val request = call.receive<ProductRequest>()
    request.validation()  // ← THIS LINE IS MISSING IN MOST ROUTES

    val result = service.create(request, ...)
    // ...
}
```

**Files to update:**

| File | Request Model | Status |
|------|--------------|--------|
| `ProductRoutes.kt` | `ProductRequest` | ❌ Not called |
| `ProductRoutes.kt` | `ProductWithFilterRequest` | ❌ Not called |
| `ProductRoutes.kt` | `ProductSearchRequest` | ❌ Not called |
| `ShopRoutes.kt` | `ShopRequest` | ❌ Not called |
| `InventoryRoutes.kt` | `InventoryRequest` | ❌ Not called |

**Add `validation()` method to UpdateProductRequest:**
```kotlin
data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val categoryId: String? = null,
    val subCategoryId: String? = null,
    val brandId: String? = null,
    val price: Long? = null,
    val discountPrice: Long? = null,
    val stockQuantity: Int? = null,
    val status: String? = null,
    val featured: Boolean? = null,
    val bestSeller: Boolean? = null,
    val hotDeal: Boolean? = null,
    val freeShipping: Boolean? = null
) {
    fun UpdateProductRequest.validation() {
        validate(this) {
            name?.let {
                validate(UpdateProductRequest::name).hasMaxLength(255)
            }
            price?.let {
                validate(UpdateProductRequest::price).isGreaterThan(0)
            }
            discountPrice?.let {
                validate(UpdateProductRequest::discountPrice).isGreaterThan(0)
            }
            stockQuantity?.let {
                validate(UpdateProductRequest::stockQuantity).isGreaterThan(-1)
            }
        }
    }
}
```

---

### 3.2 Add Pagination Metadata

**New response wrapper:**
```kotlin
// src/main/kotlin/com/piashcse/model/response/PaginatedResponse.kt
package com.piashcse.model.response

data class PaginatedResponse<T>(
    val data: List<T>,
    val pagination: PaginationInfo
)

data class PaginationInfo(
    val page: Int,
    val limit: Int,
    val total: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrev: Boolean
) {
    companion object {
        fun from(page: Int, limit: Int, total: Long): PaginationInfo {
            val totalPages = ((total + limit - 1) / limit).toInt()
            return PaginationInfo(
                page = page,
                limit = limit,
                total = total,
                totalPages = totalPages,
                hasNext = page < totalPages,
                hasPrev = page > 1
            )
        }
    }
}
```

**Update list endpoints to return paginated responses:**

```kotlin
// ProductRoutes.kt — GET /product
get {
    val page = call.parameters["page"]?.toIntOrNull() ?: 1
    val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
    val offset = (page - 1) * limit

    // ... existing filter logic
    val (products, total) = productService.getProductsPaginated(limit, offset, filters)

    call.respond(HttpStatusCode.OK, ApiResponse.success(
        PaginatedResponse(products, PaginationInfo.from(page, limit, total))
    ))
}
```

**Add paginated service methods:**
```kotlin
// ProductService.kt
fun getProductsPaginated(
    limit: Int, offset: Int, filters: ProductFilters
): Pair<List<ProductResponse>, Long> = query {
    var condition: Op<Boolean> = ProductTable.status eq ProductStatus.ACTIVATED.name
    // ... build condition from filters

    val total = ProductDAO.find { condition }.count()
    val products = ProductDAO.find { condition }
        .limit(limit, offset.toLong())
        .toList()

    products.map { it.toResponse() } to total
}
```

**Endpoints requiring pagination:**
```
GET /product                  → add page, return PaginatedResponse
GET /product/search           → add page, return PaginatedResponse
GET /product/seller           → add page, return PaginatedResponse
GET /shop                     → add page, return PaginatedResponse
GET /shop/public              → add page, return PaginatedResponse
GET /order                    → add page, return PaginatedResponse
GET /review-rating            → add page, return PaginatedResponse
GET /wishlist                 → already has offset, add total count
GET /inventory/low-stock      → add page, return PaginatedResponse
```

---

### 3.3 Standardize Error Response Format

**Current ApiResponse:**
```kotlin
sealed class ApiResponse {
    data class Success<T>(val data: T, val message: String) : ApiResponse()
    data class Failure(val message: String, val errorCode: String? = null) : ApiResponse()
}
```

**Update Failure to include field-level details:**
```kotlin
data class Failure(
    val message: String,
    val errorCode: String? = null,
    val details: List<FieldError>? = null
) : ApiResponse()

data class FieldError(
    val field: String,
    val message: String,
    val rejectedValue: Any? = null
)
```

**Standard usage across all routes:**
```kotlin
// Success
call.respond(HttpStatusCode.OK, ApiResponse.success(data, "Operation successful"))

// Not found
call.respond(HttpStatusCode.NotFound, ApiResponse.failure("Resource not found", errorCode = "NOT_FOUND"))

// Validation error
call.respond(HttpStatusCode.BadRequest, ApiResponse.failure(
    "Invalid input",
    errorCode = "VALIDATION_ERROR",
    details = listOf(FieldError("email", "Must be valid email", request.email))
))

// Unauthorized
call.respond(HttpStatusCode.Unauthorized, ApiResponse.failure("Unauthorized", errorCode = "UNAUTHORIZED"))

// Server error
call.respond(HttpStatusCode.InternalServerError, ApiResponse.failure(
    "Internal server error",
    errorCode = "INTERNAL_ERROR"
))
```

**Update StatusPages to match:**
```kotlin
install(StatusPages) {
    exception<ValidationException> { call, cause ->
        call.respond(HttpStatusCode.BadRequest, ApiResponse.failure(
            cause.message ?: "Validation failed",
            errorCode = "VALIDATION_ERROR"
        ))
    }
    exception<NotFoundException> { call, cause ->
        call.respond(HttpStatusCode.NotFound, ApiResponse.failure(
            cause.message ?: "Resource not found",
            errorCode = "NOT_FOUND"
        ))
    }
    exception<Throwable> { call, cause ->
        call.application.environment.log.error("Unhandled exception", cause)
        call.respond(HttpStatusCode.InternalServerError, ApiResponse.failure(
            "Internal server error",
            errorCode = "INTERNAL_ERROR"
        ))
    }
}
```

---

### 3.4 Convert All Mutation Query Params to POST Body

**Endpoints to convert:**

| Current | Target |
|---------|--------|
| `PUT /cart/update?productId=&quantity=` | `PUT /cart/update` with `{ "productId": "...", "quantity": 1 }` |
| `DELETE /cart/remove?productId=` | `DELETE /cart/remove` with `{ "productId": "..." }` |
| `PUT /inventory/stock/{id}?quantity=&operation=` | `PUT /inventory/stock/{id}` with `{ "quantity": 10, "operation": "add" }` |

**Example conversion for cart update:**
```kotlin
data class UpdateCartItemRequest(
    val productId: String,
    val quantity: Int
) {
    fun UpdateCartItemRequest.validation() {
        validate(this) {
            validate(UpdateCartItemRequest::productId).isRequired()
            validate(UpdateCartItemRequest::quantity).isGreaterThan(0)
        }
    }
}

// Route
put("update") {
    val request = call.receive<UpdateCartItemRequest>()
    request.validation()

    val result = cartService.updateCartItem(request, userId)
    // ...
}
```

---

### Phase 3 Completion Checklist

```
[ ] 3.1  .validation() called on all request models in routes
[ ] 3.2  PaginatedResponse wrapper implemented
[ ] 3.3  All list endpoints return paginated responses with total count
[ ] 3.4  Error response format includes errorCode and optional details list
[ ] 3.5  StatusPages updated to use standardized error format
[ ] 3.6  Mutation endpoints use request body instead of query params
```

---

## Phase 4: Database Schema Fixes

> **Goal:** Add missing tables, columns, and indexes needed for a production-ready database.

---

### 4.1 All Missing Indexes

Create a new Flyway migration: `V5__add_performance_indexes.sql`

```sql
-- Product indexes
CREATE INDEX IF NOT EXISTS idx_product_category ON product(category_id);
CREATE INDEX IF NOT EXISTS idx_product_subcategory ON product(sub_category_id);
CREATE INDEX IF NOT EXISTS idx_product_brand ON product(brand_id);
CREATE INDEX IF NOT EXISTS idx_product_shop ON product(shop_id);
CREATE INDEX IF NOT EXISTS idx_product_status_created ON product(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_product_sku ON product(sku);
CREATE INDEX IF NOT EXISTS idx_product_featured ON product(featured, status);
CREATE INDEX IF NOT EXISTS idx_product_best_seller ON product(best_seller, status);

-- Order indexes
CREATE INDEX IF NOT EXISTS idx_order_user_created ON "order"(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_shop ON "order"(shop_id);
CREATE INDEX IF NOT EXISTS idx_order_status ON "order"(status);
CREATE INDEX IF NOT EXISTS idx_order_payment_status ON "order"(payment_status);
CREATE INDEX IF NOT EXISTS idx_order_number ON "order"(order_number);

-- Order item indexes
CREATE INDEX IF NOT EXISTS idx_order_item_order ON order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_product ON order_item(product_id);
CREATE INDEX IF NOT EXISTS idx_order_item_shop ON order_item(shop_id);

-- Cart indexes
CREATE INDEX IF NOT EXISTS idx_cart_user ON cart_item(user_id);
CREATE INDEX IF NOT EXISTS idx_cart_product ON cart_item(product_id);

-- Wishlist indexes
CREATE INDEX IF NOT EXISTS idx_wishlist_user ON wishlist(user_id);
CREATE INDEX IF NOT EXISTS idx_wishlist_user_product ON wishlist(user_id, product_id);

-- Payment indexes
CREATE INDEX IF NOT EXISTS idx_payment_order ON payment(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_user ON payment(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_transaction ON payment(transaction_id);

-- Review indexes
CREATE INDEX IF NOT EXISTS idx_review_product_status ON review_rating(product_id, status);
CREATE INDEX IF NOT EXISTS idx_review_user ON review_rating(user_id);

-- Shipping indexes
CREATE INDEX IF NOT EXISTS idx_shipping_order ON shipping(order_id);

-- Inventory indexes
CREATE INDEX IF NOT EXISTS idx_inventory_product ON inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_shop ON inventory(shop_id);
CREATE INDEX IF NOT EXISTS idx_inventory_low_stock ON inventory(status) WHERE status = 'LOW_STOCK';

-- Shop indexes
CREATE INDEX IF NOT EXISTS idx_shop_status ON shop(status);
CREATE INDEX IF NOT EXISTS idx_shop_user ON shop(user_id);
CREATE INDEX IF NOT EXISTS idx_shop_category ON shop(category_id);

-- Seller indexes
CREATE INDEX IF NOT EXISTS idx_seller_user ON seller(user_id);
CREATE INDEX IF NOT EXISTS idx_seller_shop ON seller(shop_id);
```

---

### 4.2 Missing Columns on Existing Tables

**Flyway migration:** `V6__add_missing_columns.sql`

```sql
-- User table additions
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS failed_login_attempts INT DEFAULT 0;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Order table additions
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS canceled_by VARCHAR(50) REFERENCES "user"(id);
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS cancel_reason TEXT;
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS refund_amount BIGINT DEFAULT 0;
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100) UNIQUE;
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(100);
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS estimated_delivery TIMESTAMP;

-- Payment table additions
ALTER TABLE payment ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'USD';
ALTER TABLE payment ADD COLUMN IF NOT EXISTS refunded_amount BIGINT DEFAULT 0;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS refunded_at TIMESTAMP;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS gateway VARCHAR(50);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS gateway_response JSONB;

-- Product table additions
ALTER TABLE product ADD COLUMN IF NOT EXISTS slug VARCHAR(255);
ALTER TABLE product ADD COLUMN IF NOT EXISTS seo_title VARCHAR(255);
ALTER TABLE product ADD COLUMN IF NOT EXISTS seo_description TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS idx_product_slug ON product(slug);

-- Shipping table additions
ALTER TABLE shipping ADD COLUMN IF NOT EXISTS tracking_company VARCHAR(100);
ALTER TABLE shipping ADD COLUMN IF NOT EXISTS tracking_url VARCHAR(500);
ALTER TABLE shipping ADD COLUMN IF NOT EXISTS estimated_delivery TIMESTAMP;
ALTER TABLE shipping ADD COLUMN IF NOT EXISTS shipping_cost BIGINT;
ALTER TABLE shipping ADD COLUMN IF NOT EXISTS notes TEXT;

-- Review table additions
ALTER TABLE review_rating ADD COLUMN IF NOT EXISTS images JSONB;
ALTER TABLE review_rating ADD COLUMN IF NOT EXISTS admin_response TEXT;
ALTER TABLE review_rating ADD COLUMN IF NOT EXISTS reported_count INT DEFAULT 0;
```

---

### 4.3 New Tables — Auth & Security

**Flyway migration:** `V7__auth_security_tables.sql`

```sql
-- Refresh tokens (complements Phase 1.9)
CREATE TABLE IF NOT EXISTS refresh_token (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_token(user_id);

-- Login attempts (complements Phase 1.13)
CREATE TABLE IF NOT EXISTS login_attempt (
    id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    attempt_count INT DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(email, user_type)
);
CREATE INDEX IF NOT EXISTS idx_login_attempt_locked ON login_attempt(locked_until) WHERE locked_until IS NOT NULL;
```

---

### 4.4 New Tables — Business Logic

**Flyway migration:** `V8__business_logic_tables.sql`

```sql
-- Coupon system
CREATE TABLE IF NOT EXISTS coupon (
    id VARCHAR(50) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL, -- PERCENTAGE, FIXED
    discount_value BIGINT NOT NULL,
    min_order_amount BIGINT,
    max_uses INT,
    max_discount_amount BIGINT, -- cap for percentage discounts
    used_count INT DEFAULT 0,
    valid_from TIMESTAMP,
    valid_until TIMESTAMP,
    applicable_shop_ids JSONB, -- limit to specific shops
    applicable_category_ids JSONB, -- limit to specific categories
    applicable_product_ids JSONB, -- limit to specific products
    user_type_restriction VARCHAR(20), -- null = all, or CUSTOMER/SELLER
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_coupon_code ON coupon(code);
CREATE INDEX IF NOT EXISTS idx_coupon_active_dates ON coupon(is_active, valid_from, valid_until);

CREATE TABLE IF NOT EXISTS coupon_usage (
    id VARCHAR(50) PRIMARY KEY,
    coupon_id VARCHAR(50) NOT NULL REFERENCES coupon(id) ON DELETE CASCADE,
    user_id VARCHAR(50) NOT NULL REFERENCES "user"(id),
    order_id VARCHAR(50) NOT NULL REFERENCES "order"(id),
    discount_amount BIGINT NOT NULL,
    used_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_coupon_usage_coupon ON coupon_usage(coupon_id);
CREATE INDEX IF NOT EXISTS idx_coupon_usage_user ON coupon_usage(user_id);
CREATE INDEX IF NOT EXISTS idx_coupon_usage_order ON coupon_usage(order_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_coupon_usage_user_order ON coupon_usage(user_id, order_id);

-- Order status history (audit trail)
CREATE TABLE IF NOT EXISTS order_status_history (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by VARCHAR(50) REFERENCES "user"(id),
    notes TEXT,
    changed_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_order_status_history_order ON order_status_history(order_id, changed_at DESC);

-- Return/Refund requests
CREATE TABLE IF NOT EXISTS return_request (
    id VARCHAR(50) PRIMARY KEY,
    order_item_id VARCHAR(50) NOT NULL REFERENCES order_item(id),
    user_id VARCHAR(50) NOT NULL REFERENCES "user"(id),
    reason TEXT NOT NULL,
    images JSONB,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, REFUNDED, SHIPPED
    refund_amount BIGINT,
    refund_method VARCHAR(50), -- ORIGINAL, STORE_CREDIT, BANK_TRANSFER
    tracking_number VARCHAR(100),
    tracking_company VARCHAR(100),
    requested_at TIMESTAMP DEFAULT NOW(),
    resolved_at TIMESTAMP,
    notes TEXT,
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_return_request_order_item ON return_request(order_item_id);
CREATE INDEX IF NOT EXISTS idx_return_request_user ON return_request(user_id);
CREATE INDEX IF NOT EXISTS idx_return_request_status ON return_request(status);

-- Audit log (general purpose)
CREATE TABLE IF NOT EXISTS audit_log (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) REFERENCES "user"(id),
    action VARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE, LOGIN, STATUS_CHANGE, PASSWORD_CHANGE
    entity_type VARCHAR(50) NOT NULL, -- PRODUCT, ORDER, USER, SHOP, PAYMENT, INVENTORY
    entity_id VARCHAR(50),
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_log_entity ON audit_log(entity_type, entity_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_user ON audit_log(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log(action, created_at DESC);
```

---

### 4.4 Update Exposed Entity Classes

For each new table, create corresponding Exposed entity. Pattern:

```kotlin
// src/main/kotlin/com/piashcse/database/entities/Coupon.kt
package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import java.time.Instant

object CouponTable : BaseIdTable("coupon") {
    val code = varchar("code", 50).uniqueIndex()
    val description = text("description").nullable()
    val discountType = varchar("discount_type", 20) // PERCENTAGE, FIXED
    val discountValue = long("discount_value")
    val minOrderAmount = long("min_order_amount").nullable()
    val maxUses = integer("max_uses").nullable()
    val maxDiscountAmount = long("max_discount_amount").nullable()
    val usedCount = integer("used_count").default(0)
    val validFrom = timestamp("valid_from").nullable()
    val validUntil = timestamp("valid_until").nullable()
    val applicableShopIds = text("applicable_shop_ids").nullable() // JSONB stored as text
    val applicableCategoryIds = text("applicable_category_ids").nullable()
    val applicableProductIds = text("applicable_product_ids").nullable()
    val userTypeRestriction = varchar("user_type_restriction", 20).nullable()
    val isActive = bool("is_active").default(true)
}

class Coupon(id: EntityID<String>) : BaseEntity(id) {
    companion object : BaseEntityClass<Coupon>(CouponTable)

    var code by CouponTable.code
    var description by CouponTable.description
    var discountType by CouponTable.discountType
    var discountValue by CouponTable.discountValue
    var minOrderAmount by CouponTable.minOrderAmount
    var maxUses by CouponTable.maxUses
    var maxDiscountAmount by CouponTable.maxDiscountAmount
    var usedCount by CouponTable.usedCount
    var validFrom by CouponTable.validFrom
    var validUntil by CouponTable.validUntil
    var applicableShopIds by CouponTable.applicableShopIds
    var applicableCategoryIds by CouponTable.applicableCategoryIds
    var applicableProductIds by CouponTable.applicableProductIds
    var userTypeRestriction by CouponTable.userTypeRestriction
    var isActive by CouponTable.isActive

    val isValid: Boolean
        get() {
            if (!isActive) return false
            val now = Instant.now()
            if (validFrom != null && now < validFrom) return false
            if (validUntil != null && now > validUntil) return false
            if (maxUses != null && usedCount >= maxUses) return false
            return true
        }
}
```

**Register all new entities in `ConfigureDataBase.kt`:**
```kotlin
SchemaUtils.createMissingTablesAndColumns(
    UserTable, UserProfileTable, SellerTable,
    ShopTable, ShopCategoryTable,
    ProductTable, ProductCategoryTable, ProductSubCategoryTable, BrandTable,
    CartItemTable, WishListTable,
    OrderTable, OrderItemTable,
    PaymentTable, ShippingTable,
    InventoryTable,
    ReviewRatingTable,
    PolicyDocumentsTable, PolicyConsentsTable,
    // New tables
    RefreshTokenTable,
    LoginAttemptTable,
    CouponTable, CouponUsageTable,
    OrderStatusHistoryTable,
    ReturnRequestTable,
    AuditLogTable
)
```

---

### Phase 4 Completion Checklist

```
[ ] 4.1  All performance indexes added via Flyway migration
[ ] 4.2  Missing columns added to user, order, payment, product, shipping tables
[ ] 4.3  refresh_token table created with entity + repository
[ ] 4.4  login_attempt table created with entity + repository
[ ] 4.5  coupon + coupon_usage tables created with entities
[ ] 4.6  order_status_history table created with entity
[ ] 4.7  return_request table created with entity
[ ] 4.8  audit_log table created with entity
[ ] 4.9  All new entities registered in ConfigureDataBase.kt
[ ] 4.10  Flyway migrations ordered and tested
```

---

## Phase 5: Architecture & Code Quality

> **Goal:** Improve maintainability, add test infrastructure, and prepare for production deployment.

---

### 5.1 Add API Versioning

**Change base route prefix:**
```kotlin
// In ConfigureRouting.kt
route("/api/v1") {
    // ... all existing routes
}
```

**Rule:** All new endpoints go under `/api/v1/`. When breaking changes are needed in the future, add `/api/v2/` alongside.

---

### 5.2 Separate Admin Route Namespace

**Current:** Admin routes mixed with seller/customer routes

**Fix:**
```kotlin
// Current structure:
route("/product") {
    authenticate("seller") { ... }
    authenticate("admin") { ... }
}

// New structure:
route("/product") {
    authenticate("customer", "seller") {
        // Public + seller product routes
    }
    authenticate("seller") {
        // Seller-only product routes
    }
}

route("/admin") {
    authenticate("admin", "super_admin") {
        route("/products") {
            // Admin product management
        }
        route("/orders") {
            // Admin order management
        }
        route("/users") {
            // Admin user management
        }
        route("/shops") {
            // Admin shop approval
        }
    }
}
```

---

### 5.3 Add Health Check Endpoint

**New plugin:** `src/main/kotlin/com/piashcse/plugin/ConfigureHealth.kt`
```kotlin
package com.piashcse.plugin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import com.piashcse.database.ConfigureDataBase
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureHealth() {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf(
                "status" to "UP",
                "timestamp" to java.time.Instant.now().toString(),
                "service" to "ktor-ecom",
                "version" to application.engine.application.environment.log.name
            ))
        }

        get("/health/ready") {
            val dbHealthy = try {
                transaction { ConfigureDataBase.database.isClosed().not }
            } catch (e: Exception) {
                false
            }

            if (dbHealthy) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "READY"))
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "NOT_READY", "database" to "DOWN"))
            }
        }

        get("/health/live") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ALIVE"))
        }
    }
}
```

**Register in Application.kt:**
```kotlin
configureHealth()
```

---

### 5.4 Add Structured Logging

**Add to `build.gradle.kts`:**
```kotlin
implementation("net.logstash.logback:logstash-logback-encoder:7.4")
```

**Update `logback.xml`:**
```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <providers>
                <timestamp/>
                <loggerName/>
                <threadName/>
                <logLevel/>
                <message/>
                <stackTrace/>
                <mdc/>
            </providers>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

    <logger name="com.piashcse" level="DEBUG"/>
    <logger name="io.ktor" level="INFO"/>
</configuration>
```

**Add request ID tracking:**
```kotlin
// In ConfigureRouting.kt or a new plugin
intercept(ApplicationCallPipeline.Monitoring) { call ->
    val requestId = call.request.header("X-Request-ID") ?: UUID.randomUUID().toString()
    call.response.header("X-Request-ID", requestId)
    MDC.put("requestId", requestId)
}
```

---

### 5.5 Add Test Infrastructure

**Test directory structure:**
```
src/test/kotlin/com/piashcse/
├── feature/
│   ├── auth/
│   │   ├── AuthServiceTest.kt
│   │   └── AuthRoutesTest.kt
│   ├── product/
│   │   ├── ProductServiceTest.kt
│   │   └── ProductRoutesTest.kt
│   ├── order/
│   │   ├── OrderServiceTest.kt
│   │   └── OrderRoutesTest.kt
│   └── ...
├── repository/
│   └── ...
└── TestApplication.kt
```

**TestApplication helper:**
```kotlin
// src/test/kotlin/com/piashcse/TestApplication.kt
package com.piashcse

import io.ktor.server.testing.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container

object TestDatabase {
    @Container
    val postgres = PostgreSQLContainer("postgres:15-alpine")
        .withDatabaseName("test_ecom")
        .withUsername("test")
        .withPassword("test")

    fun start() {
        if (!postgres.isRunning) postgres.start()
    }

    fun getJdbcUrl(): String = postgres.jdbcUrl
    fun stop() {
        if (postgres.isRunning) postgres.stop()
    }
}
```

**Example test:**
```kotlin
// src/test/kotlin/com/piashcse/feature/auth/AuthRoutesTest.kt
package com.piashcse.feature.auth

import com.piashcse.TestDatabase
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class AuthRoutesTest {
    @BeforeAll
    fun setup() {
        TestDatabase.start()
    }

    @AfterAll
    fun teardown() {
        TestDatabase.stop()
    }

    @Test
    fun `login with invalid credentials returns 404`() = testApplication {
        application {
            // configure test app
        }

        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"invalid@test.com","password":"wrong","userType":"CUSTOMER"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `register with weak password returns 400`() = testApplication {
        application {
            // configure test app
        }

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"test@test.com","password":"123","userType":"CUSTOMER"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
```

---

### 5.6 Migrate from Gson to kotlinx.serialization

**Why:** Gson has no Kotlin support (no data class defaults, no sealed classes, slower).

**Step 1 — Update `build.gradle.kts`:**
```kotlin
// Replace:
implementation(libs.ktor.serialization.gson)
// With:
implementation(libs.ktor.serialization.kotlinx.json)
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
```

**Step 2 — Update `ConfigureBasic.kt`:**
```kotlin
import kotlinx.serialization.json.Json

install(ContentNegotiation) {
    json(Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = false
        explicitNulls = false
        serializersModule = SerializersModule {
            // Custom serializers if needed
        }
    })
}
```

**Step 3 — Add `@Serializable` to all DTOs:**
```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class ProductRequest(
    val name: String,
    val description: String,
    // ...
)
```

---

### 5.7 Add Request ID Header Propagation

**In ConfigureRouting.kt:**
```kotlin
intercept(ApplicationCallPipeline.Monitoring) { call ->
    val requestId = call.request.headers["X-Request-ID"] ?: java.util.UUID.randomUUID().toString()
    call.response.header("X-Request-ID", requestId)
    call.attributes.put(RequestIdKey, requestId)
}

val RequestIdKey = AttributeKey<String>("RequestId")

// Usage in service layer:
val requestId = call.attributes[RequestIdKey]
log.info("[$requestId] Processing request...")
```

---

### 5.8 Add Docker Support

**`Dockerfile`:**
```dockerfile
FROM eclipse-temurin:17-jre-alpine AS runtime

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY build/install/ktor-ecom/ .

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar ktor-ecom.jar"]
```

**`docker-compose.yml`:**
```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=jdbc:postgresql://postgres:5432/ecommerce
      - DATABASE_USER=ecommerce
      - DATABASE_PASSWORD=ecommerce_secret
      - JWT_SECRET=your-jwt-secret-here
      - JWT_ISSUER=ktor-ecom
      - ALLOWED_ORIGINS=http://localhost:3000
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: ecommerce
      POSTGRES_USER: ecommerce
      POSTGRES_PASSWORD: ecommerce_secret
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ecommerce"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

---

### Phase 5 Completion Checklist

```
[ ] 5.1  API versioning (/api/v1/) added to routing
[ ] 5.2  Admin routes separated to /admin/* namespace
[ ] 5.3  Health check endpoints (/health, /health/ready, /health/live)
[ ] 5.4  Structured logging with JSON output + request ID
[ ] 5.5  Test infrastructure with TestContainers
[ ] 5.6  (Optional) Migrate Gson → kotlinx.serialization
[ ] 5.7  Request ID header propagation
[ ] 5.8  Docker + Docker Compose setup
```

---

## Implementation Checklist (Master)

### Phase 1: Critical Bugs & Security
- [ ] 1.1 EntityID table references
- [ ] 1.2 Duplicate DELETE route
- [ ] 1.3 searchProduct memory explosion
- [ ] 1.4 adjustWhere filter logic
- [ ] 1.5 getShops memory explosion
- [ ] 1.6 stockQuantity default to 0
- [ ] 1.7 Image upload null safety
- [ ] 1.8 Inventory concurrency
- [ ] 1.9 Refresh token system
- [ ] 1.10 Rate limiting
- [ ] 1.11 Password reset → POST body
- [ ] 1.12 Password strength validation
- [ ] 1.13 Login attempt tracking
- [ ] 1.14 CORS configuration

### Phase 2: Core E-Commerce Logic
- [ ] 2.1 Cart summary endpoint
- [ ] 2.2 Stock validation at checkout
- [ ] 2.3 Price validation at checkout
- [ ] 2.4 Cart clear after order
- [ ] 2.5 Idempotency key for orders
- [ ] 2.6 Human-readable order numbers
- [ ] 2.7 Order cancellation
- [ ] 2.8 Return/refund flow
- [ ] 2.9 Order status history
- [ ] 2.10 Seller order listing
- [ ] 2.11 Admin order listing
- [ ] 2.12 Payment order lookup
- [ ] 2.13 Payment validation
- [ ] 2.14 Dual stock quantity resolved

### Phase 3: Validation & API Design
- [ ] 3.1 Validation on all request models
- [ ] 3.2 Pagination metadata wrapper
- [ ] 3.3 Paginated list endpoints
- [ ] 3.4 Standardized error format
- [ ] 3.5 Mutation query params → body

### Phase 4: Database Schema
- [ ] 4.1 Performance indexes
- [ ] 4.2 Missing columns
- [ ] 4.3 Auth/security tables
- [ ] 4.4 Business logic tables
- [ ] 4.5 Entity class updates

### Phase 5: Architecture & Quality
- [ ] 5.1 API versioning
- [ ] 5.2 Admin route namespace
- [ ] 5.3 Health checks
- [ ] 5.4 Structured logging
- [ ] 5.5 Test infrastructure
- [ ] 5.6 Request ID tracking
- [ ] 5.7 Docker support

---

## Quick Reference: File Change Map

| File | Phases | Changes |
|------|--------|---------|
| `ProductService.kt` | 1.1, 1.3, 1.4, 2.2, 2.3, 2.14 | EntityID fix, SQL search, WHERE fix, checkout validation, stock sync |
| `ProductRoutes.kt` | 1.2, 1.5, 1.6, 1.7, 3.1, 3.2 | Duplicate route, pagination, stockQuantity, image upload, validation |
| `OrderService.kt` | 2.2, 2.3, 2.4, 2.5, 2.6 | Checkout validation, cart clear, idempotency, order number, cancel |
| `OrderRoutes.kt` | 2.6, 2.8, 2.9, 3.2 | Cancel endpoint, seller/admin listing, pagination |
| `PaymentService.kt` | 2.11, 2.13 | Amount validation, gateway response |
| `PaymentRoutes.kt` | 2.10, 3.2 | Order lookup, pagination |
| `InventoryService.kt` | 1.8, 2.12 | Atomic SQL update, ownership check, stock sync |
| `CartService.kt` | 2.1 | Summary endpoint |
| `CartRoutes.kt` | 3.4 | Mutation body conversion |
| `ShopService.kt` | 1.5, 3.2 | SQL filtering, pagination |
| `AuthService.kt` | 1.9, 1.10, 1.11, 1.12, 1.13 | Refresh tokens, rate limiting, password reset, strength, login attempts |
| `AuthRoutes.kt` | 1.9, 1.10, 1.11 | Refresh/logout endpoints, POST reset |
| `ConfigureDataBase.kt` | 4.1-4.5 | All new entities |
| `CongfigureAuth.kt` | 1.9 | Refresh token validation support |
| `ConfigureBasic.kt` | 1.14, 5.6 | CORS, serialization |
| `ConfigureRouting.kt` | 5.1, 5.3, 5.7 | API versioning, request ID |

---

## How to Use This Document with AI

When asking an AI to implement a phase:

1. **Reference the phase number** — e.g., "Implement Phase 1.9: Refresh Token System"
2. **Specify the exact files** — e.g., "Create `RefreshToken.kt` in `database/entities/`"
3. **Copy the code blocks** — The code provided is ready to paste
4. **Verify with the checklist** — Check off items after implementation
5. **Ask for tests** — "Write tests for this implementation following the pattern in Section 5.5"

Example prompts:
- "Implement Phase 1.1: Fix EntityID table references in ProductService.kt"
- "Implement Phase 1.9: Refresh Token System — create the table, entity, repository, service methods, and routes"
- "Implement Phase 3.2: Add pagination metadata wrapper and convert GET /product to use it"

---

*Document version: 1.0 | Generated: 2026-04-11 | Project: ktor-ecom*