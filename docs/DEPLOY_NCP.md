# NCP(네이버 클라우드 플랫폼) 배포 가이드

Running App을 NCP Server(VPC) + Cloud DB for PostgreSQL로 배포하는 방법입니다.

---

## 1. NCP 개요

| 구분          | 내용                                                                                  |
| ------------- | ------------------------------------------------------------------------------------- |
| **제공**      | 네이버 클라우드                                                                       |
| **특징**      | 한글 콘솔, 한국 리전, 국내 기업 다수 사용                                             |
| **무료 체험** | 신규 가입 시 크레딧 제공 (금액·기간은 [ncloud.com](https://www.ncloud.com) 정책 확인) |

---

## 2. 사전 준비

- NCP 회원가입 및 결제 수단 등록 (무료 체험 시에도 필요할 수 있음)
- SSH 클라이언트 (터미널 또는 PuTTY)
- 프로젝트 JAR 빌드: `./gradlew bootJar -x test`

---

## 3. VPC 환경 준비

NCP에서 **Server(VPC)** 와 **Cloud DB for PostgreSQL** 은 **VPC** 안에서만 사용합니다.

1. **콘솔** → **Services** → **Compute** → **Server** (VPC)
2. **VPC** 가 없으면 먼저 생성
   - **VPC** → **VPC 생성** → 이름·IPv4 CIDR 지정 (예: `10.0.0.0/16`)
3. **Subnet** 생성 (예: `10.0.1.0/24`, 용도: Private 또는 Public)

---

## 4. Server(VPC) 생성

### 4.1 서버 생성

1. **Server** → **Server** → **Server 생성**
2. **리전/존**: 원하는 리전 선택 (예: 한국)
3. **이미지**: **Ubuntu Server 22.04** 또는 **CentOS** 등
4. **스펙**: micro 또는 최소 스펙 (비용 고려)
5. **스토리지**: 기본 디스크 크기
6. **네트워크**: 위에서 만든 VPC·Subnet 선택
7. **ACG(Access Control Group)**: 새로 만들거나 기존 선택 (아래 4.2에서 규칙 추가)
8. **키 페어**: 생성 후 `.pem` 파일 다운로드 (SSH 접속용)

### 4.2 ACG 규칙 (Server용)

Server에 적용할 ACG에 **Inbound** 규칙 추가:

| 프로토콜 | 접근 포트 | 접근 소스                  | 용도    |
| -------- | --------- | -------------------------- | ------- |
| TCP      | 22        | 내 IP 또는 관리용 IP       | SSH     |
| TCP      | 8080      | 0.0.0.0/0 (또는 제한된 IP) | 앱 접속 |

- **접근 소스**: `0.0.0.0/0` 은 모든 IP 허용 (테스트용). 실제 서비스 시에는 웹/관리자 IP만 허용 권장.

---

## 5. Cloud DB for PostgreSQL 생성

### 5.1 DB Server 생성

1. **콘솔** → **Services** → **Database** → **Cloud DB for PostgreSQL**
2. **DB Server 생성** 클릭
3. **리전/VPC/Subnet**: Server와 **같은 VPC** (또는 통신 가능한 Subnet) 선택
4. **사양**: 최소 사양 선택 (비용 고려)
5. **스토리지**: 기본 10GB 등
6. **마스터 사용자명/비밀번호**: 기억해두기 (예: `running` / `비밀번호`)
7. **DB 이름**: `runningdb` (초기 DB로 생성 가능하면 지정)
8. **ACG**: DB용 ACG 생성 후 **PostgreSQL(5432)** Inbound 허용
   - **접근 소스**: Server가 속한 ACG 또는 Server의 Private IP 대역 (예: `10.0.1.0/24`)

### 5.2 DB 접속 정보 확인

- 생성 완료 후 **접속 정보**에서 **엔드포인트(호스트)** 와 **포트(5432)** 확인
- 예: `runningdb-xxxxx.db.ntruss.com` 또는 Private IP

---

## 6. Server ACG에서 DB 접근 허용

- DB Server ACG **Inbound**: PostgreSQL(5432) 허용 시 **접근 소스**를 **Application Server ACG** 또는 **Application Server의 Private IP**로 두면, Server(VPC)만 DB에 접속 가능하게 할 수 있습니다.

---

## 7. Server에 앱 배포

### 7.1 SSH 접속

```bash
chmod 400 your-key.pem
ssh -i your-key.pem ubuntu@<Server공인IP>
# 이미지가 Ubuntu가 아니면 사용자명이 다를 수 있음 (예: root, centos)
```

### 7.2 JDK 17 설치 (Ubuntu 기준)

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk-headless
java -version
```

### 7.3 JAR 업로드 및 실행

**방법 A: 로컬에서 SCP로 업로드**

```bash
# 로컬에서
./gradlew bootJar -x test
scp -i your-key.pem build/libs/running-app-0.0.1-SNAPSHOT.jar ubuntu@<Server공인IP>:~/app.jar
```

**방법 B: Server에서 Git 클론 후 빌드**

```bash
# Server에서
sudo apt install -y git
git clone https://github.com/jinhyuk9714/Running_App.git
cd Running_App
./gradlew bootJar -x test --no-daemon
# build/libs/running-app-0.0.1-SNAPSHOT.jar 생성
```

### 7.4 환경 변수 설정 및 실행

**prod** 프로파일과 DB 접속 정보를 환경 변수로 넣고 실행합니다.

```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:postgresql://<DB엔드포인트>:5432/runningdb
export SPRING_DATASOURCE_USERNAME=running
export SPRING_DATASOURCE_PASSWORD=<DB비밀번호>
export JWT_SECRET=<32자_이상_시크릿키>

java -jar app.jar
# 또는: java -jar build/libs/running-app-0.0.1-SNAPSHOT.jar
```

- **DB엔드포인트**: Cloud DB for PostgreSQL 접속 정보의 호스트명 또는 Private IP
- **runningdb**: 5.1에서 초기 DB를 만들지 않았다면, DB 생성 후 접속해서 `CREATE DATABASE runningdb;` 실행

### 7.5 백그라운드 실행 (nohup) 또는 systemd

**nohup 예시**

```bash
nohup java -jar app.jar > app.log 2>&1 &
```

**systemd 서비스 예시** (`/etc/systemd/system/running-app.service`)

```ini
[Unit]
Description=Running App
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="SPRING_DATASOURCE_URL=jdbc:postgresql://<DB엔드포인트>:5432/runningdb"
Environment="SPRING_DATASOURCE_USERNAME=running"
Environment="SPRING_DATASOURCE_PASSWORD=<비밀번호>"
Environment="JWT_SECRET=<시크릿>"
ExecStart=/usr/bin/java -jar /home/ubuntu/app.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable running-app
sudo systemctl start running-app
sudo systemctl status running-app
```

---

## 8. 접속 확인

- **Swagger UI**: `http://<Server공인IP>:8080/swagger-ui/index.html`  
  (`/swagger-ui.html` 은 파일 다운로드될 수 있으므로 `/swagger-ui/index.html` 사용)
- **API**: `http://<Server공인IP>:8080/api/...`

---

## 9. prod 프로파일 설정

앱이 **prod** 프로파일로 DB와 JWT를 환경 변수에서 읽으려면 `application-prod.yml` 이 필요합니다.  
(이미 있다면 아래와 같은 내용인지 확인)

```yaml
# application-prod.yml 예시
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.PostgreSQLDialect
jwt:
  secret: ${JWT_SECRET}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}
```

---

## 10. 보안 체크리스트

- [ ] DB ACG: 5432 포트는 **Application Server(또는 해당 Subnet)만** 허용
- [ ] Server ACG: 8080은 필요 시 특정 IP만 허용
- [ ] JWT_SECRET: 32자 이상 강한 랜덤 문자열 사용
- [ ] DB 비밀번호: 강한 비밀번호 사용
- [ ] 무료 체험 한도: 사용량·크레딧 주기적 확인

---

## 11. 참고 링크

- [NCP 콘솔](https://console.ncloud.com)
- [Server(VPC) 가이드](https://guide.ncloud-docs.com/docs/server-vpc-overview)
- [Cloud DB for PostgreSQL 가이드](https://guide.ncloud-docs.com/docs/clouddbforpostgresql-overview)
- [ACG 가이드](https://guide.ncloud-docs.com/docs/server-acg-vpc)
