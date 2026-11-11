-- CMS pages and SEO metadata.
CREATE TABLE IF NOT EXISTS cms_page (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    slug            VARCHAR(200) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    excerpt         TEXT,
    body            TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    seo_title       VARCHAR(200),
    seo_description VARCHAR(500),
    canonical_url   VARCHAR(500),
    og_image        VARCHAR(500),
    published_at    TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS cms_page_slug_unique ON cms_page(slug);
CREATE INDEX IF NOT EXISTS cms_page_status_idx ON cms_page(status, published_at);

CREATE TABLE IF NOT EXISTS seo_metadata (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    entity_type     VARCHAR(30)  NOT NULL,
    entity_id       VARCHAR(100),
    locale          VARCHAR(10)  NOT NULL DEFAULT 'en',
    title           VARCHAR(200),
    description     VARCHAR(500),
    keywords        VARCHAR(500),
    canonical_url   VARCHAR(500),
    og_image        VARCHAR(500),
    no_index        BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS seo_metadata_entity_unique
  ON seo_metadata(entity_type, COALESCE(entity_id, ''), locale);
