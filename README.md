# 짐보관 플랫폼 (Luggage Storage Platform)

개인 포트폴리오 프로젝트. 위치 기반으로 짐 보관 공간을 검색·예약할 수 있는 웹 서비스.

## 스택

- **Backend**: Java 17, Spring Boot 4, Spring Data JPA, Spring Security, Gradle
- **Frontend**: React 19 + TypeScript, Vite
- **DB**: H2(개발) / MySQL(운영 예정)
- **API 문서**: springdoc-openapi (Swagger UI)

## 폴더 구조

```
project/
├── backend/    # Spring Boot API 서버
└── frontend/   # React 클라이언트
```

## 실행 방법

### Backend

```bash
cd backend
./gradlew.bat bootRun
```

- API 서버: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:luggage`)

### Frontend

```bash
cd frontend
npm install
npm run dev
```

- 개발 서버: http://localhost:5173 (`/api` 요청은 8080 백엔드로 프록시)

## VSCode

`luggage-storage.code-workspace` 파일을 열면 backend/frontend를 하나의 워크스페이스로 볼 수 있습니다.
