# CMS, SEO & Sitemap

Storefront content pages, per-entity SEO metadata, XML sitemap and robots policy.

**Base URL:** `http://localhost:8080`

## CMS pages

Pages are addressed by a lowercase slug.

```json
{
  "slug": "about-us",
  "title": "About us",
  "excerpt": "Company and story",
  "body": "<p>Hello</p>",
  "status": "PUBLISHED",
  "seoTitle": "About our company",
  "seoDescription": "Who we are and what we do",
  "canonicalUrl": "https://shop.example/pages/about-us",
  "ogImage": "https://cdn.example/about.jpg"
}
```

Statuses: `DRAFT`, `PUBLISHED`, `ARCHIVED`.

Public:

- `GET /api/v1/cms/pages` — published pages, newest first
- `GET /api/v1/cms/pages/{slug}` — one published page

Admin:

- `GET /api/v1/admin/cms/pages?status=`
- `POST /api/v1/admin/cms/pages`
- `PUT /api/v1/admin/cms/pages/{id}`
- `DELETE /api/v1/admin/cms/pages/{id}` — archives

Publishing sets `publishedAt`; unpublishing clears it. Slugs must match
`^[a-z0-9]+(?:-[a-z0-9]+)*$`.

## SEO metadata

**`GET /api/v1/seo/metadata?entityType=PAGE&entityId=about-us&locale=en`**

Entity types: `HOME`, `PAGE`, `PRODUCT`, `CATEGORY`, `SHOP`.

Unknown metadata returns an empty shell with `noIndex = false`.

Admin upsert:

**`PUT /api/v1/admin/seo/metadata`**

```json
{
  "entityType": "PRODUCT",
  "entityId": "product-uuid",
  "locale": "fr",
  "title": "Chaise en bois",
  "description": "Chaise durable en bois massif",
  "keywords": "chaise,bois",
  "canonicalUrl": "https://shop.example/fr/products/product-uuid",
  "ogImage": "https://cdn.example/chair.jpg",
  "noIndex": false
}
```

`entityId` is required for every type except `HOME`. Metadata is unique per
`(entityType, entityId, locale)`.

## Sitemap and robots

- `GET /sitemap.xml` — home, active products, product categories and published CMS pages.
  Uses `PUBLIC_BASE_URL` for absolute URLs.
- `GET /robots.txt` — allows the storefront, disallows `/api/` and `/admin/`, points to the sitemap.

Configuration:

```bash
PUBLIC_BASE_URL=https://shop.example
```
