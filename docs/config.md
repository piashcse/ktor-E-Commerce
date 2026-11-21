# Storefront Runtime Configuration

Bootstrap configuration for storefront applications, mobile clients, and guest sessions.

---

## Overview

The runtime configuration endpoint is a **public, unauthenticated** endpoint designed to be invoked by web and mobile storefronts during client initialization. It informs the frontend about active feature toggles, accepted payment methods, enabled currencies, and store policies without requiring a client re-deploy.

---

## Endpoint Details

### 1. Get Public Configuration

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/config</span>
  <span class="auth-badge public">Public / No Auth Required</span>
</div>

Retrieves all storefront runtime switches, currencies, and payment capabilities.

#### Headers

| Header | Value | Required | Description |
|:---|:---|:---|:---|
| `Accept` | `application/json` | <span class="badge-opt">Optional</span> | Expected response format |

#### Request Examples

=== "cURL"
    ```bash
    curl -X GET 'http://localhost:8080/api/v1/config' \
      -H 'Accept: application/json'
    ```

=== "Kotlin (Ktor Client)"
    ```kotlin
    val response = client.get("http://localhost:8080/api/v1/config")
    val config: PublicConfigResponse = response.body()
    ```

=== "TypeScript (Fetch)"
    ```typescript
    const res = await fetch("http://localhost:8080/api/v1/config");
    const config: PublicConfigResponse = await res.json();
    ```

#### Response

=== "200 OK"
    ```json
    {
      "guestCheckout": true,
      "requirePolicyConsent": false,
      "supportedCurrencies": [
        "CAD",
        "EUR",
        "GBP",
        "USD"
      ],
      "baseCurrency": "USD",
      "paymentMethods": [
        "COD",
        "STRIPE",
        "WALLET"
      ],
      "paymentProvider": "STRIPE",
      "freeShippingThreshold": "75.00",
      "returnWindowDays": 30
    }
    ```

---

## Response Field Definitions

| Field | Type | Description |
|:---|:---|:---|
| `guestCheckout` | boolean | When `true`, customers can place orders without creating a password-authenticated account. |
| `requirePolicyConsent` | boolean | When `true`, order placement strictly requires prior consent to terms and privacy policies. |
| `supportedCurrencies` | string[] | List of ISO 4217 currency codes accepted for pricing and multi-currency checkout. |
| `baseCurrency` | string | Store's default reference ledger currency (e.g. `"USD"`). |
| `paymentMethods` | string[] | List of currently usable payment methods (e.g. `"COD"`, `"STRIPE"`, `"SSLCOMMERZ"`, `"WALLET"`). Prevents showing unpayable options. |
| `paymentProvider` | string | Configured payment gateway provider backend (e.g. `"MANUAL"`, `"STRIPE"`). |
| `freeShippingThreshold` | string | Minimum cart subtotal qualifying for free ground shipping. |
| `returnWindowDays` | integer | Maximum number of days after delivery within which customers may initiate return or RMA requests. |
