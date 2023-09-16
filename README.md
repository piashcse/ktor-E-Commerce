# Ktor-E-Commerce Api for Backend. 
[![Ktor](https://img.shields.io/badge/ktor-2.3.4-blue.svg)](https://github.com/ktorio/ktor)
[![Exposed](https://img.shields.io/badge/Exposed-0.43.0-blue.svg)](https://github.com/JetBrains/Exposed)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
<a href="https://github.com/piashcse"><img alt="License" src="https://img.shields.io/static/v1?label=GitHub&message=piashcse&color=C51162"/></a>

Ktor E-Commerce Backend, a high-performance backend solution for your e-commerce application built with [ktor](https://ktor.io/docs/welcome.html).

## Swagger View

<p align="center">
  <img width="100%" height="40%" src="https://github.com/piashcse/ktor-E-Commerce/blob/master/screenshots/Screenshot-2023-05-13.png" />
</p>


# Features

### 1. Role-Based Access Control

- **User Role**: Shoppers with basic access to browse and make purchases.
- **Seller Role**: Vendors can list products and manage their inventory.
- **Admin Role**: Administrators have full control over the platform.
  
### 2. User Accounts and Authentication

- **User Registration**: Allow customers to create accounts.
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
  <img width="40%" height="25%" src="https://github.com/piashcse/ktor-E-Commerce/blob/master/screenshots/mvc.png" />
</p>
<p align="center">
<b>Fig.  MVC (Model - View - Controller) design pattern.</b>
</p>

## Built With 🛠
- [Ktor](https://ktor.io/docs/welcome.html) - Ktor is a framework to easily build connected applications – web applications, HTTP services, mobile and browser applications. Modern connected applications need to be asynchronous to provide the best experience to users, and Kotlin Coroutines provides awesome facilities to do it in an easy and straightforward way.
- [Exposed](https://github.com/JetBrains/Exposed) - Exposed is a lightweight SQL library on top of JDBC driver for Kotlin language. Exposed has two flavors of database access: typesafe SQL wrapping DSL and lightweight Data Access Objects (DAO).
- [PostgreSQL](https://www.postgresql.org/) - PostgreSQL is a powerful, open-source object-relational database system that uses and extends the SQL language combined with many features that safely store and scale the most complicated data workloads. 
- [Kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime) - A multiplatform Kotlin library for working with date and time.
- [Bcrypt](https://github.com/patrickfav/bcrypt) - A Java standalone implementation of the bcrypt password hash function. Based on the Blowfish cipher it is the default password hash algorithm for OpenBSD and other systems including some Linux distributions.
- [Apache Commons Email](https://github.com/apache/commons-email) - Apache Commons Email aims to provide an API for sending email. It is built on top of the JavaMail API, which it aims to simplify.
- [Ktor OpenAPI/Swagger](https://github.com/LukasForst/ktor-openapi-generator) - The Ktor OpenAPI Generator is a library to automatically generate the descriptor as you route your ktor application.
- [Valiktor](https://github.com/valiktor/valiktor) - Valiktor is a type-safe, powerful and extensible fluent DSL to validate objects in Kotlin

## Requirements

- [JAVA 11](https://jdk.java.net/11/) (or latest)
- [PostgreSQL](https://www.postgresql.org/) (latest)

## How to run

- `git clone git@github.com:piashcse/ktor-E-Commerce.git` 
-  Create a database in postgreSQL
-  Change your db name in `resources/hikari.properties` and replace your database name in `dataSource.databaseName=ktor-1.0.0` instead of `ktor-1.0.0`.
- `run fun main()` from application class

## Documentation

### ROLE MANAGEMENT
<details>
<summary>Admin, Seller, User </summary>

- <b> User Role:</b> Users can browse products, make purchases, and manage their accounts.
- <b> Seller Role:</b>  Sellers can list products, manage their inventory, and view order details.
- <b> Admin Role:</b>  Admins have full access to manage users, roles, and system settings.

</details>


### USER
<details>
  
<summary> <code>GET </code> <code>/login</code></summary>

### Curl

    curl -X 'GET' \ 'http://localhost:8080/login?email=piash599%40gmail.com&password=p1234&userType=user' \ 
    -H 'accept: application/json'

### Request URL

    http://localhost:8080/login?email=piash599%40gmail.com&password=p1234&userType=user


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
      "id": "a9a662a7-50fe-4f13-8eab-0e0810fb9909",
      "email": "piash599@gmail.com",
      "userType": {
        "id": "04d85ebe-a667-4f80-94c8-0f68a3b3d96d",
        "userType": "user"
      }
    },
    "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImE5YTY2MmE3LTUwZmUtNGYxMy04ZWFiLTBlMDgxMGZiOTkwOSIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5Mjc2NTM0MX0.8GscAPCxFWOhN2bmy5bsoz5V311O4g72XqlEUWoz_y0wADkTzdgOVfG5CKJba5VUvwNiVE3MmQPmNt-fq6hyyw"
  }
}
```   
</details>

<details>
  
<summary> <code>POST</code> <code>/registration</code></summary>

### Curl

```
  curl -X 'POST' \
  'http://localhost:8080/registration' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "email": "piash88@gmail.com",
  "password": "piash956",
  "userType": "admin"
}'
``` 

### Request URL

    http://localhost:8080/registration


### Response
```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "email": "piash88@gmail.com"
  }
}

```   
</details>


<details>
  
<summary><code>GET </code> <code>/forget-password</code></summary>

### Curl

```
 curl -X 'GET' \
  'http://localhost:8080/forget-password?email=piash599%40gmail.com' \
  -H 'accept: application/json'
``` 

### Request URL

```
http://localhost:8080/forget-password?email=piash599%40gmail.com

``` 


### Response
```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "verification code sent to piash599@gmail.com"
}

```   
</details>

<details>
  
<summary><code>GET </code> <code>/verify-change-password</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/verify-change-password?email=piash599%40gmail.com&verificationCode=9889&password=p1234' \
  -H 'accept: application/json'
``` 

### Request URL

```
http://localhost:8080/verify-change-password?email=piash599%40gmail.com&verificationCode=9189&newPassword=p1234
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
  
<summary> <code>PUT </code> <code>/change-password</code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/change-password?oldPassword=p1234&newPassword=p1234' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImE5YTY2MmE3LTUwZmUtNGYxMy04ZWFiLTBlMDgxMGZiOTkwOSIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5MzEzMzI3NH0.Jy136YnG5Py4zotIZBr4KvaPblONOu1MVy58iECgyGb4spQjW8Vu_tBwc0frl85Vqup8g3NJlqHIDqLs8f-J0g'
``` 

### Request URL

```
http://localhost:8080/change-password?oldPassword=p1234&newPassword=p1234
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
  
<summary> <code>GET </code> <code>/profile</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/profile' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImE5YTY2MmE3LTUwZmUtNGYxMy04ZWFiLTBlMDgxMGZiOTkwOSIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5MzEzMzI3NH0.Jy136YnG5Py4zotIZBr4KvaPblONOu1MVy58iECgyGb4spQjW8Vu_tBwc0frl85Vqup8g3NJlqHIDqLs8f-J0g'
``` 

### Request URL

```
http://localhost:8080/profile
``` 


### Response
```
{
  "isSuccess": true,
  "statsCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "userId": "a9a662a7-50fe-4f13-8eab-0e0810fb9909",
    "userProfileImage": "708196a2-1529-45d4-ab8e-585c248e434f.jpeg",
    "firstName": "Mehedi Hassan Piash",
    "lastName": "string",
    "secondaryMobileNumber": "string",
    "faxNumber": "string",
    "streetAddress": "string",
    "city": "string",
    "identificationType": "string",
    "identificationNo": "string",
    "occupation": "string",
    "userDescription": "string",
    "maritalStatus": "string",
    "postCode": "string",
    "gender": "string"
  }
}
```   
</details>

<details>
  
<summary> <code>PUT </code> <code>/profile</code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/profile' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImE5YTY2MmE3LTUwZmUtNGYxMy04ZWFiLTBlMDgxMGZiOTkwOSIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5MzEzMzI3NH0.Jy136YnG5Py4zotIZBr4KvaPblONOu1MVy58iECgyGb4spQjW8Vu_tBwc0frl85Vqup8g3NJlqHIDqLs8f-J0g' \
  -H 'Content-Type: application/json' \
  -d '{
  "firstName": "Mehedi Hassan",
  "lastName": "Piash"
}'
``` 

### Request URL

```
http://localhost:8080/profile
``` 


### Response
```
{
  "isSuccess": true,
  "statsCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "userId": "a9a662a7-50fe-4f13-8eab-0e0810fb9909",
    "userProfileImage": "708196a2-1529-45d4-ab8e-585c248e434f.jpeg",
    "firstName": "Mehedi Hassan Piash",
    "lastName": "string",
    "secondaryMobileNumber": "string",
    "faxNumber": "string",
    "streetAddress": "string",
    "city": "string",
    "identificationType": "string",
    "identificationNo": "string",
    "occupation": "string",
    "userDescription": "string",
    "maritalStatus": "string",
    "postCode": "string",
    "gender": "string"
  }
}
```   
</details>


<details>
  
<summary> <code>POST</code> <code>/profile</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/profile-photo-upload' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImE5YTY2MmE3LTUwZmUtNGYxMy04ZWFiLTBlMDgxMGZiOTkwOSIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5MzEzMzI3NH0.Jy136YnG5Py4zotIZBr4KvaPblONOu1MVy58iECgyGb4spQjW8Vu_tBwc0frl85Vqup8g3NJlqHIDqLs8f-J0g' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@piash.jpeg;type=image/jpeg'
``` 

### Request URL

```
http://localhost:8080/profile-photo-upload
``` 


### Response
```
{
  "isSuccess": true,
  "statsCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "37a1453d-1b76-4052-8f58-56eddeaeadca.jpeg"
}
```   
</details>

### SHOP
<details>
  
<summary> <code>POST</code> <code>/shop/category</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/shop/category?shopCategoryName=BD%20Shop' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjdhMGQ5YTU0LTIzZDctNGY5Yy05YWI2LTgwYzQ3Mzg4MDVlNCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTMzNjg0OTl9.0KnZ9PyQ9XMbxjCaOKsDKyk7lWvwxv4weQDi9wmhHJpaXhqRvZYxU43RzdmuGmxJwnLpT32fe-rwwvkl1IOPpQ' \
  -d ''
``` 

### Request URL

```
http://localhost:8080/shop/category?shopCategoryName=BD%20Shop
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
    "id": "2a17da31-7517-41db-b7d3-f77d0ddd52a5",
    "shopName": "BD Shop"
  }
}
```   
</details>

<details>
  
<summary> <code>GET </code> <code>/shop/category</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/shop/category?limit=10&offset=0' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjdhMGQ5YTU0LTIzZDctNGY5Yy05YWI2LTgwYzQ3Mzg4MDVlNCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTMzNjg0OTl9.0KnZ9PyQ9XMbxjCaOKsDKyk7lWvwxv4weQDi9wmhHJpaXhqRvZYxU43RzdmuGmxJwnLpT32fe-rwwvkl1IOPpQ'
``` 

### Request URL

```
http://localhost:8080/shop/category?limit=10&offset=0
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
      "id": "5e67ec97-9ed6-48ee-9d56-4163fe1711cb",
      "shopName": "Royal shop"
    },
    {
      "id": "2a17da31-7517-41db-b7d3-f77d0ddd52a5",
      "shopName": "BD Shop"
    }
  ]
}
```   
</details>

<details>
  
<summary> <code>DELETE</code> <code>/shop/category</code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/shop/category?shopCategoryId=2a17da31-7517-41db-b7d3-f77d0ddd52a5' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjdhMGQ5YTU0LTIzZDctNGY5Yy05YWI2LTgwYzQ3Mzg4MDVlNCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTMzNjg0OTl9.0KnZ9PyQ9XMbxjCaOKsDKyk7lWvwxv4weQDi9wmhHJpaXhqRvZYxU43RzdmuGmxJwnLpT32fe-rwwvkl1IOPpQ'
``` 

### Request URL

```
http://localhost:8080/shop/category?shopCategoryId=2a17da31-7517-41db-b7d3-f77d0ddd52a5
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
  
<summary> <code>PUT </code> <code>/shop/category</code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/shop/category?shopCategoryId=5e67ec97-9ed6-48ee-9d56-4163fe1711cb&shopCategoryName=BD%20Shop' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjdhMGQ5YTU0LTIzZDctNGY5Yy05YWI2LTgwYzQ3Mzg4MDVlNCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTMzNjg0OTl9.0KnZ9PyQ9XMbxjCaOKsDKyk7lWvwxv4weQDi9wmhHJpaXhqRvZYxU43RzdmuGmxJwnLpT32fe-rwwvkl1IOPpQ'
``` 

### Request URL

```
http://localhost:8080/shop/category?shopCategoryId=5e67ec97-9ed6-48ee-9d56-4163fe1711cb&shopCategoryName=BD%20Shop
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
    "id": "5e67ec97-9ed6-48ee-9d56-4163fe1711cb",
    "shopName": "BD Shop"
  }
}
```   
</details>

<details>
  
<summary> <code>POST</code> <code>/shop/add-shop</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/shop/add-shop?shopName=Royal%20Shop&shopCategoryId=5e67ec97-9ed6-48ee-9d56-4163fe1711cb' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjdhMGQ5YTU0LTIzZDctNGY5Yy05YWI2LTgwYzQ3Mzg4MDVlNCIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTMzNjg0OTl9.0KnZ9PyQ9XMbxjCaOKsDKyk7lWvwxv4weQDi9wmhHJpaXhqRvZYxU43RzdmuGmxJwnLpT32fe-rwwvkl1IOPpQ' \
  -d ''
``` 

### Request URL

```
http://localhost:8080/shop/add-shop?shopName=Royal%20Shop&shopCategoryId=5e67ec97-9ed6-48ee-9d56-4163fe1711cb
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
    "shopName": "Royal Shop"
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
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjA1ZjA4MWIxLWRhY2UtNDllMy1iMmIyLTIxNmE3N2U5NjUxYyIsInVzZXJUeXBlIjoic2VsbGVyIiwiZXhwIjoxNjkzMzY5OTcyfQ.QF530JiVfrYSz1m4aDAcTqOkvJ2zLy68MGiYkBQlzWamxF1O2BfqTYBWofRM0LWspCMXQt2HQTRijmjLW7dglQ' \
  -H 'Content-Type: application/json' \
  -d '{
  "bestRated": "string",
  "buyOneGetOne": "string",
  "categoryId": "ec757f21-4054-43fb-ac22-d6a2973e49e4",
  "discountPrice": 0,
  "hotDeal": "string",
  "hotNew": "string",
  "imageOne": "string",
  "imageTwo": "string",
  "mainSlider": "string",
  "midSlider": "string",
  "price": 10,
  "productCode": "string",
  "productDetail": "string",
  "productName": "string",
  "productQuantity": 5,
  "status": 0,
  "trend": "string",
  "videoLink": "string"
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
    "id": "79a97389-78d5-4dff-a1f7-13bc7ae10a8d",
    "categoryId": "ec757f21-4054-43fb-ac22-d6a2973e49e4",
    "productName": "string",
    "productCode": "string",
    "productQuantity": 5,
    "productDetail": "string",
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
  
<summary> <code>GET</code> <code>/product/{productId}</code></summary>

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
    "id": "79a97389-78d5-4dff-a1f7-13bc7ae10a8d",
    "categoryId": "ec757f21-4054-43fb-ac22-d6a2973e49e4",
    "productName": "string",
    "productCode": "string",
    "productQuantity": 5,
    "productDetail": "string",
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
  
<summary> <code>PUT </code> <code>/product/{productId}</code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/product/79a97389-78d5-4dff-a1f7-13bc7ae10a8d' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjA1ZjA4MWIxLWRhY2UtNDllMy1iMmIyLTIxNmE3N2U5NjUxYyIsInVzZXJUeXBlIjoic2VsbGVyIiwiZXhwIjoxNjkzMzcwMjUxfQ.aUj7fEXcNtKP_XdKVI6ICk5GlnTVivxhOkZ8S7_l3NExzIAT93QjuoFiNCDs873OEVO66cEUiSSWjkJVDmzMuA' \
  -H 'Content-Type: application/json' \
  -d '{
 
  "productName": "Smartch watch",
   "productDetail":"Xiaomi Smart Watch"
  
}'
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
    "id": "79a97389-78d5-4dff-a1f7-13bc7ae10a8d",
    "categoryId": "ec757f21-4054-43fb-ac22-d6a2973e49e4",
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
  
<summary> <code>GET </code> <code>/product</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/product?limit=10&offset=0&maxPrice=100&minPrice=0' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjA1ZjA4MWIxLWRhY2UtNDllMy1iMmIyLTIxNmE3N2U5NjUxYyIsInVzZXJUeXBlIjoic2VsbGVyIiwiZXhwIjoxNjkzMzcwMjUxfQ.aUj7fEXcNtKP_XdKVI6ICk5GlnTVivxhOkZ8S7_l3NExzIAT93QjuoFiNCDs873OEVO66cEUiSSWjkJVDmzMuA'
``` 

### Request URL

```
http://localhost:8080/product?limit=10&offset=0&maxPrice=100&minPrice=0
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
      "id": "79a97389-78d5-4dff-a1f7-13bc7ae10a8d",
      "categoryId": "ec757f21-4054-43fb-ac22-d6a2973e49e4",
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
  ]
}
```   
</details>


<details>
  
<summary> <code>DELETE</code> <code>/product</code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/product?productId=79a97389-78d5-4dff-a1f7-13bc7ae10a8d' \
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
  
<summary> <code>POST</code> <code>/product/photo-upload</code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/product/photo-upload?productId=71b26dd9-b4b5-4f87-a84d-c8daa506018a' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6IjEyYzA5OGFhLTYzMWYtNDJlZC05MjAzLTFkMjdlMDA0MmY4YSIsInVzZXJUeXBlIjoic2VsbGVyIiwiZXhwIjoxNjkzMzc3NTMyfQ.GeBDEnWNm84mHPhCxTCXUwRSmRo7KjkJ6AfuEXZNiiqKVGtof1xNi8tsBp53L9jbyYwK49HQnDpe6tb0nVwhHA' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@307116353_3302565903398527_525569142260037778_n.png;type=image/png'
``` 

### Request URL

```
http://localhost:8080/product/photo-upload?productId=71b26dd9-b4b5-4f87-a84d-c8daa506018a
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
http://localhost:8080/product-category?categoryName=Kids
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
    "categoryName": "Kids",
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
  'http://localhost:8080/product-category?limit=10&offset=0' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM2Mjg0MTJ9.S6EML9bKGau9HB0CE9v_Sm0rCTOi0eQzRhjd-KI6ChF8n95RC9cTwphWyUisK3tKYuS5ZwXIHfmNvup2zRK5BQ'
``` 

### Request URL

```
http://localhost:8080/product-category?limit=10&offset=0
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
      "categoryName": "Mens Cloth",
      "subCategories": []
    },
    {
      "id": "75b44e08-2c94-438f-b500-b204c7c90cca",
      "categoryName": "Kids",
      "subCategories": []
    }
  ]
}
```   
</details>

<details>
  
<summary> <code>PUT </code> <code>/product-category</code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/product-category?categoryId=58f5c085-d04a-47de-beab-1d476b6ce432&categoryName=Sports' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM2Mjg0MTJ9.S6EML9bKGau9HB0CE9v_Sm0rCTOi0eQzRhjd-KI6ChF8n95RC9cTwphWyUisK3tKYuS5ZwXIHfmNvup2zRK5BQ'
``` 

