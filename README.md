# Ktor-E-Commerce

[![Ktor](https://img.shields.io/badge/ktor-3.4.3-blue.svg)](https://github.com/ktorio/ktor)
[![Exposed](https://img.shields.io/badge/Exposed-1.2.0-blue.svg)](https://github.com/JetBrains/Exposed)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
![Koin](https://img.shields.io/badge/Koin-4.2.0-29BEB0?logo=koin&logoColor=white)
[![PostgreSQL Version](https://img.shields.io/badge/PostgreSQL-42.7.8-336791?logo=postgresql)](https://www.postgresql.org/)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
<a href="https://github.com/piashcse"><img alt="License" src="https://img.shields.io/static/v1?label=GitHub&message=piashcse&color=C51162"/></a>

Ktor-E-Commerce is a high-performance backend solution designed for modern e-commerce applications built
with [ktor](https://ktor.io/docs/welcome.html). This backend leverages the power of Kotlin to provide a robust,
scalable, and efficient service for handling your e-commerce needs. For detailed documentation and guides, visit the project [documentation](https://piashcse.github.io/ktor-E-Commerce).

<p align="center">
  <img width="100%" height="40%" src="https://github.com/piashcse/ktor-E-Commerce/blob/master/screenshots/swagger.gif" />
</p>

# Features

### 1. Role-Based Access Control

- **Customer Role**: Shoppers with basic access to browse and make purchases.
- **Seller Role**: Vendors can list products and manage their inventory.
- **Admin Role**: Administrators have full control over the platform.

### 2. User Accounts and Authentication

- **User Registration**: Allow customers to create accounts. Users can register with the same email for different roles (customer and seller).
- **User Authentication**: Implement JWT-based authentication for user sessions.
- **User Profiles**: Enable users to view and update their profiles.

### 3. Product Management

- **Product Listings**: Create, update, and delete product listings.
- **Categories**: Organize products into categories and brands for easy navigation.
- **Inventory Control**: Keep track of product availability and stock levels.

### 4. Shopping Cart and Checkout

- **Shopping Cart**: Add and remove products, update quantities, and calculate totals.
- **Checkout Summary**: Retrieve detailed checkout summary with subtotal, shipping, tax, and discount breakdown.
- **Checkout**: Streamline the checkout process with consolidated shipping address and method selection.
- **Stock Validation**: Automatic stock validation during checkout using effective inventory levels.
- **Price Validation**: Order totals validated against database prices to prevent tampering.
- **Cart Clearing**: Automatic cart clearing after successful order placement.

### 5. Order Management

- **Order Processing**: Handle order creation, status updates, and order history.
- **Order Status History**: Comprehensive audit trail for every status transition (Placed, Confirmed, Shipped, etc.).
- **Automatic Taxation**: Configurable tax calculation (default 5%) applied automatically during checkout.
- **Order Cancellation**: Customers can cancel PENDING/CONFIRMED orders with automatic stock restoration.
- **Seller Orders**: Sellers can view and manage orders from their shop.
- **Admin Orders**: Admins can view all orders with advanced filters (status, date range).
- **Human-Readable Order Numbers**: Sequential order numbers in format ORD-YYYYMMDD-XXXX.
- **Idempotency Support**: Prevent duplicate orders with idempotency keys.
- **Payment Integration**: Integrate with popular payment gateways for seamless transactions.
- **Payment Validation**: Payment amounts validated against order totals.
- **Payment History**: Track all payments for each order.

### 6. Discount & Coupon System

- **Promo Codes**: Create and manage fixed or percentage-based discount coupons.
- **Usage Limits**: Enforce expiration dates, usage limits per coupon, and minimum order amounts.
- **Admin Controls**: Dedicated administrative interface for managing the coupon lifecycle.

### 7. Refund Management

- **Refund Requests**: Customers can request refunds for order items with reasons and evidence images.
- **Refund Approval**: Sellers and admins can approve, reject, or process refunds.
- **Return Shipping**: Customers can mark approved refunds as shipped with tracking numbers.
- **Refund Tracking**: Complete refund lifecycle tracking from request to resolution.

### 6. Scalability and Performance

- **Asynchronous Processing**: Leverage Ktor's async capabilities for high performance.
- **Load Balancing**: Easily scale your application to accommodate increased traffic.

### 7. Security

- **JWT Tokens**: Implement JSON Web Tokens for secure authentication.
- **Refresh Tokens**: Secure token refresh with hashed storage and automatic revocation.
- **Rate Limiting**: Auth endpoints protected against brute-force attacks (5 req/10min).
- **Account Lockout**: Automatic 30-minute lockout after 5 failed login attempts.
- **Password Strength**: Enforced password complexity requirements (min 8 chars, mixed case, digit, special char).
- **Input Validation**: Protect against common web vulnerabilities like SQL injection and cross-site scripting (XSS).
- **Atomic Stock Operations**: Thread-safe inventory updates within database transactions.

## Architecture

<p align="center">
  </br>
  <img width="60%" height="60%" src="https://github.com/piashcse/ktor-E-Commerce/blob/master/screenshots/onion_architecture.png" />
</p>
<p align="center">
<b>Fig. Clean Architecture </b>
</p>

## Built With 🛠

- [Ktor](https://ktor.io/docs/welcome.html) - Ktor is a framework to easily build connected applications – web
  applications, HTTP services, mobile and browser applications. Modern connected applications need to be asynchronous to
  provide the best experience to users, and Kotlin Coroutines provides awesome facilities to do it in an easy and
  straightforward way.
- [Exposed](https://github.com/JetBrains/Exposed) - Exposed is a lightweight SQL library on top of JDBC driver for
  Kotlin language. Exposed has two flavors of database access: typesafe SQL wrapping DSL and lightweight Data Access
  Objects (DAO).
- [PostgreSQL](https://www.postgresql.org/) - PostgreSQL is a powerful, open-source object-relational database system
  that uses and extends the SQL language combined with many features that safely store and scale the most complicated
  data workloads.
- [Koin](https://github.com/InsertKoinIO/koin) - A pragmatic lightweight dependency injection framework for Kotlin &
  Kotlin Multiplatform.
- [Kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime) - A multiplatform Kotlin library for working with date
  and time.
- [Bcrypt](https://github.com/patrickfav/bcrypt) - A Java standalone implementation of the bcrypt password hash
  function. Based on the Blowfish cipher it is the default password hash algorithm for OpenBSD and other systems
  including some Linux distributions.
- [Apache Commons Email](https://github.com/apache/commons-email) - Apache Commons Email aims to provide an API for
  sending email. It is built on top of the JavaMail API, which it aims to simplify.
- [Ktor OpenAPI/Swagger](https://ktor.io/docs/server-openapi.html) - Ktor's built-in OpenAPI generation and Swagger UI.
- [Valiktor](https://github.com/valiktor/valiktor) - Valiktor is a type-safe, powerful and extensible fluent DSL to
  validate objects in Kotlin.

## Requirements

- [JAVA 11](https://jdk.java.net/11/) (or latest)
- [PostgreSQL](https://www.postgresql.org/) (latest)

## Clone the repository

```bash
git clone git@github.com:piashcse/ktor-E-Commerce.git
```

Note: some installation instructions are for mac, for windows/linux please install accordingly.

## Environment Configuration

This project uses DotEnv for configuration management. Follow these steps to set up your environment:

1. Create a `.env` file in the project root directory:
   ```bash
   touch .env
   ```

2. Add the following variables to your `.env` file:

   ```env
   # Database Configuration
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=ktor-ecommerce
   DB_USER=postgres
   DB_PASSWORD=p123

   # Server Configuration
   PORT=8080
   HOST=localhost

   # JWT Configuration
   JWT_SECRET=your-super-secret-jwt-secret-key-change-in-production
   JWT_ISSUER=ktor-ecommerce-app
   JWT_AUDIENCE=ktor-ecommerce
   JWT_REALM=ktor-ecommerce

   # Email Configuration
   EMAIL_HOST=smtp.gmail.com
   EMAIL_PORT=587
   EMAIL_USERNAME=your-email@gmail.com
   EMAIL_PASSWORD=your-gmail-app-password
   ```

3. Update the values as needed for your environment, especially:
   - Database credentials to match your PostgreSQL setup
   - JWT secret with a strong, unique value for production
   - Email credentials with your actual Gmail and app password

The `.env` file is included in `.gitignore` to prevent sensitive information from being committed to the repository.

> **Note**: For email configuration, make sure to use a Gmail app password rather than your regular Gmail password. You can generate an app password in your Google Account settings under Security > 2-Step Verification > App passwords.

## PgAdmin Setup

On Terminal

```
brew install --cask pgadmin4
```

Open PgAdmin

In the "Create - Server" dialog that appears, fill in the following information:

General tab:

- Name: Give your server a name (e.g., "Ktor Ecommerce App")

Connection tab:

- Host name/address: localhost (if your PostgreSQL server is on the same machine)
- Port: 5432 (default PostgreSQL port)
- Maintenance database: postgres (default database)
- Username: piashcse (the user you created for your application)
- Password: p123 (the password you set for piashcse)

![server1](screenshots/ktor-postgres.png)



## 📧 SMTP Email Setup

This project uses Gmail’s SMTP service to send emails (e.g., for password recovery). The email configuration is managed through environment variables in your `.env` file.

### 🔧 SMTP Configuration
Configure your email settings in the `.env` file:

- `EMAIL_HOST`: SMTP server host (e.g., `smtp.gmail.com`)
- `EMAIL_PORT`: SMTP server port (e.g., `587` for TLS)
- `EMAIL_USERNAME`: Your email address
- `EMAIL_PASSWORD`: Your email app password

> **Important**: Use Gmail app passwords instead of your regular Gmail password. Generate an app password in your Google Account settings under Security > 2-Step Verification > App passwords.

## API Documentation & OpenAPI Specification

The application includes built-in API documentation and OpenAPI specification generation:

### Accessing API Documentation

- **Swagger UI**: Access interactive API documentation at the root `/` or `/swagger` endpoint when the application is running. The root URL automatically redirects to Swagger for easy API discovery.
- **Raw OpenAPI Specification**: Get the OpenAPI JSON specification at `/openapi` endpoint.

### Generating OpenAPI Specification

The static OpenAPI specification file is located at `src/main/resources/openapi/openapi.json` and includes documentation for all the main API endpoints.

To generate an updated OpenAPI specification file based on the current code:

```bash
./gradlew transformOpenApiJson
```

This will generate an updated OpenAPI specification and save it to `src/main/resources/openapi/openapi.json`.

**Note**: The OpenAPI generation feature is experimental in Ktor 3.x. The static specification file in resources provides stable documentation for all endpoints.

## Run the project

On Terminal

```
./gradlew run
```

## API Response Format

This API follows industry-standard patterns (Stripe, GitHub, OpenAI) for clean, predictable responses.

### Success Responses

- **HTTP status code indicates success** (200, 201, 204)
- **Response body contains data directly** - no wrapper objects
- No `isSuccess` or `statusCode` fields needed

**Example (200 OK):**
```json
{
  "id": "ce563774-d3d5-442e-ad1a-b884bb0a53f0",
  "email": "customer@gmail.com",
  "userType": "customer"
}
```

### Error Responses

**Standard Error (400/401/403/404/500):**
```json
{
  "message": "Invalid email or password"
}
```

**Validation Error (400):**
```json
{
  "message": "Validation failed",
  "errors": [
    {"field": "email", "message": "Invalid email format"},
    {"field": "password", "message": "Password must be at least 8 characters with uppercase, lowercase, digit, and special character"}
  ]
}
```

### Pagination

All collection-based endpoints support standardized pagination using `limit` and `offset` query parameters.

- **`limit`**: Maximum number of items to return (default: 20)
- **`offset`**: Number of items to skip (default: 0)

**Paginated Response Format:**
```json
{
  "data": [
    { ... item 1 ... },
    { ... item 2 ... }
  ],
  "metadata": {
    "totalCount": 100,
    "limit": 20,
    "skip": 0
  }
}
```

### Common Error Codes

| Status Code | Description | Example Message |
|-------------|-------------|-----------------|
| `400` | Bad Request | `"Invalid email address"` |
| `401` | Unauthorized | `"Authentication required"` |
| `403` | Forbidden | `"Insufficient permissions"` |
| `404` | Not Found | `"Product not found"` |
| `409` | Conflict | `"User already exists with this email"` |
| `410` | Gone | `"API version v0 for 'product' is no longer available"` |
| `500` | Internal Server Error | `"Internal server error"` |

### Message Constants

All error messages are centralized in `Message.kt` for consistency and maintainability:
- **Validation**: Field-level validation messages
- **Auth**: Authentication and authorization errors  
- **Orders/Products/Shops**: Domain-specific errors
- **General**: Common error messages

This ensures consistent, actionable error messages across all endpoints.

## Documentation

### 📖 MkDocs Documentation

Comprehensive API documentation is maintained using [MkDocs](https://www.mkdocs.org/) and available in the `docs/` directory. To view the documentation locally:

1. Install MkDocs: `pip install mkdocs-material`
2. Run the documentation server: `mkdocs serve`
3. Open [http://localhost:8000](http://localhost:8000) in your browser

### 🔗 API Endpoints

The API is organized into the following modules:

- **Authentication** - Login, register, OTP, password reset
- **Profile** - User profile management
- **Shop** - Shop management for sellers
- **Product** - Product catalog management
- **Cart** - Shopping cart operations and summary
- **Order** - Order processing, cancellation, and management
- **Payment** - Payment processing and history
- **Refund Request** - Refund lifecycle management
- **Inventory** - Stock management
- **Wishlist** - Product wishlist
- **Shipping** - Shipping management
- **Review & Rating** - Product reviews
- **Brand** - Brand management
- **Product Category** - Product categories
- **Shop Category** - Shop categories
- **Privacy Policy** - Policy management
- **Policy Consent** - User consent tracking

### 🌐 Swagger/OpenAPI

Interactive API documentation is available via Swagger UI at `http://localhost:8080/swagger` when the application is running.

### ROLE MANAGEMENT

<details>
<summary>Admin, Seller, Customer </summary>

- <b>Customer Role:</b> Basic shoppers who can browse products, manage their cart, wishlist, shipping addresses, and place orders.
- <b>Seller Role:</b> Vendors who can manage their own shops, list products, handle inventory, and manage orders and refund requests for their products.
- <b>Admin Role:</b> Platform administrators with full access to manage all users, shops, categories, brands, coupons, shipping methods, and system-wide settings.

</details>

### AUTH

<details>
<summary> <code>POST</code> <code>/api/v1/auth/login</code></summary>

### Description
Authenticate user with email, password and user type.
> **Security**: Rate-limited to 5 requests per 10 minutes.

### Curl
```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/auth/login' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "email": "customer@gmail.com",
  "password": "p123",
  "userType": "customer"
}'
```
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/auth/register</code></summary>

### Description
Register a new user account.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/auth/otp-verification</code></summary>

### Description
Verify user account or password reset with OTP.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/auth/forget-password</code></summary>

### Description
Request password reset OTP.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/auth/reset-password</code></summary>

### Description
Reset password using OTP verification.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/auth/refresh-token</code></summary>

### Description
Refresh access token using refresh token.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/auth/logout</code></summary>

### Description
Logout authenticated user and revoke refresh token.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/auth/change-password</code></summary>

### Description
Change password for authenticated user.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/admin/auth/{userId}/change-user-type</code></summary>

### Description
Admin: Change user role (CUSTOMER, SELLER, ADMIN).
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/admin/auth/{userId}/deactivate</code></summary>

### Description
Admin: Deactivate a user account.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/admin/auth/{userId}/activate</code></summary>

### Description
Admin: Activate a user account.
</details>

### PROFILE

<details>
<summary> <code>GET</code> <code>/api/v1/profile</code></summary>

### Description
Retrieve the authenticated user's profile information.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/profile</code></summary>

### Description
Update the authenticated user's profile information.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/profile/image-upload</code></summary>

### Description
Upload a profile image.
</details>

### SHOP CATEGORY

<details>
<summary> <code>POST</code> <code>/api/v1/admin/shop-category</code></summary>

### Description
Admin: Create a new shop category.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/admin/shop-category/{id}</code></summary>

### Description
Admin: Update an existing shop category name.
</details>

<details>
<summary> <code>DELETE</code> <code>/api/v1/admin/shop-category/{id}</code></summary>

### Description
Admin: Permanently delete a shop category.
</details>

### SHOP

<details>
<summary> <code>GET</code> <code>/api/v1/shop/{id}</code></summary>

### Description
Retrieve detailed information about a specific shop.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/shop/public</code></summary>

### Description
Retrieve public shops with filters (status, category).
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/shop/category/{categoryId}</code></summary>

### Description
Retrieve shops by category.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/shop/featured</code></summary>

### Description
Retrieve featured shops.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/seller/shop</code></summary>

### Description
Seller: Create a new shop.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/seller/shop</code></summary>

### Description
Seller: Retrieve owned shops.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/seller/shop/{id}</code></summary>

### Description
Seller: Update shop details.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v2/seller/shop/{shopId}</code></summary>

### Description
Seller: Update shop details with optimized response.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/admin/shop/status</code></summary>

### Description
Admin: Retrieve shops filtered by status.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/admin/shop/approve/{id}</code></summary>

### Description
Admin: Approve a pending shop application.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/admin/shop/reject/{id}</code></summary>

### Description
Admin: Reject a shop application.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/admin/shop/suspend/{id}</code></summary>

### Description
Admin: Suspend an active shop.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/admin/shop/activate/{id}</code></summary>

### Description
Admin: Activate a suspended shop.
</details>

### BRAND

<details>
<summary> <code>GET</code> <code>/api/v1/brand</code></summary>

### Description
Retrieve a paginated list of all brands.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/admin/brand</code></summary>

### Description
Admin: Create a new brand.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/admin/brand/{id}</code></summary>

### Description
Admin: Update an existing brand.
</details>

<details>
<summary> <code>DELETE</code> <code>/api/v1/admin/brand/{id}</code></summary>

### Description
Admin: Delete a brand.
</details>

### PRODUCT CATEGORY

<details>
<summary> <code>GET</code> <code>/api/v1/product-category</code></summary>

### Description
Retrieve a paginated list of all product categories.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/admin/product-category</code></summary>

### Description
Admin: Create a new product category.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/admin/product-category/{id}</code></summary>

### Description
Admin: Update an existing product category.
</details>

<details>
<summary> <code>DELETE</code> <code>/api/v1/admin/product-category/{id}</code></summary>

### Description
Admin: Permanently delete a product category.
</details>

### PRODUCT SUB CATEGORY

<details>
<summary> <code>GET</code> <code>/api/v1/product-subcategory</code></summary>

### Description
Retrieve subcategories for a specific category (require categoryId).
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/admin/product-subcategory</code></summary>

### Description
Admin: Create a new product subcategory.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/admin/product-subcategory/{id}</code></summary>

### Description
Admin: Update an existing product subcategory name.
</details>

<details>
<summary> <code>DELETE</code> <code>/api/v1/admin/product-subcategory/{id}</code></summary>

### Description
Admin: Permanently delete a product subcategory.
</details>

### PRODUCT

<details>
<summary> <code>GET</code> <code>/api/v1/product/{id}</code></summary>

### Description
Retrieve detailed information about a specific product.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/product</code></summary>

### Description
Retrieve a paginated list of products with optional filters (maxPrice, minPrice, categoryId, subCategoryId, brandId, sortBy, sortOrder).
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/product/search</code></summary>

### Description
Search for products by name.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/seller/product</code></summary>

### Description
Seller: Retrieve seller products with filters.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/seller/product</code></summary>

### Description
Seller: Add a new product listing.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/seller/product/{id}</code></summary>

### Description
Seller: Update an existing product listing.
</details>

<details>
<summary> <code>DELETE</code> <code>/api/v1/seller/product/{id}</code></summary>

### Description
Seller: Permanently delete a product listing.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/seller/product/image-upload</code></summary>

### Description
Seller: Upload a product image.
</details>

<details>
<summary> <code>DELETE</code> <code>/api/v1/admin/product/{id}</code></summary>

### Description
Admin: Permanently delete any product.
</details>

### REVIEW RATING

<details>
<summary> <code>GET</code> <code>/api/v1/review-rating</code></summary>

### Description
Retrieve reviews and ratings for a specific product.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/review-rating</code></summary>

### Description
Submit a new review and rating for a product.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/review-rating/{id}</code></summary>

### Description
Update an existing review and rating.
</details>

<details>
<summary> <code>DELETE</code> <code>/api/v1/review-rating/{id}</code></summary>

### Description
Delete a review and rating.
</details>

### CART

<details>
<summary> <code>POST</code> <code>/api/v1/cart</code></summary>

### Description
Add an item to the authenticated user's cart.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/cart</code></summary>

### Description
Retrieve all items in the authenticated user's cart.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/cart/update</code></summary>

### Description
Update the quantity of an item in the cart.
</details>

<details>
<summary> <code>DELETE</code> <code>/api/v1/cart/remove</code></summary>

### Description
Remove a specific item from the cart (require productId).
</details>

<details>
<summary> <code>DELETE</code> <code>/api/v1/cart/all</code></summary>

### Description
Remove all items from the authenticated user's cart.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/cart/summary</code></summary>

### Description
Retrieve a summary of the cart (totals, counts).
</details>

### WISHLIST

<details>
<summary> <code>POST</code> <code>/api/v1/wishlist</code></summary>

### Description
Add a product to the authenticated user's wishlist.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/wishlist</code></summary>

### Description
Retrieve all items in the user's wishlist.
</details>

<details>
<summary> <code>DELETE</code> <code>/api/v1/wishlist/remove</code></summary>

### Description
Remove a specific product from the wishlist.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/wishlist/check</code></summary>

### Description
Check if a specific product is in the user's wishlist.
</details>

### CHECKOUT

<details>
<summary> <code>POST</code> <code>/api/v1/checkout/shipping-address</code></summary>

### Description
Add a new shipping address for the authenticated user.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/checkout/shipping-address</code></summary>

### Description
Retrieve all shipping addresses for the authenticated user.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/checkout/shipping-address/{id}</code></summary>

### Description
Update an existing shipping address.
</details>

<details>
<summary> <code>DELETE</code> <code>/api/v1/checkout/shipping-address/{id}</code></summary>

### Description
Delete a shipping address.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/checkout/shipping-method</code></summary>

### Description
Retrieve all available shipping methods.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/checkout/summary</code></summary>

### Description
Get a summary of the checkout (totals) without placing an order.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/checkout/place-order</code></summary>

### Description
Place a new order from the cart.
</details>

### ORDER

<details>
<summary> <code>GET</code> <code>/api/v1/order</code></summary>

### Description
Retrieve all orders for the authenticated customer.
</details>

<details>
<summary> <code>PATCH</code> <code>/api/v1/order/status/{id}</code></summary>

### Description
Update order status (Customer: CANCELED/RECEIVED, Seller: CONFIRMED/DELIVERED).
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/order/{id}/cancel</code></summary>

### Description
Cancel an order.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/seller/order</code></summary>

### Description
Seller: Retrieve orders for the seller's shop.
</details>

<details>
<summary> <code>PATCH</code> <code>/api/v1/admin/order/status/{id}</code></summary>

### Description
Admin: Update the status of any order.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/admin/order/{id}/cancel</code></summary>

### Description
Admin: Cancel any order.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/admin/order</code></summary>

### Description
Admin: Retrieve all orders with advanced filtering (status, startDate, endDate).
</details>

### PAYMENT

<details>
<summary> <code>POST</code> <code>/api/v1/payment</code></summary>

### Description
Create a new payment record for an order.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/payment/{id}</code></summary>

### Description
Retrieve payment details by ID.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/payment/order/{orderId}</code></summary>

### Description
Retrieve all payments for a specific order.
</details>

### PRIVACY POLICY

<details>
<summary> <code>GET</code> <code>/api/v1/policy/{policyType}</code></summary>

### Description
Retrieve the latest active version of a policy by type.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/admin/policy</code></summary>

### Description
Admin: Create a new policy document or new version.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/admin/policy/{policyType}/history</code></summary>

### Description
Admin: Retrieve all versions of a specific policy type.
</details>

### PRIVACY POLICY CONSENT

<details>
<summary> <code>POST</code> <code>/api/v1/policy-consents/consent</code></summary>

### Description
Record user consent for a specific policy document.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/policy-consents</code></summary>

### Description
Retrieve all consent records for the authenticated user.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/policy-consents/{policyType}</code></summary>

### Description
Check if the user has consented to a specific policy type.
</details>

### REFUND REQUEST

<details>
<summary> <code>POST</code> <code>/api/v1/refund-requests/{orderId}</code></summary>

### Description
Create a refund request for an order item.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/refund-requests/{id}/ship</code></summary>

### Description
Mark an approved refund as shipped.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/refund-requests/order/{orderId}</code></summary>

### Description
Get refund requests for an order.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/refund-requests/{id}</code></summary>

### Description
Get refund request details.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/seller/refund-requests/{id}/status</code></summary>

### Description
Seller: Update refund request status.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/admin/refund-requests/order/{orderId}</code></summary>

### Description
Admin: Get all refund requests for an order.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/admin/refund-requests/{id}/status</code></summary>

### Description
Admin: Update refund status.
</details>

### COUPON

<details>
<summary> <code>GET</code> <code>/api/v1/coupon/{code}</code></summary>

### Description
Retrieve detailed information about a coupon by its code.
</details>

<details>
<summary> <code>POST</code> <code>/api/v1/admin/coupon</code></summary>

### Description
Admin: Create a new discount coupon.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/admin/coupon</code></summary>

### Description
Admin: Retrieve a list of all coupons.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/admin/coupon/{id}</code></summary>

### Description
Admin: Update an existing coupon.
</details>

<details>
<summary> <code>DELETE</code> <code>/api/v1/admin/coupon/{id}</code></summary>

### Description
Admin: Delete a coupon.
</details>

### INVENTORY

<details>
<summary> <code>POST</code> <code>/api/v1/seller/inventory</code></summary>

### Description
Seller: Initialize or update inventory for a product.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/seller/inventory/stock/{productId}</code></summary>

### Description
Seller: Update stock quantity.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/seller/inventory/product/{productId}</code></summary>

### Description
Seller: Retrieve inventory item details by product ID.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/seller/inventory/shop/{shopId}</code></summary>

### Description
Seller: Retrieve all inventory items for a shop.
</details>

<details>
<summary> <code>GET</code> <code>/api/v1/seller/inventory/low-stock</code></summary>

### Description
Seller: Retrieve items with stock below a threshold.
</details>


### SHIPPING METHOD

<details>
<summary> <code>POST</code> <code>/api/v1/admin/shipping-method</code></summary>

### Description
Admin: Create a new shipping method.
</details>

<details>
<summary> <code>PUT</code> <code>/api/v1/admin/shipping-method/{id}</code></summary>

### Description
Admin: Update an existing shipping method.
</details>

<details>
<summary> <code>DELETE</code> <code>/api/v1/admin/shipping-method/{id}</code></summary>

### Description
Admin: Delete a shipping method.
</details>




## 👨 Developed By

<a href="https://twitter.com/piashcse" target="_blank">
  <img src="https://avatars.githubusercontent.com/piashcse" width="90" align="left">
</a>

**Mehedi Hassan Piash**

[![Twitter](https://img.shields.io/badge/-Twitter-1DA1F2?logo=x&logoColor=white&style=for-the-badge)](https://twitter.com/piashcse)
[![Medium](https://img.shields.io/badge/-Medium-00AB6C?logo=medium&logoColor=white&style=for-the-badge)](https://medium.com/@piashcse)
[![Linkedin](https://img.shields.io/badge/-LinkedIn-0077B5?logo=linkedin&logoColor=white&style=for-the-badge)](https://www.linkedin.com/in/piashcse/)
[![Web](https://img.shields.io/badge/-Web-0073E6?logo=appveyor&logoColor=white&style=for-the-badge)](https://piashcse.github.io/)
[![Blog](https://img.shields.io/badge/-Blog-0077B5?logo=readme&logoColor=white&style=for-the-badge)](https://piashcse.blogspot.com)

# License

```
Copyright 2023 piashcse (Mehedi Hassan Piash)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
