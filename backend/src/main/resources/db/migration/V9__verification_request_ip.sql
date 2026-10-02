-- 인증 문자를 요청한 곳(IP)을 남겨서, 번호를 바꿔 가며 문자를 보내게 하는 남용(문자 비용)을 IP별로 막는다.
ALTER TABLE phone_verifications ADD COLUMN request_ip VARCHAR(64);
CREATE INDEX idx_phone_verifications_ip ON phone_verifications (request_ip, created_at);
-- 하루 전체 발송 상한을 셀 때 쓴다
CREATE INDEX idx_phone_verifications_created ON phone_verifications (created_at);