### Request URL

```
http://localhost:8080/product-category?categoryId=58f5c085-d04a-47de-beab-1d476b6ce432&categoryName=Sports
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
    "id": "58f5c085-d04a-47de-beab-1d476b6ce432",
    "categoryName": "Sports",
    "subCategories": []
  }
}
```   
</details>

<details>
  
<summary> <code>DELETE</code> <code>/product-category</code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/product-category?categoryId=75b44e08-2c94-438f-b500-b204c7c90cca' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM2Mjg0MTJ9.S6EML9bKGau9HB0CE9v_Sm0rCTOi0eQzRhjd-KI6ChF8n95RC9cTwphWyUisK3tKYuS5ZwXIHfmNvup2zRK5BQ'
``` 

### Request URL

```
http://localhost:8080/product-category?categoryId=75b44e08-2c94-438f-b500-b204c7c90cca
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
  'http://localhost:8080/product-sub-category?categoryId=58f5c085-d04a-47de-beab-1d476b6ce432&subCategoryName=Cricket%20' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM2MzAyNjR9.lutSpglUtRQxkAQY4z94OIH6j-A1-TvF2L2_6zj7YA4VL6pnaZbMq-_uPZu4CnULYXVIRnwx4vzP9CWzyrc5Mw' \
  -d ''
