# ERD

```mermaid
erDiagram
    MEMBER ||--o{ STORE : hosts
    MEMBER ||--o{ RESERVATION : makes
    MEMBER ||--o{ REVIEW : writes
    STORE ||--o{ STORE_IMAGE : has
    STORE ||--o{ RESERVATION : receives
    STORE ||--o{ REVIEW : receives
    RESERVATION ||--o| REVIEW : "reviewed by"

    MEMBER {
        bigint id PK
        varchar email UK
        varchar password
        varchar name
        varchar phone_number
        varchar role
        datetime created_at
        datetime updated_at
    }

    STORE {
        bigint id PK
        bigint host_id FK
        varchar name
        varchar description
        varchar address
        double latitude
        double longitude
        int price_per_hour
        int capacity
        time open_time
        time close_time
        datetime created_at
        datetime updated_at
    }

    STORE_IMAGE {
        bigint id PK
        bigint store_id FK
        varchar image_url
        int sort_order
    }

    RESERVATION {
        bigint id PK
        bigint member_id FK
        bigint store_id FK
        int luggage_count
        datetime start_time
        datetime end_time
        varchar status
        int total_price
        datetime created_at
        datetime updated_at
    }

    REVIEW {
        bigint id PK
        bigint reservation_id FK "unique"
        bigint member_id FK
        bigint store_id FK
        int rating
        varchar content
        datetime created_at
        datetime updated_at
    }
```

## 관계 설명

- `MEMBER` 1 — N `STORE` : 한 회원(호스트)이 여러 보관소를 등록할 수 있음
- `MEMBER` 1 — N `RESERVATION` : 한 회원이 여러 예약을 만들 수 있음
- `STORE` 1 — N `RESERVATION` : 한 보관소는 여러 예약을 받을 수 있음
- `STORE` 1 — N `STORE_IMAGE` : 보관소 이미지는 여러 장 등록 가능
- `RESERVATION` 1 — 1 `REVIEW` : 예약 1건당 리뷰는 최대 1개
