CREATE TABLE coupon
(
    id                  UUID                     NOT NULL,
    code                VARCHAR(100)             NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    max_usage_count     INTEGER                  NOT NULL,
    current_usage_count INTEGER                  NOT NULL DEFAULT 0,
    country_code        VARCHAR(2)               NOT NULL,
    CONSTRAINT pk_coupon PRIMARY KEY (id),
    CONSTRAINT uq_coupon_code UNIQUE (code),
    CONSTRAINT ck_coupon_code_not_blank CHECK (BTRIM(code) <> ''),
    CONSTRAINT ck_coupon_max_usage_count_positive CHECK (max_usage_count > 0),
    CONSTRAINT ck_coupon_current_usage_count_non_negative CHECK (current_usage_count >= 0),
    CONSTRAINT ck_coupon_usage_count_within_limit CHECK (current_usage_count <= max_usage_count),
    CONSTRAINT ck_coupon_country_code_length CHECK (CHAR_LENGTH(country_code) = 2)
);

CREATE TABLE coupon_redemption
(
    id          UUID                     NOT NULL,
    coupon_id   UUID                     NOT NULL,
    user_id     VARCHAR(255)             NOT NULL,
    redeemed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_coupon_redemption PRIMARY KEY (id),
    CONSTRAINT fk_coupon_redemption_coupon FOREIGN KEY (coupon_id) REFERENCES coupon (id),
    CONSTRAINT uq_coupon_redemption_coupon_user UNIQUE (coupon_id, user_id),
    CONSTRAINT ck_coupon_redemption_user_id_not_blank CHECK (BTRIM(user_id) <> '')
);