``` 

### Request URL

```
http://localhost:8080/product-sub-category?categoryId=58f5c085-d04a-47de-beab-1d476b6ce432&subCategoryName=Cricket%20
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
    "id": "978c50c1-70d7-405c-b323-c0d11eef714b",
    "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
    "subCategoryName": "Cricket "
  }
}
```   
</details>

<details>
  
<summary> <code>GET </code> <code>/product-sub-category/{categoryId}</code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/product-sub-category/{categoryId}?categoryId=58f5c085-d04a-47de-beab-1d476b6ce432&limit=10&offset=0' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM2MzA2ODl9.EQrEn_nAbTIH85sPOZdb1EY0y0n2gu97oRYC6QTSEDVNmqlouqR7KUx5bGqHsYVTkm9orimn8slqa0FONWyVuA'
``` 

### Request URL

```
http://localhost:8080/product-sub-category/{categoryId}?categoryId=58f5c085-d04a-47de-beab-1d476b6ce432&limit=10&offset=0
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
      "id": "978c50c1-70d7-405c-b323-c0d11eef714b",
      "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
      "subCategoryName": "Cricket "
    }
  ]
}
```   
</details>

<details>
  
<summary> <code>PUT </code> <code>/product-sub-category</code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/product-sub-category?subCategoryId=978c50c1-70d7-405c-b323-c0d11eef714b&subCategoryName=Football' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM2MzA2ODl9.EQrEn_nAbTIH85sPOZdb1EY0y0n2gu97oRYC6QTSEDVNmqlouqR7KUx5bGqHsYVTkm9orimn8slqa0FONWyVuA'
``` 

### Request URL

```
http://localhost:8080/product-sub-category?subCategoryId=978c50c1-70d7-405c-b323-c0d11eef714b&subCategoryName=Football
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
    "id": "978c50c1-70d7-405c-b323-c0d11eef714b",
    "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
    "subCategoryName": "Football"
  }
}
```   
</details>


<details>
  
<summary> <code>DELETE </code> <code>/product-sub-category</code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/product-sub-category?subCategoryId=978c50c1-70d7-405c-b323-c0d11eef714b' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM2MzA2ODl9.EQrEn_nAbTIH85sPOZdb1EY0y0n2gu97oRYC6QTSEDVNmqlouqR7KUx5bGqHsYVTkm9orimn8slqa0FONWyVuA'
``` 

