# Ktor-E-Commerce

[![Ktor](https://img.shields.io/badge/ktor-3.1.3-blue.svg)](https://github.com/ktorio/ktor)
[![Exposed](https://img.shields.io/badge/Exposed-0.61.0-blue.svg)](https://github.com/JetBrains/Exposed)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.21-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
![Koin](https://img.shields.io/badge/Koin-4.0.2-29BEB0?logo=koin&logoColor=white)
[![PostgreSQL Version](https://img.shields.io/badge/PostgreSQL-42.7.4-336791?logo=postgresql)](https://www.postgresql.org/)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
<a href="https://github.com/piashcse"><img alt="License" src="https://img.shields.io/static/v1?label=GitHub&message=piashcse&color=C51162"/></a>

Ktor-E-Commerce is a high-performance backend solution designed for e-commerce applications built
with [ktor](https://ktor.io/docs/welcome.html). This backend leverages the power of Kotlin to provide a robust,
scalable, and efficient service for handling your e-commerce needs.

## Swagger View

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
- **Checkout**: Streamline the checkout process for quick and secure payments.

### 5. Order Management

- **Order Processing**: Handle order creation, status updates, and order history.
- **Payment Integration**: Integrate with popular payment gateways for seamless transactions.

### 6. Scalability and Performance

- **Asynchronous Processing**: Leverage Ktor's async capabilities for high performance.
- **Load Balancing**: Easily scale your application to accommodate increased traffic.

### 7. Security

- **JWT Tokens**: Implement JSON Web Tokens for secure authentication.
- **Input Validation**: Protect against common web vulnerabilities like SQL injection and cross-site scripting (XSS).

## Architecture

<p align="center">
  </br>
  <img width="60%" height="60%" src="https://github.com/piashcse/ktor-E-Commerce/blob/master/screenshots/onion_architecture.png" />
</p>
<p align="center">
<b>Fig.  Onion Architecture </b>
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
- [ktor-openapi-tools](https://github.com/SMILEY4/ktor-swagger-ui) - Kotlin Ktor plugin to generate OpenAPI
  specifications and provides Swagger UI and ReDoc.
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

## Postgres Setup

- Create a database in postgreSQL
- Change your db name, user, and password in `resources/hikari.properties` and replace them accordingly.

```bash
dataSource.user=postgres
dataSource.password=p123
dataSource.databaseName=ktor-1.0.0
```

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

## Run the project

On Terminal

```
./gradlew run
```

## 📧 SMTP Email Setup

This project uses Gmail’s SMTP service to send emails (e.g., for password recovery). Follow the instructions below to configure it securely.

### 🔧 Configuration SMTP Email
Open the file constants/AppConstants.kt and update the SmtpServer object:

```
object SmtpServer {
    const val HOST_NAME = "smtp.googlemail.com"
    const val PORT = 465
    const val DEFAULT_AUTHENTICATOR = "your-email@gmail.com" // Your Gmail address
    const val DEFAULT_AUTHENTICATOR_PASSWORD = "your-app-password" // App-specific password
    const val EMAIL_SUBJECT = "Forget Password"
    const val SENDING_EMAIL = "your-email@gmail.com" // Sender email displayed to recipients
}
```
## Documentation

### ROLE MANAGEMENT

<details>
<summary>Admin, Seller, Customer </summary>

- <b> Customer Role:</b> Customers can browse products, make purchases, and manage their accounts.
- <b> Seller Role:</b>  Sellers can list products, manage their inventory, and view order details.
- <b> Admin Role:</b>  Admins have full access to manage users, roles, and system settings.

</details>

### AUTH

<details>

<summary> <code>POST </code> <code>/auth/login</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/auth/Login' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "email": "customer@gmail.com",
  "password": "p1234",
  "userType": "customer"
}'
```

### Request URL

```
  http://localhost:8080/auth/Login
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "user": {
      "id": "ce563774-d3d5-442e-ad1a-b884bb0a53f0",
      "email": "customer@gmail.com",
      "userType": "customer"
    },
    "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJjZTU2Mzc3NC1kM2Q1LTQ0MmUtYWQxYS1iODg0YmIwYTUzZjAiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI5NTkzMjQ5fQ.XWWEO1NFN3Gysb1Tghm1l1BcQ2NsYexXE2YmgeIvBv_Wq-DXgmihDed1zt3_TAJevM631vtMQ7LtwOXbYhKF9A"
  }
}
```   

</details>

<details>

<summary> <code>POST</code> <code>/auth/register</code></summary>

### Description
Register a new user with the specified email, password, and userType. 
- Users can register with the same email for different roles (customer and seller).
- If a user is already registered as a customer and wants to become a seller, they can register with the same email but different userType.
- If a user is already verified and tries to register with the same email and userType, they will receive a message indicating that the user already exists.

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/auth/register' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "email": "piash@gmail.com",
  "password": "p1234",
  "userType": "admin"
}'
``` 

### Request URL

```
    http://localhost:8080/auth/register
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "f48ec4f9-5482-4a23-9e49-e69f97bd20a6",
    "email": "piash@gmail.com"
  }
}

```   

</details>

<details>

<summary> <code>GET</code> <code>/auth/otp-verification</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/auth/otp-verification?userId=3842e19b-2608-40f8-98bd-6a6b43939fec&otp=560674d' \
  -H 'accept: application/json'
``` 

### Request URL

```
   http://localhost:8080/auth/otp-verification?userId=3842e19b-2608-40f8-98bd-6a6b43939fec&otp=560674d
``` 

### Response

```

Response body
Download
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": true
}

```   

</details>
<details>

<summary><code>GET </code> <code>/auth/forget-password</code></summary>

### Description
Send a password reset verification code to the specified email. If the user has multiple accounts with the same email but different roles, the userType parameter can be used to specify which account to reset the password for.

### Curl

```
 curl -X 'GET' \
  'http://localhost:8080/auth/forget-password?email=piash@gmail.com&userType=customer' \
  -H 'accept: application/json'
``` 

### Request URL

```
http://localhost:8080/auth/forget-password?email=piash@gmail.com&userType=customer

``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "verification code sent to piash@gmail.com"
}

```   

</details>

<details>

<summary><code>GET </code> <code>/auth/reset-password</code></summary>

### Description
Reset the password for a user account using the verification code sent to their email. If the user has multiple accounts with the same email but different roles, the userType parameter can be used to specify which account to reset the password for.

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/auth/reset-password?email=piash599%40gmail.com&otp=9889&newPassword=p1234&userType=customer' \
  -H 'accept: application/json'
``` 

### Request URL

```
http://localhost:8080/auth/reset-password?email=piash599%40gmail.com&otp=9189&newPassword=p1234&userType=customer
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "Password change successful"
}

```   

</details>

<details>

<summary> <code>PUT </code> <code>/auth/change-password</code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/auth/change-password?oldPassword=p1234&newPassword=p1234' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImE5YTY2MmE3LTUwZmUtNGYxMy04ZWFiLTBlMDgxMGZiOTkwOSIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5MzEzMzI3NH0.Jy136YnG5Py4zotIZBr4KvaPblONOu1MVy58iECgyGb4spQjW8Vu_tBwc0frl85Vqup8g3NJlqHIDqLs8f-J0g'
``` 

### Request URL

```
http://localhost:8080/auth/change-password?oldPassword=p1234&newPassword=p1234
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "Password has been changed"
}
```   

</details>

### PROFILE

<details>

<summary> <code>GET</code> <code>/profile</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/profile' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJwaWFzaDU5OUBnbWFpbC5jb20iLCJ1c2VySWQiOiI3MDdhYzI2NC1iZTJlLTRlODktYjZkMy03YTQ5YjE0MjYzZDIiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzQ0NzMyNzg4fQ.xJQw7NLnXzBO5yIAbK3HJtIFge4n0-z-SNk6l9ZmajbHNqFN4NtH-u2Lwt48kbL1W_xc-jUKNmqmhaamLuj9dg'

``` 

### Request URL

```
http://localhost:8080/profile
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "userId": "707ac264-be2e-4e89-b6d3-7a49b14263d2",
    "firstName": "Mehedi ",
    "lastName": "Hassan",
    "mobile": "01812353930",
    "faxNumber": "454",
    "streetAddress": "Dhaka",
    "city": "Dhaka",
    "postCode": "1205",
    "gender": "Malde"
  }
}
```   

</details>


<details>

<summary> <code>PUT</code> <code>/profile</code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/profile?firstName=Mehedi%20&lastName=Hassan&mobile=01812353930&faxNumber=454&streetAddress=Dhaka&city=Dhaka&postCode=1205&gender=Malde' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJwaWFzaDU5OUBnbWFpbC5jb20iLCJ1c2VySWQiOiI3MDdhYzI2NC1iZTJlLTRlODktYjZkMy03YTQ5YjE0MjYzZDIiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzQ0NzMyNzg4fQ.xJQw7NLnXzBO5yIAbK3HJtIFge4n0-z-SNk6l9ZmajbHNqFN4NtH-u2Lwt48kbL1W_xc-jUKNmqmhaamLuj9dg'

``` 

### Request URL

```
http://localhost:8080/profile?firstName=Mehedi%20&lastName=Hassan&mobile=01812353930&faxNumber=454&streetAddress=Dhaka&city=Dhaka&postCode=1205&gender=Malde

``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "userId": "707ac264-be2e-4e89-b6d3-7a49b14263d2",
    "firstName": "Mehedi ",
    "lastName": "Hassan",
    "mobile": "01812353930",
    "faxNumber": "454",
    "streetAddress": "Dhaka",
    "city": "Dhaka",
    "postCode": "1205",
    "gender": "Malde"
  }
}

```   

</details>

<details>

<summary> <code>POST</code> <code>/profile/image-upload</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/profile/image-upload' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJwaWFzaDU5OUBnbWFpbC5jb20iLCJ1c2VySWQiOiI3MDdhYzI2NC1iZTJlLTRlODktYjZkMy03YTQ5YjE0MjYzZDIiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzQ0NzMyNzg4fQ.xJQw7NLnXzBO5yIAbK3HJtIFge4n0-z-SNk6l9ZmajbHNqFN4NtH-u2Lwt48kbL1W_xc-jUKNmqmhaamLuj9dg' \
  -H 'Content-Type: multipart/form-data' \
  -F 'image=@piashdp.jpg;type=image/jpeg'
``` 

### Request URL

```
http://localhost:8080/profile/image-upload

``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "73b21d27-466e-45c6-bc2b-0480eb4db2d2.jpg"
}

```   

</details>

### SHOP CATEGORY

<details>

<summary> <code>POST</code> <code>/shop-category</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/shop-category' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6ImFkbWluQGdtYWlsLmNvbSIsInVzZXJJZCI6IjNhNGFlMDMyLTY4MDEtNDc1Yi05NTFhLTI2MTRmMDRhOWJiMCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE3MjU4MDQwODB9.lE39-L8N1KeSeWIOJkUwoWO5WdMO9fHzhtU4kyOGG0-2eGBtMLNx9T9mfgKagam_qbI8C6E8oteL5r3KHsQP-g' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "New digital Shop"
}'
``` 

### Request URL

```
http://localhost:8080/shop-category
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "28918963-f932-425b-884b-a34d8ae69b2a",
    "name": "New digital Shop"
  }
}
```   

</details>

<details>

<summary> <code>GET </code> <code>/shop-category</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/shop-category?limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6ImFkbWluQGdtYWlsLmNvbSIsInVzZXJJZCI6IjNhNGFlMDMyLTY4MDEtNDc1Yi05NTFhLTI2MTRmMDRhOWJiMCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE3MjU4MDQwODB9.lE39-L8N1KeSeWIOJkUwoWO5WdMO9fHzhtU4kyOGG0-2eGBtMLNx9T9mfgKagam_qbI8C6E8oteL5r3KHsQP-g'
``` 

