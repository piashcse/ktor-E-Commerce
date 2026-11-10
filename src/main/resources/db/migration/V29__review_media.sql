-- Review media: product reviews can carry a small gallery of customer images.
CREATE TABLE review_image (
    id         VARCHAR(50) PRIMARY KEY,
    created_at TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at TIMESTAMP,
    review_id  VARCHAR(50)  NOT NULL REFERENCES review_rating(id) ON DELETE CASCADE,
    image_url  VARCHAR(500) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX review_image_review_id_idx ON review_image(review_id);
