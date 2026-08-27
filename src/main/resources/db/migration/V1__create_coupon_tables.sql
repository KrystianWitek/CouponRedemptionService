CREATE TABLE coupon
(
    id                  UUID PRIMARY KEY,
    code                VARCHAR(100)             NOT NULL UNIQUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    max_usage_count     INTEGER                  NOT NULL,
    current_usage_count INTEGER                  NOT NULL,
    country_code        VARCHAR(2)               NOT NULL
);

CREATE TABLE coupon_redemption
(
    id          UUID PRIMARY KEY,
    coupon_id   UUID                     NOT NULL REFERENCES coupon (id),
    user_id     VARCHAR(255)             NOT NULL,
    redeemed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (coupon_id, user_id)
);