### Request URL

```
http://localhost:8080/product-sub-category?subCategoryId=978c50c1-70d7-405c-b323-c0d11eef714b
``` 


### Response
```
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "978c50c1-70d7-405c-b323-c0d11eef714b"
}
```   
</details>

### BRAND
<details>
  
<summary> <code>POST </code> <code>/brand </code></summary>

### Curl

```
curl -X 'POST' \
  'http://localhost:8080/brand?brandName=Nike' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM5MDU0Mzl9.Jrn49AipUud_MkH4NbOtBNy9AsAwGE3W2wnW-dnUMifhEaijeaSbwn-jlUsCMPf1ayos2K0pQZma4LmWwuivPg' \
  -d ''
``` 

### Request URL

```
http://localhost:8080/brand?brandName=Nike
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
    "brandName": "Nike"
  }
}
```   
</details>

<details>
  
<summary> <code>GET </code> <code>/brand </code></summary>

### Curl

```
curl -X 'GET' \
  'http://localhost:8080/brand?limit=10&offset=0' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM5MDU0Mzl9.Jrn49AipUud_MkH4NbOtBNy9AsAwGE3W2wnW-dnUMifhEaijeaSbwn-jlUsCMPf1ayos2K0pQZma4LmWwuivPg'
``` 

### Request URL