### Request URL

```
http://localhost:8080/shop-category?limit=10
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": [
    {
      "id": "9c95c44c-3767-4ca2-9486-e28e390b3741",
      "name": "New Electronics"
    }
  ]
}
```   

</details>

<details>

<summary> <code>DELETE</code> <code>/shop-category/{id}</code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/shop-category/9c95c44c-3767-4ca2-9486-e28e390b3741' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjdhMGQ5YTU0LTIzZDctNGY5Yy05YWI2LTgwYzQ3Mzg4MDVlNCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTMzNjg0OTl9.0KnZ9PyQ9XMbxjCaOKsDKyk7lWvwxv4weQDi9wmhHJpaXhqRvZYxU43RzdmuGmxJwnLpT32fe-rwwvkl1IOPpQ'
``` 

### Request URL

```
http://localhost:8080/shop-category/2a17da31-7517-41db-b7d3-f77d0ddd52a5
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "2a17da31-7517-41db-b7d3-f77d0ddd52a5"
}
```   

</details>


<details>

<summary> <code>PUT </code> <code>/shop-category/{id}</code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/shop-category/9c95c44c-3767-4ca2-9486-e28e390b3741?name=Piash%20Digital%20shop' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6ImFkbWluQGdtYWlsLmNvbSIsInVzZXJJZCI6IjNhNGFlMDMyLTY4MDEtNDc1Yi05NTFhLTI2MTRmMDRhOWJiMCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE3MjU4MDQwODB9.lE39-L8N1KeSeWIOJkUwoWO5WdMO9fHzhtU4kyOGG0-2eGBtMLNx9T9mfgKagam_qbI8C6E8oteL5r3KHsQP-g'
``` 

### Request URL

```
http://localhost:8080/shop-category/9c95c44c-3767-4ca2-9486-e28e390b3741?name=Piash%20Digital%20shop
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "9c95c44c-3767-4ca2-9486-e28e390b3741",
    "name": "Piash Digital shop"
  }
}
```   

</details>

### SHOP

<details>
<summary> <code>POST</code> <code>/shop</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/shop?name=Royal%20Shop&categoryId=5e67ec97-9ed6-48ee-9d56-4163fe1711cb' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjdhMGQ5YTU0LTIzZDctNGY5Yy05YWI2LTgwYzQ3Mzg4MDVlNCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTMzNjg0OTl9.0KnZ9PyQ9XMbxjCaOKsDKyk7lWvwxv4weQDi9wmhHJpaXhqRvZYxU43RzdmuGmxJwnLpT32fe-rwwvkl1IOPpQ' \
  -d ''
``` 

### Request URL

```
http://localhost:8080/shop?name=Royal%20Shop&categoryId=5e67ec97-9ed6-48ee-9d56-4163fe1711cb
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "cbfdcfa3-fb65-4fa3-9078-e0f8cc63ddbc",
    "name": "Royal Shop"
  }
}
```   

</details>

<details>
<summary> <code>Get</code> <code>/shop</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/shop?limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6ImFkbWluQGdtYWlsLmNvbSIsInVzZXJJZCI6IjNhNGFlMDMyLTY4MDEtNDc1Yi05NTFhLTI2MTRmMDRhOWJiMCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE3MjU4MDQwODB9.lE39-L8N1KeSeWIOJkUwoWO5WdMO9fHzhtU4kyOGG0-2eGBtMLNx9T9mfgKagam_qbI8C6E8oteL5r3KHsQP-g'
``` 

