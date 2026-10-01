-- ───────── 회원: 이메일 대신 휴대폰 번호로 가입·본인 확인 ─────────
-- 같은 번호로 두 계정을 만들 수 없다 (예전 계정처럼 번호가 없는 회원은 제외)
CREATE UNIQUE INDEX ux_members_phone ON members (phone_number) WHERE phone_number IS NOT NULL;

-- 휴대폰 인증번호. 번호(code)는 해시만 저장하고, 인증에 성공하면 한 번만 쓸 수 있는 토큰을 발급한다.
CREATE TABLE phone_verifications (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    verified_at TIMESTAMP,
    token VARCHAR(64) UNIQUE,
    token_expires_at TIMESTAMP,
    used_at TIMESTAMP
);

CREATE INDEX idx_phone_verifications_phone ON phone_verifications (phone_number, purpose, created_at);

-- ───────── 로그인 유지: 리프레시 토큰 ─────────
-- 토큰 원문은 저장하지 않고 SHA-256 해시만 둔다. 쓸 때마다 새 토큰으로 바꾼다(rotation).
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    member_id BIGINT NOT NULL REFERENCES members (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_member ON refresh_tokens (member_id);

-- ───────── 알림(문자·카카오 알림톡) 발송 기록 ─────────
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    member_id BIGINT REFERENCES members (id) ON DELETE SET NULL,
    phone_number VARCHAR(20) NOT NULL,
    type VARCHAR(30) NOT NULL,
    channel VARCHAR(20),
    content VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error VARCHAR(500),
    sent_at TIMESTAMP
);

CREATE INDEX idx_notifications_created ON notifications (created_at DESC);

-- ───────── 예약: 결제 마감 시각 (지나면 자동 취소) ─────────
ALTER TABLE stores ADD COLUMN payment_deadline TIMESTAMP;

-- ───────── 지점: 지도 좌표 ─────────
ALTER TABLE storage_places ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE storage_places ADD COLUMN longitude DOUBLE PRECISION;

UPDATE storage_places SET latitude = 37.5572, longitude = 126.9245 WHERE code = 'HONGDAE';
UPDATE storage_places SET latitude = 37.5552, longitude = 126.9369 WHERE code = 'SINCHON';
UPDATE storage_places SET latitude = 37.4979, longitude = 127.0276 WHERE code = 'GANGNAM';
UPDATE storage_places SET latitude = 37.5404, longitude = 127.0692 WHERE code = 'KONKUK';
UPDATE storage_places SET latitude = 37.4812, longitude = 126.9527 WHERE code = 'SNU';
UPDATE storage_places SET latitude = 37.5547, longitude = 126.9707 WHERE code = 'SEOULSTN';
UPDATE storage_places SET latitude = 37.5822, longitude = 127.0018 WHERE code = 'HYEHWA';
UPDATE storage_places SET latitude = 37.5133, longitude = 127.1001 WHERE code = 'JAMSIL';

-- ───────── 운영 신청: 운영자는 지점을 바로 맡지 않고 신청하고, 관리자가 승인한다 ─────────
CREATE TABLE branch_applications (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    place_id BIGINT NOT NULL REFERENCES storage_places (id),
    applicant_id BIGINT NOT NULL REFERENCES members (id) ON DELETE CASCADE,
    capacity INT NOT NULL,
    description VARCHAR(500),
    message VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    decided_at TIMESTAMP,
    reject_reason VARCHAR(300)
);

CREATE INDEX idx_branch_applications_status ON branch_applications (status, created_at);
-- 한 사람이 같은 지점에 심사 중인 신청을 여러 개 낼 수 없다
CREATE UNIQUE INDEX ux_branch_applications_pending
    ON branch_applications (place_id, applicant_id) WHERE status = 'PENDING';
