---
hide:
  - navigation
---
# Ktor-E-Commerce

Ktor-E-Commerce is a high-performance backend solution designed for e-commerce applications built
with [ktor](https://ktor.io/docs/welcome.html). This backend leverages the power of Kotlin to provide a robust,
scalable, and efficient service for handling your e-commerce needs.

## Swagger View

<p align="center">
  <img width="100%" height="40%" src="https://raw.githubusercontent.com/piashcse/ktor-E-Commerce/master/screenshots/swagger.gif" />
</p>

## Features

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
  <img width="60%" height="60%" src="https://raw.githubusercontent.com/piashcse/ktor-E-Commerce/master/screenshots/onion_architecture.png" />
</p>
<p align="center">
<b>Fig.  Onion Architecture </b>
</p>

### Requirements

- [JAVA 11](https://jdk.java.net/11/) (or latest)
- [PostgreSQL](https://www.postgresql.org/) (latest)

## Clone the repository

```bash
git clone git@github.com:piashcse/ktor-E-Commerce.git
```

Note: some installation instructions are for mac, for windows/linux please install accordingly.

### Postgres Setup

- Create a database in postgreSQL
- Change your db name, user, and password in `resources/hikari.properties` and replace them accordingly.

```bash
dataSource.user=postgres
dataSource.password=p123
dataSource.databaseName=ktor-1.0.0
```

### PgAdmin Setup

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

<p align="center">
  </br>
  <img width="60%" height="60%" src="https://raw.githubusercontent.com/piashcse/ktor-E-Commerce/master/screenshots/ktor-postgres.png" />
</p>

## Run the project

On Terminal

```
./gradlew run
```

### 📧 SMTP Email Setup

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

## License

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
