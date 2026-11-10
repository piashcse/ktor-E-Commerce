#!/usr/bin/env python3
"""Generate README-style <details> endpoint blocks from the Ktor OpenAPI document.

The root README documents each operation as:

    <details>
    <summary> <code>GET</code> <code>/path</code></summary>
    ...
    </details>

This script detects which operations are not yet documented and renders the
missing ones (request body/response examples come from the OpenAPI schemas).

Usage:
    ./gradlew test --tests 'com.piashcse.plugin.OpenApiGenerationTest'
    pip install pyyaml
    python3 scripts/generate_readme_endpoints.py build/endpoints.yaml README.md
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

import yaml

METHODS = {"get", "post", "put", "patch", "delete"}
BASE = "http://localhost:8080"

# Tag -> README module heading. Existing headings are reused; new headings are
# appended after the last existing API module.
MODULE_HEADINGS = {
    "Auth": "### AUTH",
    "Profile": "### PROFILE",
    "Shop": "### SHOP",
    "Shop Category": "### SHOP CATEGORY",
    "Brand": "### BRAND",
    "Product Category": "### PRODUCT CATEGORY",
    "Product Sub Category": "### PRODUCT SUB CATEGORY",
    "Product": "### PRODUCT",
    "Recommendation": "### RECOMMENDATIONS",
    "Review": "### REVIEW RATING",
    "Wishlist": "### WISHLIST",
    "Cart": "### CART",
    "Checkout": "### CHECKOUT",
    "Order": "### ORDER",
    "Payment": "### PAYMENT",
    "Coupon": "### COUPON",
    "Shipping Method": "### SHIPPING METHOD",
    "Inventory": "### INVENTORY",
    "Warehouse": "### WAREHOUSES",
    "Refund": "### REFUND REQUEST",
    "RMA": "### RMA / RETURNS",
    "Notification": "### NOTIFICATIONS",
    "KYC": "### SELLER KYC",
    "Payout": "### PAYOUTS",
    "Tax": "### TAX RULES",
    "FX": "### FX RATES",
    "API Keys": "### API KEYS",
    "Feature Flags": "### FEATURE FLAGS",
    "Outbox": "### OUTBOX & DEAD LETTERS",
    "Policy": "### PRIVACY POLICY",
    "Consent": "### PRIVACY POLICY CONSENT",
    "Privacy": "### PRIVACY / GDPR",
    "Dashboard": "### DASHBOARD",
    "Audit Log": "### AUDIT LOG",
    "Health": "### HEALTH & OPERATIONS",
}

PATH_PARAM_EXAMPLES = {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "orderId": "order-uuid",
    "orderItemId": "order-item-uuid",
    "productId": "product-uuid",
    "shopId": "shop-uuid",
    "categoryId": "category-uuid",
    "sellerId": "seller-uuid",
    "userId": "user-uuid",
    "variantId": "variant-uuid",
    "optionId": "option-uuid",
    "valueId": "value-uuid",
    "fileName": "receipt.png",
    "targetCurrency": "EUR",
    "key": "guest_checkout",
    "policyType": "PRIVACY_POLICY",
    "type": "PRIVACY_POLICY",
    "code": "SUMMER20",
    "eventType": "REFUND_APPROVED",
    "currency": "EUR",
}


def access_for(path: str, operation: dict) -> str:
    if not operation.get("security"):
        return "Public"
    if "/admin/" in path:
        return "Admin"
    if "/seller/" in path:
        return "Seller"
    if path == "/api/v1/orders/status/{id}":
        return "Customer / Seller"
    if path == "/api/v1/notifications/send":
        return "Admin / Seller"
    if path.endswith("/policy-consents/{id}"):
        return "Owner / Admin"
    return "Authenticated"


def ref_name(schema: dict) -> str | None:
    ref = schema.get("$ref") if isinstance(schema, dict) else None
    return ref.rsplit("/", 1)[-1] if ref else None


def string_example(name: str, schema: dict) -> str:
    if "enum" in schema and schema["enum"]:
        return str(schema["enum"][0])
    lowered = name.lower()
    if "email" in lowered:
        return "user@example.com"
    if any(k in lowered for k in ("price", "amount", "total", "rate", "taxamount", "taxrate", "discount", "balance", "weight")):
        return "19.99"
    if "password" in lowered:
        return "Password123!"
    if "token" in lowered:
        return "eyJhbGciOiJIUzUxMiJ9.example"
    if any(k in lowered for k in ("image", "logo", "url", "document")):
        return "/uploads/example.png"
    if "phone" in lowered or "mobile" in lowered:
        return "+8801700000000"
    if "date" in lowered or lowered.endswith("at"):
        return "2024-05-06T12:00:00"
    if "country" in lowered:
        return "BD"
    if "currency" in lowered:
        return "USD"
    if "code" in lowered:
        return "CODE-001"
    if "reason" in lowered:
        return "Reason for the request"
    if "name" in lowered:
        return "Example Name"
    if "status" in lowered:
        return "ACTIVE"
    if "type" in lowered:
        return "TYPE"
    if lowered == "id" or lowered.endswith("id"):
        return "uuid"
    return "string"


def example_for_schema(
    schema: dict,
    components: dict,
    field_name: str = "",
    seen: set[str] | None = None,
    depth: int = 0,
):
    seen = seen or set()
    if depth > 8:
        return None
    if not isinstance(schema, dict):
        return None

    name = ref_name(schema)
    if name:
        if name in seen:
            return {}
        target = components.get(name, {})
        return example_for_schema(target, components, field_name, seen | {name}, depth + 1)

    if "allOf" in schema:
        merged: dict = {}
        for part in schema["allOf"]:
            value = example_for_schema(part, components, field_name, seen, depth + 1)
            if isinstance(value, dict):
                merged.update(value)
        return merged

    for key in ("oneOf", "anyOf"):
        if schema.get(key):
            variants = [
                item
                for item in schema[key]
                if not (isinstance(item, dict) and item.get("type") == "null")
            ]
            if variants:
                return example_for_schema(variants[0], components, field_name, seen, depth + 1)
            return None

    kind = schema.get("type")
    if isinstance(kind, list):
        non_null = [item for item in kind if item != "null"]
        kind = non_null[0] if non_null else "null"
    if kind == "object" or "properties" in schema:
        result = {}
        for prop, prop_schema in schema.get("properties", {}).items():
            result[prop] = example_for_schema(prop_schema, components, prop, seen, depth + 1)
        return result
    if kind == "array":
        return [example_for_schema(schema.get("items", {}), components, field_name, seen, depth + 1)]
    if kind == "integer":
        lowered = field_name.lower()
        if any(k in lowered for k in ("count", "quantity", "stock", "limit", "attempt")):
            return 1
        return 1
    if kind == "number":
        lowered = field_name.lower()
        if any(k in lowered for k in ("price", "amount", "total", "rate", "tax", "discount", "balance")):
            return "19.99"
        return 1
    if kind == "boolean":
        return True
    if kind == "string":
        return string_example(field_name, schema)
    return None


def example_for_content(content: dict, components: dict):
    for mime in ("application/json", "application/*+json"):
        if mime in content:
            schema = content[mime].get("schema", {})
            return example_for_schema(schema, components)
    return None


def path_with_values(path: str) -> str:
    def repl(match: re.Match[str]) -> str:
        name = match.group(1)
        return PATH_PARAM_EXAMPLES.get(name, f"{name}-value")

    return re.sub(r"\{([^}]+)\}", repl, path)


def query_string(parameters: list[dict], required_only: bool = False) -> tuple[str, str]:
    """Return (curl query, documented query)."""
    curl_parts: list[str] = []
    doc_parts: list[str] = []
    for param in parameters:
        if param.get("in") != "query":
            continue
        name = param["name"]
        required = bool(param.get("required"))
        if required_only and not required:
            # Still add common pagination to make examples useful.
            if name not in {"limit", "offset"}:
                continue
        if name in {"page", "perPage"} and not required:
            continue
        if name == "offset" and not required:
            continue
        example = "20" if name in {"limit", "perPage"} else "0" if name == "offset" else ""
        if not example:
            schema = param.get("schema", {})
            example = str(example_for_schema(schema, {}, name) or "")
        curl_parts.append(f"{name}={example}")
        doc_parts.append(f"{name}={{{name}}}")
    curl = ("?" + "&".join(curl_parts)) if curl_parts else ""
    doc = ("?" + "&".join(doc_parts)) if doc_parts else ""
    return curl, doc


def heading_pattern(heading: str) -> re.Pattern[str]:
    return re.compile(rf"(?m)^{re.escape(heading)}$")


def find_heading(
    text: str,
    heading: str,
    start: int = 0,
) -> int:
    match = heading_pattern(heading).search(text, start)
    return match.start() if match else -1


def render_block(
    path: str,
    method: str,
    operation: dict,
    components: dict,
    tag: str,
) -> str:
    summary = operation.get("summary") or f"{method.upper()} {path}"
    description = operation.get("description") or summary
    access = access_for(path, operation)

    parameters = operation.get("parameters", [])
    curl_query, doc_query = query_string(parameters)
    curl_url = f"{BASE}{path_with_values(path)}{curl_query}"
    doc_url = f"{BASE}{path}{doc_query}"

    is_csv = path.endswith("/export")
    is_binary = "/evidence/" in path
    accept = "text/csv" if is_csv else "image/*" if is_binary else "application/json"

    headers = [f"  -H 'accept: {accept}' \\"]
    if access != "Public":
        token = "<admin-token>" if access.startswith("Admin") else "<seller-token>" if access == "Seller" else "<token>"
        headers.append(f"  -H 'Authorization: Bearer {token}' \\")

    body = operation.get("requestBody", {})
    body_json = None
    multipart = False
    if body:
        content = body.get("content", {})
        multipart = "multipart/form-data" in content
        body_json = example_for_content(content, components)

    curl_lines = [f"curl -X '{method.upper()}' \\", f"  '{curl_url}' \\", *headers]
    if multipart:
        curl_lines.append("  -F 'file=@image.png'")
    elif body_json is not None:
        curl_lines.append("  -H 'Content-Type: application/json' \\")
        curl_lines.append(f"  -d '{json.dumps(body_json, indent=2)}'")
    else:
        curl_lines[-1] = curl_lines[-1].rstrip(" \\")

    response = None
    for code in ("200", "201", "202", "204"):
        if code in operation.get("responses", {}):
            response = operation["responses"][code]
            break
    if response is None:
        response = next(iter(operation.get("responses", {}).values()), {})
    response_example = example_for_content(response.get("content", {}), components)
    if response_example is None:
        response_example = {"message": "OK"}

    if path == "/health/ready":
        response_section = '```json\n{\n  "message": "READY"\n}\n```'
    elif path == "/health/live":
        response_section = '```json\n{\n  "message": "UP"\n}\n```'
    elif is_csv:
        response_section = "```csv\nid,status,amount,currency,createdAt\nexample-id,PAID,250.00,USD,2024-05-06T12:00:00\n```"
    elif is_binary:
        response_section = "Binary image bytes. The response `Content-Type` is derived from the stored file extension."
    else:
        response_section = f"```json\n{json.dumps(response_example, indent=2)}\n```"

    return f"""<details>
