-- 처음 만드는 DB의 전체 스키마 (PostgreSQL / Supabase).
-- 예전 MySQL·H2 버전에서 옮기면서 AUTO_INCREMENT → BIGSERIAL, ENUM → VARCHAR로 바꿨다.
-- enum 값은 JPA가 @Enumerated(STRING)으로 검증하므로 DB에는 문자열로 둔다.
CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone_number VARCHAR(255),
    role VARCHAR(20) NOT NULL
);

CREATE TABLE stores (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    member_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    address VARCHAR(255) NOT NULL,
    category VARCHAR(20) NOT NULL,
    luggage_count INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_price INT NOT NULL,
    CONSTRAINT fk_stores_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    store_id BIGINT NOT NULL UNIQUE,
    amount INT NOT NULL,
    approved_at TIMESTAMP,
    method VARCHAR(255),
    order_id VARCHAR(255) NOT NULL UNIQUE,
    payment_key VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_payments_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    store_id BIGINT NOT NULL UNIQUE,
    content VARCHAR(255),
    rating INT NOT NULL,
    CONSTRAINT fk_reviews_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE store_images (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT fk_store_images_store FOREIGN KEY (store_id) REFERENCES stores (id)
);
