# Environment Variables Configuration

This project uses the `io.github.cdimascio:dotenv-kotlin` library to manage environment variables.

## Setup

1. Create a `.env` file in the project root directory
2. Add your configuration values to the `.env` file

Example `.env` file:
```env
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ecommerce_db
DB_USER=postgres
DB_PASSWORD=postgres

# JWT Configuration
JWT_SECRET=secret
JWT_AUDIENCE=ecommerce-app
JWT_REALM=ecommerce-server
JWT_ISSUER=http://0.0.0.0:8080/

# Email Configuration
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password

# Server Configuration
SERVER_PORT=8080
```

## Usage

### Using DotEnvConfig (Recommended)

The project provides a `DotEnvConfig` object that you can use to access environment variables:

```kotlin
import com.piashcse.config.DotEnvConfig

val dbHost = DotEnvConfig.dbHost
val dbPort = DotEnvConfig.dbPort
val jwtSecret = DotEnvConfig.jwtSecret
```

### Using Environment Utility Directly

You can also use the `Environment` utility class directly:

```kotlin
import com.piashcse.utils.Environment

val dbHost = Environment.get("DB_HOST", "localhost")
val dbPort = Environment.getInt("DB_PORT", 5432)
val jwtSecret = Environment.get("JWT_SECRET", "default-secret")
```

### Adding New Environment Variables

1. Add the new variable to your `.env` file
2. Update the `DotEnvConfig` object to include the new variable:
   ```kotlin
   val newVariable: String get() = Environment.get("NEW_VARIABLE", "default-value")
   ```
3. Use the variable in your code through `DotEnvConfig.newVariable`