<summary> <code>{method.upper()}</code> <code>{path}</code></summary>

### Description
{description}

### Curl
```bash
{chr(10).join(curl_lines)}
```

### Request URL
```text
{doc_url}
```

### Response
{response_section}
</details>
"""


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__)
        return 1

    source, readme_path = Path(sys.argv[1]), Path(sys.argv[2])
    document = yaml.safe_load(source.read_text())
    components = document.get("components", {}).get("schemas", {})
    readme = readme_path.read_text()

    documented = {
        (method.upper(), path)
        for method, path in re.findall(
            r"<summary>\s*<code>(GET|POST|PUT|PATCH|DELETE)</code>\s*<code>([^<]+)</code>\s*</summary>",
            readme,
            re.I,
        )
    }

    groups: dict[str, list[str]] = {}
    missing = 0
    for path, operations in document.get("paths", {}).items():
        for method, operation in operations.items():
            method_upper = method.upper()
            if method.upper() not in {m.upper() for m in METHODS}:
                continue
            key = (method_upper, path)
            if key in documented:
                continue
            tag = (operation.get("tags") or ["Other"])[0]
            groups.setdefault(tag, []).append(render_block(path, method_upper, operation, components, tag))
            missing += 1

    if not missing:
        print("README already documents every operation")
        return 0

    # Remove the generated summary tables from a previous pass (if present).
    module_headings = set(MODULE_HEADINGS.values())
    for heading in module_headings:
        readme = re.sub(
            rf"(?m)^{re.escape(heading)}\n\n(?:\|[^\n]*\|\n)+\n?",
            f"{heading}\n\n",
            readme,
            count=1,
        )

    # Remove the old hand-written "Additional API Modules" section so the
    # generated details are the single source of truth for new modules.
    readme = re.sub(
        r"(?ms)^## 🧩 Additional API Modules\n.*?(?=^## 👨 Developed By)",
        "",
        readme,
    )

    insert_at = readme.index("## 👨 Developed By")
    existing, new_sections = [], []
    for tag, blocks in groups.items():
        heading = MODULE_HEADINGS.get(tag, f"### {tag.upper()}")
        section = f"{heading}\n\n" + "\n".join(blocks)
        if find_heading(readme, heading) != -1:
            existing.append((heading, "\n".join(blocks)))
        else:
            new_sections.append((tag, section))

    # Append blocks to existing module sections (before the next module heading).
    for heading, blocks in existing:
        start = find_heading(readme, heading) + len(heading)
        next_positions = [
            find_heading(readme, other, start)
            for other in module_headings
            if find_heading(readme, other, start) != -1
        ]
        end = min(next_positions) if next_positions else insert_at
        readme = readme[:end].rstrip() + "\n\n" + blocks + "\n\n" + readme[end:]

    # Append brand-new module sections before the author block, in nav order.
    if new_sections:
        order = list(MODULE_HEADINGS)
        new_sections.sort(key=lambda item: order.index(item[0]) if item[0] in order else 999)
        block = "\n\n".join(section for _, section in new_sections)
        insert_at = readme.index("## 👨 Developed By")
        readme = readme[:insert_at].rstrip() + "\n\n" + block + "\n\n" + readme[insert_at:]

    readme_path.write_text(readme)
    print(f"Added {missing} endpoint blocks in {len(groups)} modules")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