### Request URL

```
http://localhost:8080/shop?limit=10
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": [
    {
      "id": "a33b8912-e0b2-4058-9d7b-3c7ef9b935c7",
      "name": "Piash Shop update",
      "categoryId": "9c95c44c-3767-4ca2-9486-e28e390b3741"
    }
  ]
}
```   

</details>

<details>
<summary> <code>PUT</code> <code>/shop/{id}</code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/shop/a33b8912-e0b2-4058-9d7b-3c7ef9b935c7?name=Shop%20again%20update' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6ImFkbWluQGdtYWlsLmNvbSIsInVzZXJJZCI6IjNhNGFlMDMyLTY4MDEtNDc1Yi05NTFhLTI2MTRmMDRhOWJiMCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE3MjU4MDQwODB9.lE39-L8N1KeSeWIOJkUwoWO5WdMO9fHzhtU4kyOGG0-2eGBtMLNx9T9mfgKagam_qbI8C6E8oteL5r3KHsQP-g'
``` 

### Request URL

```
http://localhost:8080/shop/a33b8912-e0b2-4058-9d7b-3c7ef9b935c7?name=Shop%20again%20update
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "a33b8912-e0b2-4058-9d7b-3c7ef9b935c7",
    "name": "Shop again update",
    "categoryId": "9c95c44c-3767-4ca2-9486-e28e390b3741"
  }
}
```   

</details>

<details>
<summary> <code>DELETE</code> <code>/shop/{id}</code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/shop/d2836959-6bc5-49d0-bd98-e73255a915c5' \
  -H 'accept: application/json'
``` 

### Request URL

```
http://localhost:8080/shop/d2836959-6bc5-49d0-bd98-e73255a915c5

``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "d2836959-6bc5-49d0-bd98-e73255a915c5",
  }
}
```   

</details>

### PRODUCT

<details>

<summary> <code>POST</code> <code>/product</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/product' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJzZWxsZXJAZ21haWwuY29tIiwidXNlcklkIjoiNjY0NDdhNTctYWY2ZS00ZjI1LWE0NmUtMWVkMGUxMTE0ZDhkIiwidXNlclR5cGUiOiJzZWxsZXIiLCJleHAiOjE3NDQ0ODQ4NzB9.nYRrGaSH5rdzclhfRquQ8ZRaaGFifnu-7oLQ-IvY9_HAygbhSjDXMDtDl2VK3UuGr9vr64-pGgcY-nPCs1AaQg' \
  -H 'Content-Type: application/json' \
  -d '{
  "brandId": null,
  "categoryId": "b4f08aae-b1af-4617-963a-b0b9d1187646",
  "description": "Good watch",
  "discountPrice": null,
  "featured": true,
  "hotDeal": true,
  "images": [
    "string"
  ],
  "name": "Smart watch",
  "price": 100,
  "productCode": null,
  "status": null,
  "stockQuantity":1,
  "subCategoryId": null,
  "videoLink": null
}'
``` 

### Request URL

```
http://localhost:8080/product
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "718f0b9a-24ef-450f-9126-7d3d9b27cad5",
    "categoryId": "b4f08aae-b1af-4617-963a-b0b9d1187646",
    "name": "Smart watch",
    "description": "Good watch",
    "minOrderQuantity": 1,
    "stockQuantity": 1,
    "price": 100,
    "hotDeal": true,
    "featured": true,
    "images": "[string]",
    "status": "ACTIVE"
  }
}
```   

</details>

<details>

<summary> <code>GET</code> <code>/product/{id}</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/product/79a97389-78d5-4dff-a1f7-13bc7ae10a8d' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjA1ZjA4MWIxLWRhY2UtNDllMy1iMmIyLTIxNmE3N2U5NjUxYyIsInVzZXJUeXBlIjoic2VsbGVyIiwiZXhwIjoxNjkzMzcwMjUxfQ.aUj7fEXcNtKP_XdKVI6ICk5GlnTVivxhOkZ8S7_l3NExzIAT93QjuoFiNCDs873OEVO66cEUiSSWjkJVDmzMuA'
``` 

### Request URL

```
http://localhost:8080/product/79a97389-78d5-4dff-a1f7-13bc7ae10a8d
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "718f0b9a-24ef-450f-9126-7d3d9b27cad5",
    "categoryId": "b4f08aae-b1af-4617-963a-b0b9d1187646",
    "name": "Smart watch",
    "description": "Good watch",
    "minOrderQuantity": 1,
    "stockQuantity": 1,
    "price": 100,
    "hotDeal": true,
    "featured": true,
    "images": "[string]",
    "status": "ACTIVE"
  }
}
```   

</details>

<details>

<summary> <code>PUT </code> <code>/product/{id}</code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/product/718f0b9a-24ef-450f-9126-7d3d9b27cad5' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjA1ZjA4MWIxLWRhY2UtNDllMy1iMmIyLTIxNmE3N2U5NjUxYyIsInVzZXJUeXBlIjoic2VsbGVyIiwiZXhwIjoxNjkzMzcwMjUxfQ.aUj7fEXcNtKP_XdKVI6ICk5GlnTVivxhOkZ8S7_l3NExzIAT93QjuoFiNCDs873OEVO66cEUiSSWjkJVDmzMuA' \
  -H 'Content-Type: application/json' \
  -d '{

  "name": "Smartch watch",
  "detail":"Xiaomi Smart Watch"

}'
``` 

### Request URL

```
http://localhost:8080/product/718f0b9a-24ef-450f-9126-7d3d9b27cad5
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "718f0b9a-24ef-450f-9126-7d3d9b27cad5",
    "categoryId": "b4f08aae-b1af-4617-963a-b0b9d1187646",
    "name": "Smart watch",
    "description": "Good watch",
    "minOrderQuantity": 1,
    "stockQuantity": 1,
    "price": 100,
    "hotDeal": true,
    "featured": true,
    "images": "[string]",
    "status": "ACTIVE"
  }
}
```   

</details>


<details>

<summary> <code>GET </code> <code>/product</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/product?limit=10&maxPrice=100&minPrice=0' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjA1ZjA4MWIxLWRhY2UtNDllMy1iMmIyLTIxNmE3N2U5NjUxYyIsInVzZXJUeXBlIjoic2VsbGVyIiwiZXhwIjoxNjkzMzcwMjUxfQ.aUj7fEXcNtKP_XdKVI6ICk5GlnTVivxhOkZ8S7_l3NExzIAT93QjuoFiNCDs873OEVO66cEUiSSWjkJVDmzMuA'
``` 

### Request URL

```
http://localhost:8080/product?limit=10&maxPrice=100&minPrice=0
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": [
    {
     "id": "718f0b9a-24ef-450f-9126-7d3d9b27cad5",
    "categoryId": "b4f08aae-b1af-4617-963a-b0b9d1187646",
    "name": "Smart watch",
    "description": "Good watch",
    "minOrderQuantity": 1,
    "stockQuantity": 1,
    "price": 100,
    "hotDeal": true,
    "featured": true,
    "images": "[string]",
    "status": "ACTIVE"
    }
  ]
}
```   

</details>


<details>