```
http://localhost:8080/brand?limit=10&offset=0
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
      "brandName": "Nike"
    },
    {
      "id": "19dd1021-432c-473c-8b19-0f56d19af9ad",
      "brandName": "PUMA"
    }
  ]
}
```   
</details>

<details>
  
<summary> <code>PUT </code> <code>/brand </code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/brand?brandId=6c5078d3-f8e3-4c88-9afe-48b5423c664f&brandName=Addidas' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM5MDU0Mzl9.Jrn49AipUud_MkH4NbOtBNy9AsAwGE3W2wnW-dnUMifhEaijeaSbwn-jlUsCMPf1ayos2K0pQZma4LmWwuivPg'
``` 

### Request URL

```
http://localhost:8080/brand?brandId=6c5078d3-f8e3-4c88-9afe-48b5423c664f&brandName=Addidas
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
    "brandName": "Addidas"
  }
}
```   
</details>

<details>
  
<summary> <code>DELETE</code> <code>/brand </code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/brand?brandId=19dd1021-432c-473c-8b19-0f56d19af9ad' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6ImU0Yjk2YWU0LTNjYTItNDQ1OC1hNTczLWUwOTI5YTUyMTcxOSIsInVzZXJUeXBlIjoiYWRtaW4iLCJleHAiOjE2OTM5MDU0Mzl9.Jrn49AipUud_MkH4NbOtBNy9AsAwGE3W2wnW-dnUMifhEaijeaSbwn-jlUsCMPf1ayos2K0pQZma4LmWwuivPg'
``` 

