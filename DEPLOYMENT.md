# 배포 가이드 (Supabase + Render + Vercel)

| 역할 | 서비스 | 무료 플랜 |
|---|---|---|
| DB (PostgreSQL) | Supabase | ✅ |
| 짐 사진 저장 | Supabase Storage (S3 호환) | ✅ |
| 백엔드 (Spring Boot) | Render (Docker) | ✅ (15분 쉬면 잠듦 → 접속하면 깨어남) |
| 프론트엔드 (React) | Vercel | ✅ |
| 문자·카카오 알림톡 | 솔라피 SOLAPI | 건당 과금 (가입 시 무료 포인트) |

> 환경 변수 전체 목록: 백엔드는 [`.env.example`](.env.example), 프론트는 [`frontend/.env.example`](frontend/.env.example)

---

## 1. Supabase — DB와 사진 저장소

1. https://supabase.com 에서 새 프로젝트를 만든다. **Region: Northeast Asia (Seoul)**, DB 비밀번호는 따로 적어 둔다.
2. **DB 접속 정보**: 상단 **Connect** → **Session pooler** (포트 5432)
   - `DB_URL` = `jdbc:postgresql://<host>:5432/postgres?sslmode=require`
   - `DB_USERNAME` = `postgres.<project-ref>`
   - `DB_PASSWORD` = 1번에서 정한 비밀번호
   - 테이블은 만들 필요 없다. 백엔드가 처음 켜질 때 Flyway가 V1~V7 마이그레이션으로 만들고, 임시 정식 지점 8곳도 넣는다.
3. **사진 버킷**: Storage → New bucket → 이름 `storefit`, **Public bucket 켜기**
4. **S3 키**: Project Settings → Storage → S3 Connection
   - `S3_ENDPOINT` = 화면의 Endpoint (`https://<ref>.supabase.co/storage/v1/s3`)
   - `S3_REGION` = 화면의 Region
   - New access key → `S3_ACCESS_KEY`, `S3_SECRET_KEY`
   - `S3_PUBLIC_URL` = `https://<ref>.supabase.co/storage/v1/object/public/storefit`

## 2. 솔라피 — 문자·카카오 알림톡

1. https://solapi.com 가입 → **발신번호 등록** (본인 휴대폰 인증) → `SOLAPI_SENDER`
2. 개발/연동 → API Key 생성 → `SOLAPI_API_KEY`, `SOLAPI_API_SECRET`
3. 여기까지만 해도 **모든 알림이 문자(SMS/LMS)로** 나간다. 휴대폰 인증번호도 문자로 간다.
4. (선택) 카카오 알림톡
   - 카카오톡 채널(비즈니스 채널)을 만들고 솔라피에 연동 → `SOLAPI_KAKAO_PF_ID`
   - 템플릿을 등록하고 카카오 검수를 받는다. 문구는 [`NotificationType.java`](backend/src/main/java/com/luggagestorage/notification/NotificationType.java)에 있는 그대로 쓰면 된다 (`#{place}` 같은 변수 형식도 같다).
   - 승인된 템플릿 ID를 `KAKAO_TEMPLATE_<종류>`에 넣는다 (예: `KAKAO_TEMPLATE_PAYMENT_DONE`). 넣은 종류만 알림톡으로 가고, 알림톡이 실패하면 같은 내용이 문자로 대신 간다.

## 3. Render — 백엔드

1. 이 저장소를 GitHub에 올린다.
2. Render → **New → Blueprint** → 저장소 선택. 루트의 [`render.yaml`](render.yaml)대로 `storefit-api` 서비스가 만들어진다.
3. `sync: false`인 값들을 입력한다. Supabase·솔라피 값 외에 아래도 필요하다.
   - `JWT_SECRET`: `openssl rand -base64 48` 결과
   - `TOSS_SECRET_KEY`: 결제 서비스 개발자센터의 시크릿 키 (테스트 키로 먼저 확인 추천)
   - `ADMIN_USERNAME` / `ADMIN_PASSWORD`: 본사 관리자 계정 (처음 켜질 때 만들어진다)
   - `CORS_ALLOWED_ORIGINS`: 4단계에서 받은 Vercel 주소
4. 배포가 끝나면 `https://storefit-api.onrender.com/api/health` 가 `{"status":"OK","database":"UP"}` 인지 확인한다.
   - 서비스 이름을 바꿨다면 주소도 바뀐다. 그 주소로 [`frontend/vercel.json`](frontend/vercel.json)과 [`.github/workflows/keep-awake.yml`](.github/workflows/keep-awake.yml)을 고친다.

## 4. Vercel — 프론트엔드