<summary> <code>DELETE</code> <code>/product/{id}</code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/product?id=79a97389-78d5-4dff-a1f7-13bc7ae10a8d' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjA1ZjA4MWIxLWRhY2UtNDllMy1iMmIyLTIxNmE3N2U5NjUxYyIsInVzZXJUeXBlIjoic2VsbGVyIiwiZXhwIjoxNjkzMzcwMjUxfQ.aUj7fEXcNtKP_XdKVI6ICk5GlnTVivxhOkZ8S7_l3NExzIAT93QjuoFiNCDs873OEVO66cEUiSSWjkJVDmzMuA'
``` 

### Request URL

```
http://localhost:8080/product?productId=79a97389-78d5-4dff-a1f7-13bc7ae10a8d
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "79a97389-78d5-4dff-a1f7-13bc7ae10a8d"
}
```   

</details>

<details>

<summary> <code>POST</code> <code>/product/image-upload</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/product/photo-upload?id=71b26dd9-b4b5-4f87-a84d-c8daa506018a' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjEyYzA5OGFhLTYzMWYtNDJlZC05MjAzLTFkMjdlMDA0MmY4YSIsInVzZXJUeXBlIjoic2VsbGVyIiwiZXhwIjoxNjkzMzc3NTMyfQ.GeBDEnWNm84mHPhCxTCXUwRSmRo7KjkJ6AfuEXZNiiqKVGtof1xNi8tsBp53L9jbyYwK49HQnDpe6tb0nVwhHA' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@307116353_3302565903398527_525569142260037778_n.png;type=image/png'
``` 

### Request URL

```
http://localhost:8080/product/image-upload?id=71b26dd9-b4b5-4f87-a84d-c8daa506018a
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "cc38e31e-3a7f-435c-9e86-293daf0d6877",
    "imageUrl": "bf68a3f9-d131-4bee-bbbc-80264a3da437.png"
  }
}
```   

</details>

### PRODUCT CATEGORY

<details>

<summary> <code>POST</code> <code>/product-category</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/product-category?categoryName=Kids' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM2Mjg0MTJ9.S6EML9bKGau9HB0CE9v_Sm0rCTOi0eQzRhjd-KI6ChF8n95RC9cTwphWyUisK3tKYuS5ZwXIHfmNvup2zRK5BQ' \
  -d ''
``` 

### Request URL

```
http://localhost:8080/product-category?name=Kids
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "75b44e08-2c94-438f-b500-b204c7c90cca",
    "name": "Kids",
    "subCategories": []
  }
}
```   

</details>


<details>

<summary> <code>GET </code> <code>/product-category</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/product-category?limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM2Mjg0MTJ9.S6EML9bKGau9HB0CE9v_Sm0rCTOi0eQzRhjd-KI6ChF8n95RC9cTwphWyUisK3tKYuS5ZwXIHfmNvup2zRK5BQ'
``` 

### Request URL

```
http://localhost:8080/product-category?limit=10
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": [
    {
      "id": "58f5c085-d04a-47de-beab-1d476b6ce432",
      "name": "Mens Cloth",
      "subCategories": []
    },
    {
      "id": "75b44e08-2c94-438f-b500-b204c7c90cca",
      "name": "Kids",
      "subCategories": []
    }
  ]
}
```   

</details>

<details>

<summary> <code>PUT </code> <code>/product-category/{id}</code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/product-category/b8ccc13f-e118-4540-8e9e-5eaa8028cb4f?name=Education%203.0' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJhZG1pbkBnbWFpbC5jb20iLCJ1c2VySWQiOiIzYTRhZTAzMi02ODAxLTQ3NWItOTUxYS0yNjE0ZjA0YTliYjAiLCJ1c2VyVHlwZSI6ImFkbWluIiwiZXhwIjoxNzI4OTExNTEyfQ._VBrVUeHJ2gOpCvmNHLhkjn5RMZUpf0B35uy_0k1cAQMMuDG7EDgGHzG3eq6PWpNYjMKBeNRrIByGG0lWSYpjg'
``` 

### Request URL

```
http://localhost:8080/product-category/b8ccc13f-e118-4540-8e9e-5eaa8028cb4f?name=Education%203.0
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
    "name": "Education 3.0",
    "subCategories": []
  }
}
```   

</details>

<details>

<summary> <code>DELETE</code> <code>/product-category/{id}</code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/product-category/75b44e08-2c94-438f-b500-b204c7c90cca' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM2Mjg0MTJ9.S6EML9bKGau9HB0CE9v_Sm0rCTOi0eQzRhjd-KI6ChF8n95RC9cTwphWyUisK3tKYuS5ZwXIHfmNvup2zRK5BQ'
``` 

### Request URL

```
http://localhost:8080/product-category/75b44e08-2c94-438f-b500-b204c7c90cca
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "75b44e08-2c94-438f-b500-b204c7c90cca"
}
```   

</details>

### PRODUCT SUB CATEGORY

<details>

<summary> <code>POST </code> <code>/product-sub-category</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/product-subcategory' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6ImFkbWluQGdtYWlsLmNvbSIsInVzZXJJZCI6IjNhNGFlMDMyLTY4MDEtNDc1Yi05NTFhLTI2MTRmMDRhOWJiMCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE3MjU4NjUyMDJ9.q3w-4G4bMJwdwB-kCv7KhR6JlTjsbUungkAMWFYTwVRpDLezumFsW_6kU7AJ2MqyQn1zAoEW7-Qtp68JZlHYgQ' \
  -H 'Content-Type: application/json' \
  -d '{
  "categoryId": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
  "name": "Paper"
}'
``` 

### Request URL

```
http://localhost:8080/product-subcategory
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "751cef10-f98a-4ecc-ae03-4173830a626d",
    "categoryId": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
    "name": "Paper"
  }
}
```   

</details>

<details>

<summary> <code>GET </code> <code>/product-sub-category</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/product-subcategory?id=b8ccc13f-e118-4540-8e9e-5eaa8028cb4f&limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6ImFkbWluQGdtYWlsLmNvbSIsInVzZXJJZCI6IjNhNGFlMDMyLTY4MDEtNDc1Yi05NTFhLTI2MTRmMDRhOWJiMCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE3MjU4NjUyMDJ9.q3w-4G4bMJwdwB-kCv7KhR6JlTjsbUungkAMWFYTwVRpDLezumFsW_6kU7AJ2MqyQn1zAoEW7-Qtp68JZlHYgQ'
``` 

### Request URL

```
http://localhost:8080/product-subcategory?id=b8ccc13f-e118-4540-8e9e-5eaa8028cb4f&limit=10
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": [
    {
      "id": "751cef10-f98a-4ecc-ae03-4173830a626d",
      "categoryId": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
      "name": "Paper"
    }
  ]
}
```   

</details>

<details>

<summary> <code>PUT </code> <code>/product-sub-category/{id}</code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/product-subcategory/b8ccc13f-e118-4540-8e9e-5eaa8028cb4f?name=Pencil' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6ImFkbWluQGdtYWlsLmNvbSIsInVzZXJJZCI6IjNhNGFlMDMyLTY4MDEtNDc1Yi05NTFhLTI2MTRmMDRhOWJiMCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE3MjU4NjUyMDJ9.q3w-4G4bMJwdwB-kCv7KhR6JlTjsbUungkAMWFYTwVRpDLezumFsW_6kU7AJ2MqyQn1zAoEW7-Qtp68JZlHYgQ'
``` 

### Request URL

```
http://localhost:8080/product-subcategory/751cef10-f98a-4ecc-ae03-4173830a626d?name=Pencil
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "751cef10-f98a-4ecc-ae03-4173830a626d",
    "categoryId": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
    "name": "Pencil"
  }
}
```   

</details>


<details>

<summary> <code>DELETE </code> <code>/product-sub-category/{id}</code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/product-subcategory/751cef10-f98a-4ecc-ae03-4173830a626d' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6ImFkbWluQGdtYWlsLmNvbSIsInVzZXJJZCI6IjNhNGFlMDMyLTY4MDEtNDc1Yi05NTFhLTI2MTRmMDRhOWJiMCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE3MjU4NjUyMDJ9.q3w-4G4bMJwdwB-kCv7KhR6JlTjsbUungkAMWFYTwVRpDLezumFsW_6kU7AJ2MqyQn1zAoEW7-Qtp68JZlHYgQ'
``` 

