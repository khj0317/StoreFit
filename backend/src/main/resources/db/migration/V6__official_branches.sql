-- 보관소는 운영자가 이름·주소를 직접 만드는 게 아니라, 스토어핏이 정식 등록한 지점 중에서 고른다.
-- 지점은 code로 구분하고, 아직 운영자가 없는 지점은 owner_id가 비어 있다.
ALTER TABLE storage_places ALTER COLUMN owner_id DROP NOT NULL;
ALTER TABLE storage_places ADD COLUMN code VARCHAR(30) UNIQUE;

-- 정식 지점이 아닌, 이전 방식으로 직접 만든 보관소는 정리한다 (예약이 걸린 곳은 남긴다)
DELETE FROM storage_places p
WHERE p.code IS NULL
  AND NOT EXISTS (SELECT 1 FROM stores s WHERE s.place_id = p.id);

-- 임시 정식 지점 목록. capacity는 기본값이고, 운영자가 지점을 맡을 때 직접 정한다.
INSERT INTO storage_places (created_at, updated_at, code, name, address, description, capacity) VALUES
    (now(), now(), 'HONGDAE',  '스토어핏 홍대입구역점',   '서울 마포구 양화로 160',     '2호선 홍대입구역 9번 출구 앞', 10),
    (now(), now(), 'SINCHON',  '스토어핏 신촌점',         '서울 서대문구 신촌로 83',    '연세대·이대 사이, 계절 옷·이불 보관에 좋아요', 10),
    (now(), now(), 'GANGNAM',  '스토어핏 강남역점',       '서울 강남구 강남대로 396',   '2호선·신분당선 강남역 11번 출구', 10),
    (now(), now(), 'KONKUK',   '스토어핏 건대입구역점',   '서울 광진구 아차산로 243',   '건국대 정문 근처', 10),
    (now(), now(), 'SNU',      '스토어핏 서울대입구역점', '서울 관악구 관악로 158',     '자취생이 많은 샤로수길 입구', 10),
    (now(), now(), 'SEOULSTN', '스토어핏 서울역점',       '서울 용산구 한강대로 405',   'KTX 타기 전에 캐리어를 맡겨요', 10),
    (now(), now(), 'HYEHWA',   '스토어핏 혜화역점',       '서울 종로구 대학로 120',     '대학로 공연 보기 전 짐 보관', 10),
    (now(), now(), 'JAMSIL',   '스토어핏 잠실역점',       '서울 송파구 올림픽로 265',   '롯데월드·잠실 경기장 방문객용', 10);