1. Vercel → Add New → Project → 같은 저장소 → **Root Directory: `frontend`**
2. Environment Variables
   - `VITE_KAKAO_JS_KEY` (선택): 카카오 지도. 카카오 개발자 콘솔 → 플랫폼 → Web에 Vercel 주소를 등록해야 지도가 뜬다.
   - `VITE_TOSS_CLIENT_KEY`: 결제 클라이언트 키 (백엔드 시크릿 키와 같은 쌍)
3. 배포 후 주소를 Render의 `CORS_ALLOWED_ORIGINS`에 넣는다.
4. 화면의 `/api/**` 요청은 `vercel.json`이 Render로 넘겨주므로, 브라우저 입장에서는 같은 주소라 쿠키·CORS 문제가 없다.

## 5. 배포 후 확인 순서

1. 관리자 계정으로 로그인 → **본사 관리**에서 정식 지점 주소·좌표를 실제 값으로 고친다.
2. 다른 휴대폰 번호로 **사장님** 가입 → 지점 운영 신청 → 관리자가 승인 → 승인 문자가 오는지 확인
3. 또 다른 번호로 **이용자** 가입 → 예약 → 결제 → 결제 완료 문자 + 체크인 QR 확인
4. 사장님 계정으로 **QR 체크인** → 체크인 문자 → 체크아웃
5. 관리자 **알림 기록**에서 모든 문자가 `발송`인지 확인 (실패면 이유가 같이 나온다)

## 자동으로 돌아가는 것

- **서버 깨우기**: Render 무료 서버는 15분 동안 요청이 없으면 잠들고, 다음 접속에 깨어나는 데 1분쯤 걸린다. 프론트엔드가 사이트를 열자마자 서버를 깨우기 시작하고, 그동안 "잠든 서버를 깨우는 중이에요" 안내를 띄운 뒤 깨어나면 요청을 이어서 보낸다. 무료 사용 시간(워크스페이스 전체 월 750시간)을 다른 서비스와 나눠 쓰기 위해 24시간 깨워 두지 않는다.
- **keep-awake** (GitHub Actions, 매일 오전 10시 5분): 서버와 Supabase를 하루 한 번 깨운다. Supabase가 오래 쓰이지 않아 일시정지되는 것을 막고, 깨어 있는 동안 그날의 알림이 나간다.
- **체험 계정**: 서버가 켜질 때 `demo_user`(체험 이용자)·`demo_owner`(체험 사장님, 홍대·신촌·강남역점 운영)를 만들고, 매일 처음 깨어날 때 체험 데이터를 처음 상태로 되돌린다. 비밀번호는 매번 아무도 모르는 값으로 바뀌어서 로그인 화면의 "둘러보기"로만 들어갈 수 있고, 체험 계정에는 문자를 보내지 않는다. 끄려면 Render에서 `DEMO_ENABLED=false`.
- **인증 문자 한도**: 번호당(1분 재전송·시간당 5회)에 더해 IP당 시간당 10회, 하루 전체 30회(`VERIFICATION_DAILY_LIMIT`)까지만 보낸다. 솔라피 충전금·일일 발송 한도를 넘지 않기 위해서다.
- **CI** (GitHub Actions, 푸시·PR마다): 백엔드 테스트(실제 Postgres 컨테이너) + 프론트 린트·빌드
- **서버 안의 스케줄러** (한국 시간)
  - 1분마다: 결제 마감(예약 후 30분)이 지난 미결제 예약 자동 취소
  - 10분마다(서버가 깨어 있을 때): 시작일에 체크인하지 않은 예약 노쇼 처리
  - 오전 10시~밤 9시에 깨어 있으면 하루 한 번: 내일 찾을 짐 알림, 기간이 지난 짐 연체 알림 (보낸 날은 `daily_job_runs`에 기록)

## 로컬 개발

```bash
docker compose up -d              # Postgres (localhost:5433)
cd backend && ./gradlew bootRun   # http://localhost:8080
cd frontend && npm run dev        # http://localhost:5173
```

- 문자는 실제로 보내지 않고 백엔드 로그와 **본사 관리 → 알림 기록**에 남는다. 휴대폰 인증번호는 가입 화면에 "개발 모드"로 보여준다.
- 로그인 화면의 **이용자로 둘러보기 / 사장님으로 둘러보기**로 체험 계정에 들어갈 수 있다 (배포에서도 같음, [`DemoService`](backend/src/main/java/com/luggagestorage/demo/DemoService.java))
- 로컬 전용 데모 관리자: `demo_admin` / `demo1234!` ([`DemoDataInitializer`](backend/src/main/java/com/luggagestorage/common/config/DemoDataInitializer.java))
- 테스트: `cd backend && ./gradlew test` (Docker가 켜져 있어야 한다)
