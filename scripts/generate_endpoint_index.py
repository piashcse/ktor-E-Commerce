#!/usr/bin/env python3
"""Generate the complete API endpoint index from the Ktor OpenAPI document.

Usage:
    ./gradlew test --tests 'com.piashcse.plugin.OpenApiGenerationTest'
    pip install pyyaml
    python3 scripts/generate_endpoint_index.py build/endpoints.yaml docs/endpoints.md
    python3 scripts/generate_readme_endpoints.py build/endpoints.yaml README.md
"""

from __future__ import annotations

import sys
from pathlib import Path

import yaml

TAG_ORDER = [
    "Auth",
    "Profile",
    "Shop",
    "Shop Category",
    "Brand",
    "Product Category",
    "Product Sub Category",
    "Product",
    "Recommendation",
    "Review",
    "Wishlist",
    "Cart",
    "Checkout",
    "Order",
    "Payment",
    "Coupon",
    "Shipping Method",
    "Inventory",
    "Warehouse",
    "Refund",
    "RMA",
    "Notification",
    "KYC",
    "Payout",
    "Tax",
    "FX",
    "API Keys",
    "Feature Flags",
    "Outbox",
    "Policy",
    "Consent",
    "Privacy",
    "Dashboard",
    "Audit Log",
    "Health",
]

METHOD_ORDER = ["GET", "POST", "PUT", "PATCH", "DELETE"]


def auth_for(path: str, operation: dict) -> str:
    """Infer the intended access level from the route shape + OpenAPI security."""
    if not operation.get("security"):
        return "Public"
    if "/admin/" in path:
        return "Admin"
    if "/seller/" in path:
        return "Seller"
    # A few explicitly multi-role customer/seller endpoints.
    if path == "/api/v1/orders/status/{id}":
        return "Customer / Seller"
    if path == "/api/v1/notifications/send":
        return "Admin / Seller"
    if path.endswith("/policy-consents/{id}"):
        return "Owner / Admin"
    return "Authenticated"


def escape(value: str) -> str:
    return value.replace("|", "\\|").strip()


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__)
        return 1

    source, target = Path(sys.argv[1]), Path(sys.argv[2])
    document = yaml.safe_load(source.read_text())
    paths = document.get("paths", {})

    groups: dict[str, list[tuple[str, str, str, str]]] = {}
    total = 0
    for path, operations in paths.items():
        for method, operation in operations.items():
            method = method.upper()
            if method not in METHOD_ORDER:
                continue
            tags = operation.get("tags") or ["Other"]
            tag = tags[0]
            groups.setdefault(tag, []).append(
                (path, method, operation.get("summary", ""), auth_for(path, operation)),
            )
            total += 1

    ordered_tags = [tag for tag in TAG_ORDER if tag in groups]
    ordered_tags += sorted(tag for tag in groups if tag not in ordered_tags)

    lines = [
        "# Complete API Endpoint Index",
        "",
        "Canonical, generated index of every HTTP operation exposed by the service.",
        "It is produced from the same OpenAPI document that powers Swagger UI, so it",
        "cannot drift from the route definitions.",
        "",
        f"**Operations:** {total}  ",
        f"**Tags:** {len(ordered_tags)}  ",
        "**Base path:** `/api/v1` unless noted otherwise",
        "",
        "> Regenerate after changing routes:",
        ">",
        "> ```bash",
        "> ./gradlew test --tests 'com.piashcse.plugin.OpenApiGenerationTest'",
        "> pip install pyyaml",
        "> python3 scripts/generate_endpoint_index.py build/endpoints.yaml docs/endpoints.md",
        "> python3 scripts/generate_readme_endpoints.py build/endpoints.yaml README.md",
        "> ```",
        "",
        "## Operation summary",
        "",
        "| Tag | Operations |",
        "|---|---:|",
    ]
    for tag in ordered_tags:
        lines.append(f"| [{tag}](#{tag.lower().replace(' ', '-')}) | {len(groups[tag])} |")
    lines.append("")

    for tag in ordered_tags:
        lines.extend([f"## {tag}", "", "| Method | Endpoint | Description | Access |", "|---|---|---|---|"])
        rows = sorted(groups[tag], key=lambda row: (row[0], METHOD_ORDER.index(row[1])))
        for path, method, summary, access in rows:
            lines.append(f"| `{method}` | `{escape(path)}` | {escape(summary)} | {access} |")
        lines.append("")

    target.write_text("\n".join(lines))

    print(f"Wrote {total} operations across {len(ordered_tags)} tags to {target}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