### Request URL

```
http://localhost:8080/product-subcategory/751cef10-f98a-4ecc-ae03-4173830a626d
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "751cef10-f98a-4ecc-ae03-4173830a626d"
}
```   

</details>

### BRAND

<details>

<summary> <code>POST </code> <code>/brand </code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/brand?name=Nike' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM5MDU0Mzl9.Jrn49AipUud_MkH4NbOtBNy9AsAwGE3W2wnW-dnUMifhEaijeaSbwn-jlUsCMPf1ayos2K0pQZma4LmWwuivPg' \
  -d ''
``` 

### Request URL

```
http://localhost:8080/brand?name=Nike
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "name": "Nike"
  }
}
```   

</details>

<details>

<summary> <code>GET </code> <code>/brand </code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/brand?limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM5MDU0Mzl9.Jrn49AipUud_MkH4NbOtBNy9AsAwGE3W2wnW-dnUMifhEaijeaSbwn-jlUsCMPf1ayos2K0pQZma4LmWwuivPg'
``` 

### Request URL

```
http://localhost:8080/brand?limit=10
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "name": "Nike"
    },
    {
      "id": "19dd1021-432c-473c-8b19-0f56d19af9ad",
      "name": "PUMA"
    }
  ]
}
```   

</details>

<details>

<summary> <code>PUT </code> <code>/brand/{id} </code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/brand/6c5078d3-f8e3-4c88-9afe-48b5423c664f?name=Addidas' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM5MDU0Mzl9.Jrn49AipUud_MkH4NbOtBNy9AsAwGE3W2wnW-dnUMifhEaijeaSbwn-jlUsCMPf1ayos2K0pQZma4LmWwuivPg'
``` 

### Request URL

```
http://localhost:8080/brand/6c5078d3-f8e3-4c88-9afe-48b5423c664f?name=Addidas
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "name": "Addidas"
  }
}
```   

</details>

<details>

<summary> <code>DELETE</code> <code>/brand/{id} </code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/brand/19dd1021-432c-473c-8b19-0f56d19af9ad' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM5MDU0Mzl9.Jrn49AipUud_MkH4NbOtBNy9AsAwGE3W2wnW-dnUMifhEaijeaSbwn-jlUsCMPf1ayos2K0pQZma4LmWwuivPg'
``` 

### Request URL

```
http://localhost:8080/brand/19dd1021-432c-473c-8b19-0f56d19af9ad
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "19dd1021-432c-473c-8b19-0f56d19af9ad"
}
```   

</details>

### WISHLIST

<details>

<summary> <code>POST</code> <code>/wishlist </code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/wishlist?productId=5b24d429-c981-47c8-9318-f4d61dd2c1a4' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6Ijg5YThhMGQ1LWQyNWMtNDBiYi05ZmRmLTc1MWM1YTAxNWUzNyIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5NDM1NDUzNH0.ggdIuRpAGr6rfPWK0dH1wP_s4LGa-XQZqB3SoWyqV6lYatiHtoheNOPGocRfXG5c0zxc6MwgGX3C7rDK9_3ABQ' \
  -d ''
``` 

### Request URL

```
http://localhost:8080/wishlist?productId=5b24d429-c981-47c8-9318-f4d61dd2c1a4
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "product": {
      "id": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
      "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
      "name": "Polo T Shirt",
      "productCode": "string",
      "productQuantity": 1,
      "description": "Chinese polo T-shirt",
      "price": 100,
      "discountPrice": 0,
      "status": 0,
      "hotDeal": "string",
      "bestRated": "string",
      "buyOneGetOne": "string"
    }
  }
}
```   

</details>

<details>

<summary> <code>GET </code> <code>/wishlist </code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/wishlist' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6Ijg5YThhMGQ1LWQyNWMtNDBiYi05ZmRmLTc1MWM1YTAxNWUzNyIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5NDM1NDUzNH0.ggdIuRpAGr6rfPWK0dH1wP_s4LGa-XQZqB3SoWyqV6lYatiHtoheNOPGocRfXG5c0zxc6MwgGX3C7rDK9_3ABQ'
``` 

### Request URL

```
http://localhost:8080/wishlist
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": [
    {
      "id": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
      "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
      "name": "Polo T Shirt",
      "productCode": "string",
      "productQuantity": 1,
      "descritpion": "Chinese polo T-shirt",
      "price": 100,
      "discountPrice": 0,
      "status": 0,
      "hotDeal": "string",
      "bestRated": "string",
      "buyOneGetOne": "string"
    }
  ]
}
```   

</details>

<details>

<summary> <code>DELETE </code> <code>/wishlist </code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/wishlist?productId=5b24d429-c981-47c8-9318-f4d61dd2c1a4' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6Ijg5YThhMGQ1LWQyNWMtNDBiYi05ZmRmLTc1MWM1YTAxNWUzNyIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5NDM1NDUzNH0.ggdIuRpAGr6rfPWK0dH1wP_s4LGa-XQZqB3SoWyqV6lYatiHtoheNOPGocRfXG5c0zxc6MwgGX3C7rDK9_3ABQ'
``` 

### Request URL

```
http://localhost:8080/wishlist?productId=5b24d429-c981-47c8-9318-f4d61dd2c1a4
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
    "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
    "name": "Polo T Shirt",
    "productCode": "string",
    "productQuantity": 1,
    "description": "Chinese polo T-shirt",
    "price": 100,
    "discountPrice": 0,
    "status": 0,
    "hotDeal": "string",
    "bestRated": "string",
    "buyOneGetOne": "string"
  }
}
```   

</details>

### SHIPPING

<details>

<summary> <code>GET </code> <code>/shipping </code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/shipping?orderId=c7f38846-4f63-460f-b956-f2b6758dbffd' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI4OTI2OTE2fQ.nejmXA_iKe8MzI9jhe6HPUBASuWZ8Zdhx4zYRRW-H-vAMq5m2p88_-z0DRrdFyVrH1nDIUVO03BKb1kwuX1xZw'
``` 

### Request URL

```
http://localhost:8080/shipping?orderId=c7f38846-4f63-460f-b956-f2b6758dbffd
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "5489a8b4-7a16-4854-b157-396a8a731032",
    "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
    "orderId": "c7f38846-4f63-460f-b956-f2b6758dbffd",
    "shipAddress": "update address",
    "shipCity": "Dhaka",
    "shipPhone": 1073741824,
    "shipName": "paperfly",
    "shipEmail": "customer@gmail.com",
    "shipCountry": "Bangladesh"
  }
}
```   

</details>

<details>

<summary> <code>POST </code> <code>/shipping </code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/shipping' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI4OTI2OTE2fQ.nejmXA_iKe8MzI9jhe6HPUBASuWZ8Zdhx4zYRRW-H-vAMq5m2p88_-z0DRrdFyVrH1nDIUVO03BKb1kwuX1xZw' \
  -H 'Content-Type: application/json' \
  -d '{
  "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
  "shipAddress": "Dhaka Bangladesh",
  "shipCity": "Dhaka",
  "shipCountry": "Bangladesh",
  "shipEmail": "customer@gmail.com",
  "shipName": "string",
  "shipPhone": 1073741824
}'
``` 

### Request URL

```
http://localhost:8080/shipping
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "471ebc82-80e7-4da0-a472-d1c8835f57b8",
    "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
    "shipAddress": "Dhaka Bangladesh",
    "shipCity": "Dhaka",
    "shipPhone": 1073741824,
    "shipName": "string",
    "shipEmail": "customer@gmail.com",
    "shipCountry": "Bangladesh"
  }
}
```   

</details>

<details>

<summary> <code>PUT </code> <code>/shipping/{id} </code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/shipping/5489a8b4-7a16-4854-b157-396a8a731032?shipAddress=Updated%20shipping%20address' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI4OTI2OTE2fQ.nejmXA_iKe8MzI9jhe6HPUBASuWZ8Zdhx4zYRRW-H-vAMq5m2p88_-z0DRrdFyVrH1nDIUVO03BKb1kwuX1xZw'
``` 

