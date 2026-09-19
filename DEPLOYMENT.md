# 배포 가이드 (Oracle Cloud Free Tier)

## 1. Oracle Cloud 계정 만들기
- https://www.oracle.com/cloud/free/ 에서 가입 (본인 확인용 카드 등록이 필요하지만 Always Free 리소스는 과금되지 않음)

## 2. Always Free VM 인스턴스 생성
- Compute → Instances → Create Instance
- Image: Ubuntu (최신 LTS)
- Shape: Always Free 표시된 것 중 선택
  - `VM.Standard.A1.Flex` (ARM, 최대 4 OCPU / 24GB) 추천 — 계정/리전에 따라 재고가 없을 수 있음
  - 안 되면 `VM.Standard.E2.1.Micro` (AMD, 1 OCPU / 1GB)로 대체
- SSH 키 생성/등록 (콘솔에서 자동 생성 가능, .pem 파일 잘 보관)

## 3. 방화벽(보안 목록) 열기
인스턴스가 속한 VCN의 Security List에서 인바운드 규칙 추가:
- TCP 22 (SSH) — 보통 기본으로 열려있음
- TCP 80 (HTTP)
- TCP 443 (HTTPS, 나중에 도메인+인증서 붙일 때)

## 4. 서버 접속 및 Docker 설치
```bash
ssh -i your-key.pem ubuntu@<서버 공개 IP>

sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker $USER
# 재접속 후 docker 명령이 sudo 없이 동작하는지 확인
```

## 5. 프로젝트 가져오기 & 환경변수 설정
```bash
git clone <이 저장소 URL>
cd <저장소 폴더>
cp .env.example .env
nano .env   # DB 비밀번호, JWT_SECRET, TOSS_SECRET_KEY를 실제 값으로 채우기
```

## 6. 실행
```bash
docker compose up -d --build
```
- 프론트엔드: `http://<서버 공개 IP>/`
- 첫 실행 시 Flyway가 자동으로 스키마를 생성합니다.

## 7. 업데이트할 때
```bash
git pull
docker compose up -d --build
```

## 참고
- 지금은 IP로만 접속합니다. 도메인을 연결하려면 DNS A 레코드를 서버 IP로 지정하고, `frontend/nginx.conf`에 `server_name`을 도메인으로 바꾼 뒤 Let's Encrypt(certbot)로 HTTPS 인증서를 추가하면 됩니다 (필요할 때 다시 요청해주세요).
