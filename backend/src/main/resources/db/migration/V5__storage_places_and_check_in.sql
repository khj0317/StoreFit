-- 운영자(OWNER)가 등록하는 보관소. capacity는 하루에 동시에 맡을 수 있는 짐 개수.
CREATE TABLE storage_places (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    owner_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    capacity INT NOT NULL,
    CONSTRAINT fk_storage_places_owner FOREIGN KEY (owner_id) REFERENCES members (id),
    CONSTRAINT ck_storage_places_capacity CHECK (capacity > 0)
);

CREATE INDEX idx_storage_places_owner ON storage_places (owner_id);

-- 짐 보관 예약은 이제 사용자가 주소를 적는 대신 보관소를 고른다.
-- (Postgres로 옮기면서 새 DB에서 시작하므로 기존 행은 없다)
ALTER TABLE stores DROP COLUMN address;
ALTER TABLE stores ADD COLUMN place_id BIGINT NOT NULL;
ALTER TABLE stores ADD CONSTRAINT fk_stores_place FOREIGN KEY (place_id) REFERENCES storage_places (id);

-- 결제가 끝나면 발급되는 체크인 코드(QR 내용). 운영자가 스캔해야 보관이 시작·종료된다.
ALTER TABLE stores ADD COLUMN check_in_code VARCHAR(16) UNIQUE;
ALTER TABLE stores ADD COLUMN checked_in_at TIMESTAMP;
ALTER TABLE stores ADD COLUMN checked_out_at TIMESTAMP;

-- 수용량 계산은 "이 보관소에서 날짜가 겹치는 진행 중 예약"을 찾는다
CREATE INDEX idx_stores_place_dates ON stores (place_id, start_date, end_date);
CREATE INDEX idx_stores_member ON stores (member_id);

-- 취소·부분 환불
ALTER TABLE payments ADD COLUMN canceled_amount INT NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN canceled_at TIMESTAMP;