### Request URL

```
http://localhost:8080/shipping/5489a8b4-7a16-4854-b157-396a8a731032?shipAddress=Updated%20shipping%20address
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "5489a8b4-7a16-4854-b157-396a8a731032",
    "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
    "orderId": "c7f38846-4f63-460f-b956-f2b6758dbffd",
    "shipAddress": "Updated shipping address",
    "shipCity": "Dhaka",
    "shipPhone": 1073741824,
    "shipName": "paperfly",
    "shipEmail": "customer@gmail.com",
    "shipCountry": "Bangladesh"
  }
}
```   

</details>

<details>

<summary> <code>DELETE </code> <code>/shipping/{id} </code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/shipping/471ebc82-80e7-4da0-a472-d1c8835f57b8' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI4OTI2OTE2fQ.nejmXA_iKe8MzI9jhe6HPUBASuWZ8Zdhx4zYRRW-H-vAMq5m2p88_-z0DRrdFyVrH1nDIUVO03BKb1kwuX1xZw'
``` 

### Request URL

```
http://localhost:8080/shipping/471ebc82-80e7-4da0-a472-d1c8835f57b8
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "471ebc82-80e7-4da0-a472-d1c8835f57b8"
}
```   

</details>

### REVIEW-RATING

<details>

<summary> <code>GET</code> <code>/review-rating </code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/review-rating?productId=cbd630f6-bf9f-48ad-ac51-f806807d99fd&limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI4OTE5NzcxfQ.V5ZQKEnMVuSYXpJ8AjTljrJsmKYVSsY1dzGo8wlA8FzPXQM_Dcr9KBcNT7VFWedMz4Ctb0c8ivfvmcxD4CDleg'
``` 

### Request URL

```
http://localhost:8080/review-rating?productId=cbd630f6-bf9f-48ad-ac51-f806807d99fd&limit=10
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": [
    {
      "id": "70ac842b-7a81-4976-9564-d440880d1736",
      "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
      "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
      "reviewText": "Good product",
      "rating": 2
    }
  ]
}
```   

</details>

<details>

<summary> <code>POST</code> <code>/review-rating </code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/review-rating' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI4OTE5NzcxfQ.V5ZQKEnMVuSYXpJ8AjTljrJsmKYVSsY1dzGo8wlA8FzPXQM_Dcr9KBcNT7VFWedMz4Ctb0c8ivfvmcxD4CDleg' \
  -H 'Content-Type: application/json' \
  -d '{
  "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
  "rating": 2,
  "reviewText": "Good product"
}'
``` 

### Request URL

```
http://localhost:8080/review-rating
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "70ac842b-7a81-4976-9564-d440880d1736",
    "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
    "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
    "reviewText": "Good product",
    "rating": 2
  }
}
```   

</details>

<details>

<summary> <code>PUT</code> <code>/review-rating/{id} </code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/review-rating/70ac842b-7a81-4976-9564-d440880d1736?review=Product%20review%20edited&rating=5' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI4OTE5NzcxfQ.V5ZQKEnMVuSYXpJ8AjTljrJsmKYVSsY1dzGo8wlA8FzPXQM_Dcr9KBcNT7VFWedMz4Ctb0c8ivfvmcxD4CDleg'
``` 

### Request URL

```
http://localhost:8080/review-rating/70ac842b-7a81-4976-9564-d440880d1736?review=Product%20review%20edited&rating=5
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "70ac842b-7a81-4976-9564-d440880d1736",
    "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
    "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
    "reviewText": "Product review edited",
    "rating": 5
  }
}
```   

</details>

<details>

<summary> <code>DELETE</code> <code>/review-rating/{id} </code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/review-rating/70ac842b-7a81-4976-9564-d440880d1736' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI4OTE5NzcxfQ.V5ZQKEnMVuSYXpJ8AjTljrJsmKYVSsY1dzGo8wlA8FzPXQM_Dcr9KBcNT7VFWedMz4Ctb0c8ivfvmcxD4CDleg'
``` 

### Request URL

```
http://localhost:8080/review-rating/70ac842b-7a81-4976-9564-d440880d1736
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "70ac842b-7a81-4976-9564-d440880d1736"
}
```   

</details>

### CART

<details>

<summary> <code>POST</code> <code>/cart </code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/cart?productId=5b24d429-c981-47c8-9318-f4d61dd2c1a4&quantity=1' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6Ijg5YThhMGQ1LWQyNWMtNDBiYi05ZmRmLTc1MWM1YTAxNWUzNyIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5NDM1NTk4N30.rq2rnhBUoAEEoImdqhD7dEo0UnkEFHb5q9cOC-AQ_Gjaf2pE0R7eu15MGn12kp5KJkJQIYx5jB5Tpn3OaphuGQ' \
  -d ''
``` 

### Request URL

```
http://localhost:8080/cart?productId=5b24d429-c981-47c8-9318-f4d61dd2c1a4&quantity=1
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "productId": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
    "quantity": 1
  }
}
```   

</details>

<details>

<summary> <code>GET </code> <code>/cart </code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/cart?limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6Ijg5YThhMGQ1LWQyNWMtNDBiYi05ZmRmLTc1MWM1YTAxNWUzNyIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5NDM1NTk4N30.rq2rnhBUoAEEoImdqhD7dEo0UnkEFHb5q9cOC-AQ_Gjaf2pE0R7eu15MGn12kp5KJkJQIYx5jB5Tpn3OaphuGQ'
``` 

### Request URL

```
http://localhost:8080/cart?limit=10
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": [
    {
      "productId": "71b26dd9-b4b5-4f87-a84d-c8daa506018a",
      "quantity": 3,
      "product": {
        "id": "71b26dd9-b4b5-4f87-a84d-c8daa506018a",
        "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
        "productName": "Smartch watch",
        "productCode": "string",
        "productQuantity": 5,
        "productDetail": "Xiaomi Smart Watch",
        "price": 10,
        "discountPrice": 0,
        "status": 0,
        "videoLink": "string",
        "mainSlider": "string",
        "hotDeal": "string",
        "bestRated": "string",
        "midSlider": "string",
        "hotNew": "string",
        "trend": "string",
        "buyOneGetOne": "string",
        "imageOne": "string",
        "imageTwo": "string"
      }
    },
    {
      "productId": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
      "quantity": 1,
      "product": {
        "id": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
        "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
        "productName": "Polo T Shirt",
        "productCode": "string",
        "productQuantity": 1,
        "productDetail": "Chinese polo T-shirt",
        "price": 100,
        "discountPrice": 0,
        "status": 0,
        "hotDeal": "string",
        "bestRated": "string",
        "buyOneGetOne": "string"
      }
    }
  ]
}
```   

</details>

<details>

<summary> <code>PUT </code> <code>/cart </code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/cart?productId=5b24d429-c981-47c8-9318-f4d61dd2c1a4&quantity=1' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6Ijg5YThhMGQ1LWQyNWMtNDBiYi05ZmRmLTc1MWM1YTAxNWUzNyIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5NDM1NTk4N30.rq2rnhBUoAEEoImdqhD7dEo0UnkEFHb5q9cOC-AQ_Gjaf2pE0R7eu15MGn12kp5KJkJQIYx5jB5Tpn3OaphuGQ'
``` 

