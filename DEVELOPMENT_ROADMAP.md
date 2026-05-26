# Ktor E-Commerce — Enterprise Development Roadmap

> A phased, implementation-focused, production-readiness roadmap for building out the Ktor e-commerce platform. Use this document to guide AI-assisted development. Each phase is self-contained, sequential, and builds on top of the current compiled codebase.

---

## Table of Contents

- [Architectural Principles & Standards](#architectural-principles--standards)
- [Phase 1: REST Pluralization, Centralized Validation & Content Negotiation](#phase-1-rest-pluralization-centralized-validation--content-negotiation)
- [Phase 2: Database Schema & Migration Excellence](#phase-2-database-schema--migration-excellence)
- [Phase 3: Repository Layer Separation & Dependency Injection](#phase-3-repository-layer-separation--dependency-injection)
- [Phase 4: Production Infrastructure & Monitoring](#phase-4-production-infrastructure--monitoring)
- [Phase 5: Security Hardening & Resource Isolation](#phase-5-security-hardening--resource-isolation)
- [Phase 6: Automated Testing Suite](#phase-6-automated-testing-suite)
- [Phase 7: Real Payment Gateway Adapter Integration](#phase-7-real-payment-gateway-adapter-integration)
- [Phase 8: Advanced Catalog Search & Discovery](#phase-8-advanced-catalog-search--discovery)
- [Phase 9: Advanced Coupon & Promotion Engine](#phase-9-advanced-coupon--promotion-engine)
- [Phase 10: Concurrency-Safe Stock Reservation & Asynchronous Workers](#phase-10-concurrency-safe-stock-reservation--asynchronous-workers)
- [Master Implementation Checklist](#master-implementation-checklist)

---

## Architectural Principles & Standards

To ensure maximum **reusability**, **conciseness**, and **performance**, all upcoming development must adhere strictly to these industry-standard principles:

### 1. Centralized Validation (Transforming Pipeline Interceptor)
* **Goal**: Eliminate boilerplate `.validation()` calls inside every individual route block and do away with manual class registrations inside `ConfigureRequestValidation.kt`.
* **Standard**: Implement a unified `Validatable` interface inside `com.piashcse.utils.validator`.
* **Mechanism**: Register an interceptor on Ktor's `ApplicationReceivePipeline.Transform`. Any class received via `call.receive<T>()` that implements `Validatable` is automatically verified prior to route execution, throwing a standardized exception handled by `StatusPages`.

### 2. Centralized Exception Handling
* **Goal**: Centralize domain-level and data-level exception translation.
* **Standard**: Intercept Exposed-specific database exceptions (e.g., `ExposedSQLException`, foreign key lockouts, unique constraint violations, key collisions) within `ConfigureStatusPage.kt` and format them to safe, standardized JSON responses, keeping low-level SQL stack traces out of production client views.

### 3. Industry-Standard JSON Serialization
* **Goal**: Modernize content negotiation for type safety, performance, and cross-platform compatibility.
* **Standard**: Migrate from legacy reflection-heavy `Gson` to compile-time generated **Kotlinx Serialization**.
* **Configuration**: Register `json()` with:
  ```kotlin
  ignoreUnknownKeys = true
  coerceInputValues = true
  prettyPrint = true
  ```

### 4. RESTful Plural Routing Best Practices
* **Goal**: Follow REST conventions used by top-tier platforms (Stripe, Shopify, GitHub).
* **Standard**: Pluralize all API resource collections. Change all singular route directories to plurals (e.g. `/product` → `/products`, `/order` → `/orders`, `/cart` → `/carts`, `/wishlist` → `/wishlists`, `/coupon` → `/coupons`).

### 5. Naming Conventions & Database Standards
* **API Level**: JSON keys must strictly use `camelCase`. DTO inputs must clearly suffix requests as `Request` and responses as `Response`.
* **Database Level**: Exposed tables and columns must use database-native `snake_case` (e.g., `product_id`, `created_at`). 
* **Statuses & Enums**: Eliminate loose `String` inputs for database state fields. Bind fields directly to typed enums (`OrderStatus`, `PaymentStatus`, `InventoryStatus`).

### 6. Logs & Query Redaction
* **Standard**: Redact sensitive parameters (e.g. `password`, `token`, `otp`, `auth`) dynamically inside CallLogging configurations to guarantee credentials are never outputted to plain log files.

---

## Phase 1: REST Pluralization, Centralized Validation & Content Negotiation

> **Goal:** Standardize the routing namespace, implement zero-boilerplate centralized request validation, move to compile-time Kotlinx Serialization, and secure call logging blocks.

### 1.1 REST Route Pluralization
Update route naming paths to follow REST standards.
* **Files to modify**: Update all routes inside `src/main/kotlin/com/piashcse/feature/` and [ConfigureRouting.kt](file:///Users/mehedihassanpiash/Documents/OpenSource/ktor-ecom/src/main/kotlin/com/piashcse/plugin/ConfigureRouting.kt).
  - Pluralize API paths: `/api/v1/product` becomes `/api/v1/products`, `/api/v1/order` becomes `/api/v1/orders`, `/api/v1/cart` becomes `/api/v1/carts`, `/api/v1/wishlist` becomes `/api/v1/wishlists`, `/api/v1/coupon` becomes `/api/v1/coupons`, `/api/v1/refund-requests` becomes `/api/v1/refunds`.

### 1.2 Reusable Validatable Transforming Pipeline Interceptor
Create a zero-boilerplate validation pipeline.
* **New file**: `src/main/kotlin/com/piashcse/utils/validator/Validatable.kt`
  ```kotlin
  package com.piashcse.utils.validator
  
  interface Validatable {
      fun validation()
  }
  ```
* **Enforce Interfaces**: Update all 26 request DTOs inside `src/main/kotlin/com/piashcse/model/request/` to implement the `Validatable` interface (e.g. `data class LoginRequest(...) : Validatable`).
* **Interception Plugin**: Replace Ktor `RequestValidation` with a transforming receive interceptor:
  ```kotlin
  // Inside ConfigureRequestValidation.kt:
  fun Application.configureRequestValidation() {
      intercept(ApplicationReceivePipeline.Transform) { query ->
          val subject = subject.value
          if (subject is Validatable) {
              subject.validation()
          }
          proceedWith(subject)
      }
  }
  ```
  This automatically runs model checks on any validatable type loaded with `call.receive()`, eliminating manual `.validation()` calls inside routes.

### 1.3 Migrate Content Negotiation to Kotlinx Serialization
Adopt type-safe, reflection-free JSON handling.
* **Files to modify**: `build.gradle.kts`, `ConfigureBasic.kt`
  - Remove Ktor Gson dependencies.
  - Implement Kotlinx Serialization content negotiation:
    ```kotlin
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            prettyPrint = true
        })
    }
    ```
* **Request/Response Models**: Annotate all model classes in the `model` folder with `@Serializable`.

### 1.4 CallLogging Secret Redaction
Prevent sensitive passwords and keys from leaking into log aggregators.
* **File to modify**: `ConfigureBasic.kt`
  - Edit the query parameters formatting in `CallLogging` to redact values of sensitive keys (`password`, `token`, `otp`, `secret`):
    ```kotlin
    val sensitiveKeys = setOf("password", "token", "otp", "secret", "authorization")
    val queryParams = call.request.queryParameters.entries()
        .joinToString(", ") { (key, value) -> 
            if (key.lowercase() in sensitiveKeys) "$key=[REDACTED]" else "$key=$value" 
        }
    ```

---

## Phase 2: Database Schema & Migration Excellence

> **Goal:** Transition the database from startup-level generation to stable, version-controlled migrations with Flyway, performance-tuned indexes, cascading rules, and check constraints.

### 2.1 Transition from SchemaUtils to Flyway Migrations
Remove imperative database generation on startup. Let Flyway govern the database schema in a version-controlled manner.
* **File to modify**: [ConfigureDataBase.kt](file:///Users/mehedihassanpiash/Documents/OpenSource/ktor-ecom/src/main/kotlin/com/piashcse/database/ConfigureDataBase.kt)
  - Remove `SchemaUtils.create(...)` from `configureDataBase()`.
  - Ensure `flyway.migrate()` is the single authoritative source of schema generation.
* **New file**: `src/main/resources/db/migration/V1__baseline_schema.sql`
  - Convert all existing 21 tables currently defined in the Exposed mappings into standard SQL definitions. This forms the baseline script for production database deployments.

### 2.2 High-Performance Indexes Migration
Create database-level indexes on all foreign keys, composite filters, and sorting paths to optimize query execution and prevent sequential table scans.
* **New file**: `src/main/resources/db/migration/V2__add_performance_indexes.sql`
  - **Foreign Key Indexes**: Index all columns ending in `_id` on related tables (e.g. `product_id` on `cart_item`, `user_id` on `shop`, `category_id` on `product`).
  - **Composite Query Indexes**: Build multi-column indexes for standard filtering paths:
    ```sql
    CREATE INDEX IF NOT EXISTS idx_product_category_status ON product(category_id, status);
    CREATE INDEX IF NOT EXISTS idx_product_shop_status ON product(shop_id, status);
    CREATE INDEX IF NOT EXISTS idx_product_status_created ON product(status, created_at DESC);
    CREATE INDEX IF NOT EXISTS idx_review_product_status ON review_rating(product_id, status);
    ```
  - **Performance Flags**: Index boolean tags used in catalogs like `featured` and `best_seller` on `product` table.

### 2.3 Database-Level Constraints & Cascades
Enforce referential integrity, strict cascades, uniqueness, and column checks directly at the engine level to protect the data from garbage inputs.
* **New file**: `src/main/resources/db/migration/V3__add_database_constraints.sql`
  - **Unique Constraints**: Add composite uniqueness checks to prevent duplicate records:
    ```sql
    ALTER TABLE cart_item ADD CONSTRAINT uq_cart_user_product UNIQUE (user_id, product_id);
    ALTER TABLE wishlist ADD CONSTRAINT uq_wishlist_user_product UNIQUE (user_id, product_id);
    ALTER TABLE review_rating ADD CONSTRAINT uq_review_user_product UNIQUE (user_id, product_id);
    ALTER TABLE policy_consent ADD CONSTRAINT uq_consent_user_policy UNIQUE (user_id, policy_id);
    ```
  - **On Delete Cascade/Restrict Rules**: Drop generic foreign keys and recreate them with explicit behavior. Deleting a user must cascade delete profiles and refresh tokens. Deleting an order must cascade to order items, payments, and shipping. Product deletion must be restricted if active orders reference the SKU.
  - **CHECK Constraints**: Prevent negative and invalid values:
    ```sql
    ALTER TABLE cart_item ADD CONSTRAINT chk_cart_quantity CHECK (quantity > 0);
    ALTER TABLE order_item ADD CONSTRAINT chk_order_item_quantity CHECK (quantity > 0);
    ALTER TABLE inventory ADD CONSTRAINT chk_inventory_stock CHECK (stock_quantity >= 0);
    ALTER TABLE product ADD CONSTRAINT chk_product_price CHECK (price > 0);
    ```

### 2.4 Schema Extensions & Typed Column Integrations
Extend existing tables to support administrative logs, rich search optimization, audit history, and type safety.
* **New file**: `src/main/resources/db/migration/V4__add_missing_columns_and_history.sql`
  - Add `slug`, `seo_title`, and `seo_description` columns to `product` to support search engine friendly metadata.
  - Add tracking detail fields (`tracking_number`, `tracking_company`, `estimated_delivery`, `notes`) to `order` and `shipping` tables.
  - Add `currency`, `refunded_amount`, `gateway`, and `gateway_response` columns to the `payment` table.
* **Exposed Entity Updates**: 
  - Update Exposed table objects (e.g. `ProductTable`, `PaymentTable`) and corresponding DAO classes (e.g. `ProductDAO`, `PaymentDAO`) in `src/main/kotlin/com/piashcse/database/entities/` to register these new columns.
  - Convert `VARCHAR` status columns to proper Kotlin-typed Enums in mappings (e.g., mapping `ReviewRatingTable.status` to `ReviewStatus` enum).

---

## Phase 3: Repository Layer Separation & Dependency Injection

> **Goal:** Decouple services from database implementations by introducing concrete Repository patterns, cleaning up Koin injection blocks, resolving N+1 queries, and breaking monolithic classes apart.

### 3.1 Concrete Repository Implementation
Separate database access logic from business services. Services must depend on interfaces, while actual SQL logic resides in repositories.
* **Refactoring Steps**:
  1. Define a separate interface file if not already present (e.g. `ProductRepository`).
  2. Implement concrete repository classes using JetBrains Exposed (e.g. `ProductRepositoryImpl` inside the feature folder).
  3. Move all database query expressions (`ProductDAO.find`, `ProductTable.selectAll()`) and `query { ... }` transaction wrappers from `ProductService` into `ProductRepositoryImpl`.
  4. Repositories must return immutable DTOs or entity snapshots, leaving the service layer completely unaware of active DAO row wrappers.
* **Koin Injection Update**: Register repositories and services in [KoinModule.kt](file:///Users/mehedihassanpiash/Documents/OpenSource/ktor-ecom/src/main/kotlin/com/piashcse/di/KoinModule.kt):
  ```kotlin
  single<ProductRepository> { ProductRepositoryImpl() }
  single { ProductService(get()) }
  ```
  Remove default values (e.g. `repository = ProductRepositoryImpl()`) in Service constructor arguments to prevent bypass of the dependency injection container.

### 3.2 Eliminate N+1 Query Patterns
Audit and rewrite query mapping functions to load related datasets in batch collections rather than executing sequential subqueries inside loops.
* **File to modify**: `CartService.kt`
  - Instead of fetching cart items and calling `ProductDAO.find` inside a loop for each item (N+1 database reads), fetch all distinct product IDs first.
  - Perform a single database query `ProductDAO.find { ProductTable.id inList productIds }` and map them inside a Kotlin dictionary for memory-mapped association.
* **File to modify**: `ProductCategory.kt`
  - Remove lazy-loaded subcategory queries inside response mappings. Pre-fetch subcategories in a single batch query or split them into a dedicated paginated endpoint.

### 3.3 Split Monolithic God Services
Decompose heavy service files that bear excessive domain responsibilities into highly focused, single-responsibility services.
* **Decompose AuthService.kt** (handles login, lockout, reset keys, registration, token hashing) into:
  - `UserAuthenticationService` (managing login pipeline and brute-force lockouts)
  - `PasswordManagementService` (governing resets, strength rules, and updates)
  - `TokenManagementService` (managing JWT tokens, refresh hashes, and logouts)
* **Decompose ProductService.kt** into:
  - `ProductCrudService` (basic creating, updating, and physical deletion)
  - `ProductQueryService` (handling search, filtering, and catalog listings)
  - `ProductCatalogService` (governing bestsellers, hot deals, and popular items)

---

## Phase 4: Production Infrastructure & Monitoring

> **Goal:** Deploy containerized configurations, health status handlers, structured logs, and metrics tracking to ensure continuous monitoring and resilience in production.

### 4.1 Docker & Multi-Stage Deployment
Containerize the application following production best practices (security, small image size, dependency caching).
* **New file**: `Dockerfile`
  - Build the fat jar in a multi-stage process using Gradle and a JDK image.
  - Use `eclipse-temurin:17-jre-alpine` for the runtime phase to minimize storage footprint.
  - Define an unprivileged system group and user (`appuser`), change file ownership, and execute the service without root access.
* **New file**: `docker-compose.yml`
  - Declare an `app` service dependent on a `postgres` database container.
  - Integrate a Postgres health check using `pg_isready` to guarantee database availability before the Ktor server starts up.

### 4.2 Health & Readiness Routes
Configure endpoint probes so external orchestrators (like Kubernetes or docker-compose) can audit application health.
* **New file**: `src/main/kotlin/com/piashcse/plugin/ConfigureHealth.kt`
  - Register `/health/live` to output a generic `{"status":"ALIVE"}` immediately.
  - Register `/health/ready` to execute a lightweight database transaction (`transaction { !connection.isClosed }`). If database connectivity is interrupted, return `503 Service Unavailable` with a `"NOT_READY"` payload.

### 4.3 Structured Logging & Request IDs
Migrate standard text logs to machine-readable JSON formats and trace requests through the entire system.
* **Dependency modification**: Add `net.logstash.logback:logstash-logback-encoder:7.4` to Gradle build.
* **File to modify**: `logback.xml`
  - Configure a Logstash JSON encoder for console appenders in production, exporting structured records.
* **Request ID Interceptor**: Integrate an application plugin to extract or generate a unique `X-Request-ID` header. Track this ID inside the SLF4J MDC (Mapped Diagnostic Context) so every database query or error log is stamped with the request origin.

### 4.4 Metrics & Observability Export
Establish real-time instrumentation to audit runtime health under high traffic loads.
* **Metrics Route**: Install Ktor's `MicrometerMetrics` plugin using the Prometheus registry.
* **Route**: Expose `/metrics` protected by admin role authorization to securely export memory, thread counts, JVM garbage collection cycles, and active database connection pool stats to Prometheus/Grafana dashboards.

---

## Phase 5: Security Hardening & Resource Isolation

> **Goal:** Secure the API layer by moving credentials to safe environments, reducing token lifespan, checking size/mimetype uploads, and validating strict resource ownership.

### 5.1 Hardening Configurations & Environment Security
Ensure no secrets are exposed in source control.
* **File to modify**: `DotEnvConfig.kt`
  - Remove all default fallback strings for `JWT_SECRET`, database passwords, or third-party keys.
  - Throw a strict `IllegalStateException` on startup if these variables are empty.
* **File to modify**: `AppConstants.kt`
  - Delete all hardcoded SMTP server credentials. Read `EMAIL_USERNAME` and `EMAIL_PASSWORD` dynamically from configuration files or system environment variables.

### 5.2 Short Token Lifespan & Token Blacklisting
Reduce the exploitation window of intercepted JWT access tokens and secure the logout process.
* **File to modify**: `JwtConfig.kt`
  - Reduce access token validity duration from 24 hours to 15 minutes.
  - Implement dynamic JWT claim checks including `Audience` validation matching standard domain hosts.
* **Revocation Engine**:
  - In `AuthRoutes.kt`, when `/auth/logout` is hit, add the token signature or ID to a revocation database block.
  - Ensure the JWT authentication plugin queries this database to invalidate active logout sessions immediately.

### 5.3 Multi-Stage Upload Security Filtering
Prevent file system exploitation and denial of service attacks through unrestricted image uploads.
* **Files to modify**: `ProductRoutes.kt`, `ProfileRoutes.kt`
  - Install a strict file size validation check, rejecting payloads larger than 5MB.
  - Read first few bytes (magic bytes signature) of uploaded streams to verify they are valid images (JPEG, PNG, WEBP), preventing malicious script uploads disguised with image extensions.
  - Store files outside project resource folders under an external directory specified by the `UPLOAD_DIR` environment variable.

### 5.4 Fine-Grained Ownership Verification
Add validation guards to guarantee users can only mutate resources they physically own.
* **Ownership Matrix Checks**:
  - **Orders**: A user can only view or cancel orders belonging to their `userId`. A seller can only update order statuses for orders placed with their `shopId`.
  - **Reviews**: A user can only edit or delete a review if `review.userId == currentUserId`.
  - **Shop Management**: A seller can only update details for a shop if they are registered as the owner of that shop.
  - **Product Management**: A seller can only edit/delete products if `product.userId == currentUserId`.

---

## Phase 6: Automated Testing Suite

> **Goal:** Secure the platform from regressions by setting up PostgreSQL integration tests with Testcontainers, routing contract assertions, and mocking services.

### 6.1 Integration Tests with Testcontainers
Boot dynamic, real database environments during Gradle build test tasks to ensure query code is fully verified.
* **New file**: `src/test/kotlin/com/piashcse/testutils/PostgresTestContainer.kt`
  - Configure a shared `PostgreSQLContainer` instance to pull a lightweight `postgres:15-alpine` image.
  - Initialize connection details, overriding Koin properties and database configurations with the test container's dynamic JDBC URL.

### 6.2 Endpoint Route Contract Tests
Simulate API client calls using Ktor's native, high-performance `testApplication` test server engine.
* **New file**: `src/test/kotlin/com/piashcse/feature/auth/AuthRoutesTest.kt`
  - Write route tests checking registration strength checks, login failure lockout counters, and valid login token responses.
* **New file**: `src/test/kotlin/com/piashcse/feature/product/ProductRoutesTest.kt`
  - Test pagination parameters, validation rules, search parameters, and missing fields.
* **Validation Standard**: Assert that all API error returns strictly adhere to the standard error JSON schema:
  ```json
  {
    "message": "Error details...",
    "details": ["Field validation failed"]
  }
  ```

### 6.3 Service Layer Mocking (Unit Tests)
Use MockK to write lightweight, fast-running unit tests for business layers without needing database connections.
* **Service Coverage**:
  - Write test cases for order discount distributions, tax calculations, and cart clear validations in `OrderService`.
  - Verify that exception behaviors (such as `throwConflict`, `throwNotFound`) are triggered correctly under validation failures.

---

## Phase 7: Real Payment Gateway Adapter Integration

> **Goal:** Transition the platform from a simulated payment workflow to an actual Stripe credit card transaction execution engine using an abstract adapter pattern.

### 7.1 Gateway Adapter Abstraction
Define a clean, decoupled boundary for payment processing so alternate systems (Stripe, PayPal, Adyen) can be integrated without breaking order management code.
* **New file**: `src/main/kotlin/com/piashcse/feature/payment/PaymentGateway.kt`
  - Define an abstract interface representing core gateway capabilities:
    ```kotlin
    interface PaymentGateway {
        suspend fun createPaymentIntent(orderId: String, amount: BigDecimal, currency: String): PaymentIntentResponse
        suspend fun verifyWebhookSignature(payload: String, signature: String): Boolean
        suspend fun processRefund(transactionId: String, amount: BigDecimal): RefundResponse
    }
    ```

### 7.2 Stripe Gateway Implementation
Write the concrete payment runner utilizing Stripe's official Java SDK.
* **Dependency modification**: Add `com.stripe:stripe-java:24.x` to the dependencies catalog.
* **New file**: `src/main/kotlin/com/piashcse/feature/payment/StripePaymentGateway.kt`
  - Create the Stripe provider implementing the `PaymentGateway` interface.
  - Implement Stripe PaymentIntent creation, converting order totals to integer cents. Load keys dynamically from environment variables.
* **Service Wiring**: Inject the newly created `PaymentGateway` into the `PaymentService` via Koin.

### 7.3 Secure Webhook Routing
Set up asynchronous webhook endpoints to receive payment completion notification payloads safely.
* **File to modify**: `PaymentRoutes.kt`
  - Register `POST /api/v1/payment/webhook` (no role auth required).
  - Extract raw body text and the `Stripe-Signature` header.
  - Invoke `paymentGateway.verifyWebhookSignature(...)` to prevent webhook spoofing attacks. On successful verification, trigger asynchronous payment captures, update order payment states to `PAID`, and release invoices.

---

## Phase 8: Advanced Catalog Search & Discovery

> **Goal:** Upgrade searching logic to support full fuzzy matches, spelling typo-tolerance, and dynamic faceted count sidebars.

### 8.1 PostgreSQL Trigram Fuzzy Search
Adopt database-backed trigram similarity filtering to prevent missing product listings due to typos.
* **Migration Script**: Enable the pg_trgm extension in the database using a Flyway script:
  ```sql
  CREATE EXTENSION IF NOT EXISTS pg_trgm;
  CREATE INDEX IF NOT EXISTS idx_product_name_trgm ON product USING gin (name gin_trgm_ops);
  ```
* **Repository Implementation**:
  - Implement Exposed queries checking trigram similarity thresholds on target search parameters, returning matched items despite variations.

### 8.2 Faceted Aggregation sidebars
Return brand and category count lists alongside standard search results to empower user discovery.
* **Route & Model updates**: Update the product search response model to include categories and brand counts.
* **Query Implementation**: Use a single database aggregation using SQL `GROUP BY` to extract category and brand match counts from the search results slice, returning this metadata dynamically.

### 8.3 Multi-Factor Product Catalog Ranking
Deliver high-performance sorting systems based on catalog popularity and sale rates.
* **Matrix logic**: Implement search sort query metrics computing ranks dynamically based on sale volumes (`totalSales`), catalog views (`viewCount`), and price discount levels.

---

## Phase 9: Advanced Coupon & Promotion Engine

> **Goal:** Extend current Coupon services to cover percentage caps, minimum transaction thresholds, and target exclusions.

### 9.1 Complex Cart coupon rules
Implement highly configurable discount logic.
* **Coupon Entities**: Complete the custom `Coupon` tables and entities, enabling:
  - Percentage-based deductions with a maximum currency cap (e.g. "10% off up to $50").
  - Fixed value reductions (e.g. "$20 off orders").
  - Minimum purchase thresholds and usage expiration checks.
  - Target restrictions (limiting code validity to specific categories, shops, or individual products).
* **Checkout Integration**: Implement coupon calculations inside `placeOrder` and `getCheckoutSummary` within `OrderService`, dividing the discount across multi-vendor orders proportionally based on cart sub-totals.

---

## Phase 10: Concurrency-Safe Stock Reservation & Asynchronous Workers

> **Goal:** Secure parallel inventory deductions during checkouts to prevent double allocation and cart abandonment stock leakage, and maximize API pipeline throughput using async workers.

### 10.1 Temporary stock Locks
Introduce temporary stock locks during high-traffic checkout flows to keep database inventory completely accurate.
* **Lock Table**: Create a `stock_reservation` table tracking product reservations during active checkout.
* **Checkout Pipeline**:
  - When checkout is initiated, subtract items from available stock and insert records into `stock_reservation`.
  - If the payment succeeds, delete the reservation rows and finalize the physical inventory deduction.
  - If checkout is abandoned or unpaid after 15 minutes, a cron job or scheduled task executes a query returning the reserved items back to the product's inventory stock.

### 10.2 Decoupled Domain Event Bus
Enable clean modular boundaries by decoupling services through events.
* **Domain Event Bus**: Create an internal pub/sub event bus built on Kotlin's `SharedFlow`.
* **Flow**:
  - When an order completes, `OrderService` simply publishes an `OrderPlacedEvent` to the bus and immediately responds to the client.
  - Decoupled event subscribers handle the notifications, update internal bestseller analytics, and trigger low-stock alerts asynchronously.

### 10.3 Asynchronous Background Task Queue
Ensure API endpoints respond instantly by processing heavy operations on background worker threads.
* **Background Worker Queue**: Implement an asynchronous queue built on Kotlin Coroutines `Channel` and persistent workers.
* **Offloaded Jobs**:
  - Move SMTP email dispatching (welcome notes, checkout invoices, OTP tokens) to the worker queue.
  - Offload profile and product image compression/resizing tasks to background threads.

---

## Master Implementation Checklist

### Phase 1: REST Pluralization, Centralized Validation & Content Negotiation
- [ ] 1.1 Pluralize all 19 feature routes and path directories
- [ ] 1.2 Create Validatable interface and update 26 request DTOs
- [ ] 1.3 Implement ApplicationReceivePipeline validation transforming interceptor
- [ ] 1.4 Migrate Content Negotiation from Gson to Kotlinx Serialization
- [ ] 1.5 Sanitize logs by redacting sensitive parameters inside call logging

### Phase 2: Database Schema & Migration Excellence
- [ ] 2.1 Remove SchemaUtils.create and set Flyway as authoritative migration engine
- [ ] 2.2 Create V1__baseline_schema.sql mapping current tables
- [ ] 2.3 Create V2__add_performance_indexes.sql covering foreign keys and composite queries
- [ ] 2.4 Create V3__add_database_constraints.sql enforcing check, unique, and cascade delete rules
- [ ] 2.5 Create V4__add_missing_columns_and_history.sql extending table fields
- [ ] 2.6 Update Exposed entities to map new columns and enums

### Phase 3: Repository Layer Separation & Dependency Injection
- [ ] 3.1 Implement concrete Repository classes for all 19 feature areas
- [ ] 3.2 Decouple services from Exposed tables and transactions
- [ ] 3.3 Set up explicit constructor-based dependency injection in KoinModule
- [ ] 3.4 Resolve N+1 query hotspots in Cart and Category queries
- [ ] 3.5 Decompose monolithic AuthService and ProductService god classes

### Phase 4: Production Infrastructure & Monitoring
- [ ] 4.1 Multi-stage Dockerfile setup using Alpine JRE image
- [ ] 4.2 Health status route probes (/health, /health/ready, /health/live)
- [ ] 4.3 Logstash structured JSON logging layout inside logback.xml
- [ ] 4.4 Generate and propagate X-Request-ID across Thread MDC and headers
- [ ] 4.5 Export system performance indicators via prometheus/metrics route

### Phase 5: Security Hardening & Resource Isolation
- [ ] 5.1 Eliminate fallback variables and require explicit environment secrets
- [ ] 5.2 Reduce token validity window to 15 minutes and add dynamic audience checks
- [ ] 5.3 Implement standard JWT blacklisting on user logouts
- [ ] 5.4 Install size checks and magic bytes verification filters for media uploads
- [ ] 5.5 Enforce strict resource ownership validation checks inside the service layer

### Phase 6: Automated Testing Suite
- [ ] 6.1 Integration test framework setup with Testcontainers running real Postgres
- [ ] 6.2 Route test suite running with testApplication asserting endpoints contracts
- [ ] 6.3 Service unit test suite utilizing MockK to assert business constraints

### Phase 7: Real Payment Gateway Adapter Integration
- [ ] 7.1 Define abstract PaymentGateway interface
- [ ] 7.2 Write StripePaymentGateway using the official Stripe Java SDK
- [ ] 7.3 Secure /payment/webhook endpoint with signature verification

### Phase 8: Advanced Catalog Search & Discovery
- [ ] 8.1 Trigram-based fuzzy search implementation supporting spelling typos
- [ ] 8.2 Brand and category match counts in search metadata payloads
- [ ] 8.3 Unified ranking formula for catalog lists (views, discounts, sales)

### Phase 9: Advanced Coupon & Promotion Engine
- [ ] 9.1 Percentage cap and minimum transaction rules in Coupon Service
- [ ] 9.2 Coupon target exclusions (limit to categories/shops/products)
- [ ] 9.3 OrderService checkout coupon discount distribution engine

### Phase 10: Concurrency-Safe Stock Reservation & Asynchronous Workers
- [ ] 10.1 Create stock_reservation locks table
- [ ] 10.2 Checkout stock locking pipeline integration
- [ ] 10.3 Automatic expired reservation release scheduled worker
- [ ] 10.4 Kotlin SharedFlow Domain Event Bus routing order/user events asynchronously
- [ ] 10.5 Asynchronous worker channel executing SMTP email and image compression

---

*Document version: 3.0 | Updated: 2026-05-26 | Project: ktor-ecom*