### Request URL

```
http://localhost:8080/brand?brandId=19dd1021-432c-473c-8b19-0f56d19af9ad
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
  'http://localhost:8080/cart?limit=10&offset=0' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6Ijg5YThhMGQ1LWQyNWMtNDBiYi05ZmRmLTc1MWM1YTAxNWUzNyIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5NDM1NTk4N30.rq2rnhBUoAEEoImdqhD7dEo0UnkEFHb5q9cOC-AQ_Gjaf2pE0R7eu15MGn12kp5KJkJQIYx5jB5Tpn3OaphuGQ'
``` 

### Request URL

```
http://localhost:8080/cart?limit=10&offset=0
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
  
<summary> <code>PUT </code> <code>/cart/{productId} </code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/cart/5b24d429-c981-47c8-9318-f4d61dd2c1a4?quantity=1' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6Ijg5YThhMGQ1LWQyNWMtNDBiYi05ZmRmLTc1MWM1YTAxNWUzNyIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5NDM1NTk4N30.rq2rnhBUoAEEoImdqhD7dEo0UnkEFHb5q9cOC-AQ_Gjaf2pE0R7eu15MGn12kp5KJkJQIYx5jB5Tpn3OaphuGQ'
``` 

### Request URL

```
http://localhost:8080/cart/5b24d429-c981-47c8-9318-f4d61dd2c1a4?quantity=1
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
  
<summary> <code>DELETE </code> <code>/cart/{productId} </code></summary>

### Curl

```
curl -X 'DELETE' \
  'http://localhost:8080/cart/71b26dd9-b4b5-4f87-a84d-c8daa506018a' \
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
  'http://localhost:8080/order?limit=10&offset=0' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6Ijg5YThhMGQ1LWQyNWMtNDBiYi05ZmRmLTc1MWM1YTAxNWUzNyIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5NDk1NjIzNX0.Go4nLzsiOruUKTtETn1Yc35BNjlo79_3Vs8LfW9LJG1nvogqIR0mG9JQOUxP8YclsVBzGV0j0IIv7svTaDMxTg'