### Request URL

```
http://localhost:8080/cart?productId=5b24d429-c981-47c8-9318-f4d61dd2c1a4&quantity=1
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "productId": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
    "quantity": 2,
    "product": {
      "id": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
      "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
      "productName": "Polo T Shirt",
      "productCode": "string",
      "productQuantity": 1,
      "productDetail": "Chinese polo T-shirt",
      "price": 100,
      "discountPrice": 0,
      "status": 0,
      "hotDeal": "string",
      "bestRated": "string",
      "buyOneGetOne": "string"
    }
  }
}
```   

</details>

<details>

<summary> <code>DELETE </code> <code>/cart </code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/cart?productId=71b26dd9-b4b5-4f87-a84d-c8daa506018a' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6Ijg5YThhMGQ1LWQyNWMtNDBiYi05ZmRmLTc1MWM1YTAxNWUzNyIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5NDM1NTk4N30.rq2rnhBUoAEEoImdqhD7dEo0UnkEFHb5q9cOC-AQ_Gjaf2pE0R7eu15MGn12kp5KJkJQIYx5jB5Tpn3OaphuGQ'
``` 

### Request URL

```
http://localhost:8080/cart/71b26dd9-b4b5-4f87-a84d-c8daa506018a
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "71b26dd9-b4b5-4f87-a84d-c8daa506018a",
    "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
    "productName": "Smartch watch",
    "productCode": "string",
    "productQuantity": 5,
    "productDetail": "Xiaomi Smart Watch",
    "price": 10,
    "discountPrice": 0,
    "status": 0,
    "videoLink": "string",
    "mainSlider": "string",
    "hotDeal": "string",
    "bestRated": "string",
    "midSlider": "string",
    "hotNew": "string",
    "trend": "string",
    "buyOneGetOne": "string",
    "imageOne": "string",
    "imageTwo": "string"
  }
}
```   

</details>

<details>

<summary> <code>DELETE </code> <code>/cart/all </code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/cart/all' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6Ijg5YThhMGQ1LWQyNWMtNDBiYi05ZmRmLTc1MWM1YTAxNWUzNyIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5NDM1NzU3MX0.rRj8mHIqG-d78t_H54HjiWl7GBzgH4KOWUKAWsDveolmBcxTfyCJKzWd4K8Jwq5MKvJ3xa8J1vf0E34DSHA4sw'
``` 

### Request URL

```
http://localhost:8080/cart/all
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": true
}
```   

</details>

### ORDER

<details>

<summary> <code>POST</code> <code>/order </code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/order' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6Ijg5YThhMGQ1LWQyNWMtNDBiYi05ZmRmLTc1MWM1YTAxNWUzNyIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5NDk1NjIzNX0.Go4nLzsiOruUKTtETn1Yc35BNjlo79_3Vs8LfW9LJG1nvogqIR0mG9JQOUxP8YclsVBzGV0j0IIv7svTaDMxTg' \
  -H 'Content-Type: application/json' \
  -d '{
  "quantity": 1,
  "subTotal": 10,
  "total": 10,
  "shippingCharge": 5,
  "orderStatus": "pending",
  "orderItems": [
    {
      "productId": "71b26dd9-b4b5-4f87-a84d-c8daa506018a",
      "quantity": 1
    }
  ]
}'
``` 

### Request URL

```
http://localhost:8080/order
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "orderId": "b177431f-22f2-4c01-8ad6-da5319e2c7b9"
  }
}
```   

</details>

<details>

<summary> <code>GET </code> <code>/order </code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/order?limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6Ijg5YThhMGQ1LWQyNWMtNDBiYi05ZmRmLTc1MWM1YTAxNWUzNyIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5NDk1NjIzNX0.Go4nLzsiOruUKTtETn1Yc35BNjlo79_3Vs8LfW9LJG1nvogqIR0mG9JQOUxP8YclsVBzGV0j0IIv7svTaDMxTg'
``` 

### Request URL

```
http://localhost:8080/order?limit=10
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": [
    {
      "orderId": "04675b54-a9df-4200-a526-0b15f6a85930",
      "quantity": 1,
      "subTotal": 10,
      "total": 10,
      "shippingCharge": 100,
      "cancelOrder": false,
      "status": "pending",
      "statusCode": 0
    },
    {
      "orderId": "f88ab61d-5e52-431b-82e8-48e5b607085c",
      "quantity": 1,
      "subTotal": 10,
      "total": 10,
      "shippingCharge": 100,
      "cancelOrder": false,
      "status": "pending",
      "statusCode": 0
    },
    {
      "orderId": "b177431f-22f2-4c01-8ad6-da5319e2c7b9",
      "quantity": 1,
      "subTotal": 10,
      "total": 10,
      "shippingCharge": 5,
      "cancelOrder": false,
      "status": "pending",
      "statusCode": 0
    }
  ]
}
```   

</details>


<details>
<summary> <code>PATCH </code> <code>/order/{id} </code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/order/7e49b2a1-fa0c-4aac-b996-91f2411f14b7?status=delivered' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI4OTA5NDYxfQ.KU7eVxmkq0zg1sQy9RsAVAxuwtGcuGlB9pqMnc-eUI9If-dycduIhn7xv8np0yFVtULAF2_ns59u38gsR237eQ'
``` 

### Request URL

```
http://localhost:8080/order/7e49b2a1-fa0c-4aac-b996-91f2411f14b7?status=delivered
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
    "quantity": 1073741824,
    "subTotal": 0.1,
    "total": 0.1,
    "shippingCharge": 0.1,
    "cancelOrder": false,
    "status": "delivered",
    "statusCode": 4
  }
}
```   

</details>

### PAYMENT

<details>
<summary> <code>POST </code> <code>/payment </code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/payment' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI4OTE5NzcxfQ.V5ZQKEnMVuSYXpJ8AjTljrJsmKYVSsY1dzGo8wlA8FzPXQM_Dcr9KBcNT7VFWedMz4Ctb0c8ivfvmcxD4CDleg' \
  -H 'Content-Type: application/json' \
  -d '{
  "amount": 500,
  "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
  "paymentMethod": "Bkash",
  "status": "COMPLETED"
}'
``` 

### Request URL

```
http://localhost:8080/payment
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "4b68917d-4452-4d18-9012-47e843f05c15",
    "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
    "amount": 500,
    "status": "COMPLETED",
    "paymentMethod": "Bkash"
  }
}
```   

</details>
<details>
<summary> <code>GET </code> <code>/payment/{id} </code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/payment/4b68917d-4452-4d18-9012-47e843f05c15' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI4OTE5NzcxfQ.V5ZQKEnMVuSYXpJ8AjTljrJsmKYVSsY1dzGo8wlA8FzPXQM_Dcr9KBcNT7VFWedMz4Ctb0c8ivfvmcxD4CDleg'
``` 

### Request URL

```
http://localhost:8080/payment/4b68917d-4452-4d18-9012-47e843f05c15
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "4b68917d-4452-4d18-9012-47e843f05c15",
    "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
    "amount": 500,
    "status": "COMPLETED",
    "paymentMethod": "Bkash"
  }
}
```   

</details>

### PRIVACY POLICY

<details>

<summary> <code>GET </code> <code>/policy</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/policy' \
  -H 'accept: application/json'
```

### Request URL

