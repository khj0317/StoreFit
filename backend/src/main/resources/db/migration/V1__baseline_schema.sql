-- Full schema for a brand new database. Every statement uses IF NOT EXISTS because on an
-- existing dev DB (one that ddl-auto: update already built up piece by piece) these tables
-- are already there -- Flyway's baseline-on-migrate treats whatever already exists as version
-- 0, and this just fills in anything a fresh checkout/environment is missing.
CREATE TABLE IF NOT EXISTS members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone_number VARCHAR(255),
    role VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS stores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    member_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    address VARCHAR(255) NOT NULL,
    category ENUM('CLOTHES','LIGHT','MEDIUM','OTHER') NOT NULL,
    luggage_count INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status ENUM('COMPLETED','IN_USE','PENDING','PICKED_UP') NOT NULL,
    total_price INT NOT NULL,
    CONSTRAINT fk_stores_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    store_id BIGINT NOT NULL UNIQUE,
    amount INT NOT NULL,
    approved_at TIMESTAMP,
    method VARCHAR(255),
    order_id VARCHAR(255) NOT NULL UNIQUE,
    payment_key VARCHAR(255),
    status ENUM('CANCELED','DONE','FAILED','READY') NOT NULL,
    CONSTRAINT fk_payments_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    store_id BIGINT NOT NULL UNIQUE,
    content VARCHAR(255),
    rating INT NOT NULL,
    CONSTRAINT fk_reviews_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE IF NOT EXISTS store_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT fk_store_images_store FOREIGN KEY (store_id) REFERENCES stores (id)
);