``` 

### Request URL

```
http://localhost:8080/order?limit=10&offset=0
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
  
<summary> <code>PUT </code> <code>/order/paymment </code></summary>

### Curl

```
curl -X 'PUT' \
  'http://localhost:8080/order/payment?orderId=04675b54-a9df-4200-a526-0b15f6a85930' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoZW50aWNhdGlvbiIsImlzcyI6Imt0b3IuaW8iLCJlbWFpbCI6InBpYXNoNTk5QGdtYWlsLmNvbSIsInVzZXJJZCI6Ijg5YThhMGQ1LWQyNWMtNDBiYi05ZmRmLTc1MWM1YTAxNWUzNyIsInVzZXJUeXBlIjoidXNlciIsImV4cCI6MTY5NDk1OTMwMn0.0KN6NpFd7kZALAm6jiT44isutsEQ_r84YnfKczjbPjQSdlkBNU_M2yey4wySRnYtf2m4UsaA4ogAVbJEXlv1wA'
``` 

### Request URL

```
http://localhost:8080/order/payment?orderId=04675b54-a9df-4200-a526-0b15f6a85930
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
    "orderId": "04675b54-a9df-4200-a526-0b15f6a85930",
    "quantity": 1,
    "subTotal": 10,
    "total": 10,
    "shippingCharge": 100,
    "cancelOrder": false,
    "status": "paid",
    "statusCode": 2
  }
}
```   
</details>

## 👨 Developed By

<a href="https://twitter.com/piashcse" target="_blank">
  <img src="https://avatars.githubusercontent.com/piashcse" width="80" align="left">
</a>

**Mehedi Hassan Piash**

[![Twitter](https://img.shields.io/badge/-twitter-grey?logo=twitter)](https://twitter.com/piashcse)
[![Web](https://img.shields.io/badge/-web-grey?logo=appveyor)](https://piashcse.github.io/)
[![Medium](https://img.shields.io/badge/-medium-grey?logo=medium)](https://medium.com/@piashcse)
[![Linkedin](https://img.shields.io/badge/-linkedin-grey?logo=linkedin)](https://www.linkedin.com/in/piashcse/)

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