```
http://localhost:8080/policy
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "type": "PRIVACY_POLICY",
      "title": "Privacy Policy",
      "content": "This is our privacy policy...",
      "version": "1.0",
      "effectiveDate": "2023-01-01T00:00:00Z",
      "isActive": true,
      "createdAt": "2023-01-01T00:00:00Z",
      "updatedAt": "2023-01-01T00:00:00Z"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "type": "TERMS_CONDITIONS",
      "title": "Terms and Conditions",
      "content": "These are our terms and conditions...",
      "version": "1.0",
      "effectiveDate": "2023-01-01T00:00:00Z",
      "isActive": true,
      "createdAt": "2023-01-01T00:00:00Z",
      "updatedAt": "2023-01-01T00:00:00Z"
    }
  ]
}
```   

</details>

<details>

<summary> <code>GET </code> <code>/policy/{type}</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/policy/PRIVACY_POLICY' \
  -H 'accept: application/json'
```

### Request URL

```
http://localhost:8080/policy/PRIVACY_POLICY
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "type": "PRIVACY_POLICY",
    "title": "Privacy Policy",
    "content": "This is our privacy policy...",
    "version": "1.0",
    "effectiveDate": "2023-01-01T00:00:00Z",
    "isActive": true,
    "createdAt": "2023-01-01T00:00:00Z",
    "updatedAt": "2023-01-01T00:00:00Z"
  }
}
```   

</details>

<details>

<summary> <code>GET </code> <code>/policy/detail/{id}</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/policy/detail/550e8400-e29b-41d4-a716-446655440000' \
  -H 'accept: application/json'
```

### Request URL

```
http://localhost:8080/policy/detail/550e8400-e29b-41d4-a716-446655440000
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "type": "PRIVACY_POLICY",
    "title": "Privacy Policy",
    "content": "This is our privacy policy...",
    "version": "1.0",
    "effectiveDate": "2023-01-01T00:00:00Z",
    "isActive": true,
    "createdAt": "2023-01-01T00:00:00Z",
    "updatedAt": "2023-01-01T00:00:00Z"
  }
}
```   

</details>

<details>

<summary> <code>POST </code> <code>/policy</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/policy' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJhZG1pbkBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImFkbWluIiwiZXhwIjoxNzI4OTI2OTE2fQ.nejmXA_iKe8MzI9jhe6HPUBASuWZ8Zdhx4zYRRW-H-vAMq5m2p88_-z0DRrdFyVrH1nDIUVO03BKb1kwuX1xZw' \
  -H 'Content-Type: application/json' \
  -d '{
  "type": "PRIVACY_POLICY",
  "title": "Updated Privacy Policy",
  "content": "This is our updated privacy policy...",
  "version": "1.1",
  "effectiveDate": "2023-06-15T00:00:00Z"
}'
```

### Request URL

```
http://localhost:8080/policy
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 201,
    "description": "Created"
  },
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "type": "PRIVACY_POLICY",
    "title": "Updated Privacy Policy",
    "content": "This is our updated privacy policy...",
    "version": "1.1",
    "effectiveDate": "2023-06-15T00:00:00Z",
    "isActive": true
  }
}
```   

</details>

<details>

<summary> <code>PUT </code> <code>/policy/{id}</code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/policy/550e8400-e29b-41d4-a716-446655440003' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJhZG1pbkBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImFkbWluIiwiZXhwIjoxNzI4OTI2OTE2fQ.nejmXA_iKe8MzI9jhe6HPUBASuWZ8Zdhx4zYRRW-H-vAMq5m2p88_-z0DRrdFyVrH1nDIUVO03BKb1kwuX1xZw' \
  -H 'Content-Type: application/json' \
  -d '{
  "title": "Updated Privacy Policy v2",
  "content": "This is our further updated privacy policy...",
  "version": "1.2",
  "effectiveDate": "2023-07-01T00:00:00Z"
}'
```

### Request URL

```
http://localhost:8080/policy/550e8400-e29b-41d4-a716-446655440003
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "type": "PRIVACY_POLICY",
    "title": "Updated Privacy Policy v2",
    "content": "This is our further updated privacy policy...",
    "version": "1.2",
    "effectiveDate": "2023-07-01T00:00:00Z",
    "isActive": true
  }
}
```   

</details>

<details>

<summary> <code>POST </code> <code>/policy/deactivate/{id}</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/policy/deactivate/550e8400-e29b-41d4-a716-446655440003' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJhZG1pbkBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImFkbWluIiwiZXhwIjoxNzI4OTI2OTE2fQ.nejmXA_iKe8MzI9jhe6HPUBASuWZ8Zdhx4zYRRW-H-vAMq5m2p88_-z0DRrdFyVrH1nDIUVO03BKb1kwuX1xZw'
```

### Request URL

```
http://localhost:8080/policy/deactivate/550e8400-e29b-41d4-a716-446655440003
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "type": "PRIVACY_POLICY",
    "title": "Updated Privacy Policy v2",
    "content": "This is our further updated privacy policy...",
    "version": "1.2",
    "effectiveDate": "2023-07-01T00:00:00Z",
    "isActive": false
  }
}
```   

</details>

### PRIVACY POLICY CONSENTS

<details>

<summary> <code>POST </code> <code>/policy-consent</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/policy/consent' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI4OTI2OTE2fQ.nejmXA_iKe8MzI9jhe6HPUBASuWZ8Zdhx4zYRRW-H-vAMq5m2p88_-z0DRrdFyVrH1nDIUVO03BKb1kwuX1xZw' \
  -H 'Content-Type: application/json' \
  -d '{
  "policyId": "550e8400-e29b-41d4-a716-446655440000"
}'
```

### Request URL

```
http://localhost:8080/policy/consent
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 201,
    "description": "Created"
  },
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
    "policyId": "550e8400-e29b-41d4-a716-446655440000",
    "ipAddress": "127.0.0.1",
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
    "consentedAt": "2023-06-15T10:30:00Z"
  }
}
```   

</details>

<details>

<summary> <code>GET </code> <code>/policy-consents</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/user-consents' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI4OTI2OTE2fQ.nejmXA_iKe8MzI9jhe6HPUBASuWZ8Zdhx4zYRRW-H-vAMq5m2p88_-z0DRrdFyVrH1nDIUVO03BKb1kwuX1xZw'
```

### Request URL

```
http://localhost:8080/user-consents
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
      "policyId": "550e8400-e29b-41d4-a716-446655440000",
      "policyType": "PRIVACY_POLICY",
      "policyVersion": "1.0",
      "ipAddress": "127.0.0.1",
      "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
      "consentedAt": "2023-06-15T10:30:00Z"
    }
  ]
}
```   

</details>

<details>

<summary> <code>GET </code> <code>/policy-consents/{policyType}</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/user-consents/PRIVACY_POLICY' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6InBpYXNoY3NlIiwiZW1haWwiOiJjdXN0b21lckBnbWFpbC5jb20iLCJ1c2VySWQiOiJhNjdmZDBjYy0zZDkyLTQyNTktYmJkNC0xZTBiYTQ5ZGVjZTQiLCJ1c2VyVHlwZSI6ImN1c3RvbWVyIiwiZXhwIjoxNzI4OTI2OTE2fQ.nejmXA_iKe8MzI9jhe6HPUBASuWZ8Zdhx4zYRRW-H-vAMq5m2p88_-z0DRrdFyVrH1nDIUVO03BKb1kwuX1xZw'
```

### Request URL

```
http://localhost:8080/user-consents/PRIVACY_POLICY
``` 

### Response

```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "hasConsented": true
  }
}
```   

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
See the License for the specific language governing permissions and
limitations under the License.
```
