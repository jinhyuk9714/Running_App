# jinhyuk-portfolio1.shop HTTPS 설정 (Nginx + Let's Encrypt)

도메인 `jinhyuk-portfolio1.shop` 으로 **80/443** 접속 시 Spring Boot(8080)로 연결하고, **HTTPS** 인증서를 발급하는 순서입니다.

---

## 사전 확인

- [ ] DNS A 레코드: `jinhyuk-portfolio1.shop` → `49.50.131.57` (이미 설정됨)
- [ ] 서버 SSH 접속 가능: `ssh -i ncp-key.pem root@49.50.131.57`
- [ ] Spring Boot 실행 중: `http://jinhyuk-portfolio1.shop:8080/actuator/health` → `{"status":"UP"}`

---

## 1단계: NCP ACG에서 80, 443 포트 열기

1. **NCP 콘솔** → **Server** (VPC) → 해당 서버 선택
2. **ACG** (Access Control Group) 클릭 → **Inbound** 규칙 수정
3. 아래 두 규칙 **추가** (없으면):

| 프로토콜 | 접근 포트 | 접근 소스 |
| -------- | --------- | --------- |
| TCP      | 80        | 0.0.0.0/0 |
| TCP      | 443       | 0.0.0.0/0 |

4. 저장 후, 서버에서 80/443이 열렸는지 확인 (로컬에서):  
   `nc -zv jinhyuk-portfolio1.shop 80` (또는 브라우저로 `http://jinhyuk-portfolio1.shop` 접속 시 연결되면 OK)

---

## 2단계: 서버 SSH 접속

```bash
ssh -i ncp-key.pem root@49.50.131.57
```

이후 명령은 **모두 서버(root)에서** 실행합니다.

---

## 3단계: Nginx 설치

```bash
sudo apt update
sudo apt install -y nginx
```

설치 후 Nginx가 자동으로 실행됩니다. 확인:

```bash
sudo systemctl status nginx
```

`active (running)` 이면 OK.

---

## 4단계: Nginx 설정 파일 만들기

1. 설정 파일 생성:

```bash
sudo nano /etc/nginx/sites-available/running-app
```

2. **아래 내용 전체**를 붙여넣고 저장 (`Ctrl+O` → Enter → `Ctrl+X`):

```nginx
server {
    listen 80;
    server_name jinhyuk-portfolio1.shop www.jinhyuk-portfolio1.shop api.jinhyuk-portfolio1.shop;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

3. 기본 사이트 비활성화 (선택, 충돌 시):

```bash
sudo rm -f /etc/nginx/sites-enabled/default
```

4. 방금 만든 사이트 활성화:

```bash
sudo ln -sf /etc/nginx/sites-available/running-app /etc/nginx/sites-enabled/
```

5. 설정 문법 검사:

```bash
sudo nginx -t
```

`syntax is ok` / `test is successful` 나오면 OK.

6. Nginx 재시작:

```bash
sudo systemctl reload nginx
```

---

## 5단계: HTTP 접속 확인 (80 포트, 포트 번호 없이)

브라우저 또는 로컬 터미널에서:

- `http://jinhyuk-portfolio1.shop/actuator/health`

`{"status":"UP"}` 이 나오면 Nginx → 8080 프록시가 정상입니다.  
(아직 HTTPS 아님, 다음 단계에서 인증서 발급)

---

## 6단계: Certbot 설치 및 HTTPS 인증서 발급

1. Certbot 설치:

```bash
sudo apt install -y certbot python3-certbot-nginx
```

2. 인증서 발급 (도메인 3개: 루트 + www + api):

```bash
sudo certbot --nginx -d jinhyuk-portfolio1.shop -d www.jinhyuk-portfolio1.shop -d api.jinhyuk-portfolio1.shop
```

3. 안내에 따라:

   - **이메일** 입력 (갱신·보안 안내용)
   - **약관 동의** (Y)
   - **이메일 수신** 선택 (선택 사항, N 입력해도 됨)
   - **HTTP → HTTPS 리다이렉트** 물으면 **2 (Redirect)** 선택 권장

4. 성공 시 `Congratulations!` 메시지와 함께 HTTPS 적용 완료.  
   **api** 서브도메인은 Certbot이 Nginx에 자동 반영하지 못할 수 있음 → 아래 6.1 참고.

### 6.1 Certbot 후 443 블록이 없거나 HTTPS가 안 될 때

서버에서 `sudo ss -tlnp | grep 443` 실행 시 **:443** 이 없으면, Nginx 설정에 HTTPS 블록을 수동 추가합니다.

```bash
sudo nano /etc/nginx/sites-available/running-app
```

**기존 80 블록은 그대로 두고**, 아래 **HTTPS 블록 전체**를 파일에 추가합니다. (`listen [::]:443` 은 IPv6 미지원 서버에서는 넣지 않음.)

```nginx
# HTTP → HTTPS 리다이렉트
server {
    listen 80;
    server_name jinhyuk-portfolio1.shop www.jinhyuk-portfolio1.shop api.jinhyuk-portfolio1.shop;
    return 301 https://$host$request_uri;
}

# HTTPS (Let's Encrypt)
server {
    listen 443 ssl;
    server_name jinhyuk-portfolio1.shop www.jinhyuk-portfolio1.shop api.jinhyuk-portfolio1.shop;

    ssl_certificate /etc/letsencrypt/live/jinhyuk-portfolio1.shop/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/jinhyuk-portfolio1.shop/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

저장 후 `sudo nginx -t` → `sudo systemctl restart nginx` → `sudo ss -tlnp | grep 443` 로 443 리스닝 확인.

---

## 7단계: HTTPS 접속 확인

- `https://jinhyuk-portfolio1.shop/actuator/health` → `{"status":"UP"}`
- `https://api.jinhyuk-portfolio1.shop/actuator/health` → `{"status":"UP"}`
- `https://jinhyuk-portfolio1.shop/swagger-ui/index.html` → Swagger UI
- `http://...` 입력 시 → `https://...` 로 자동 이동 (리다이렉트 선택한 경우)

---

## 8단계: 인증서 자동 갱신 (선택)

Let's Encrypt 인증서는 **90일**마다 갱신해야 합니다. Certbot이 갱신용 cron을 자동 등록한 경우:

```bash
sudo systemctl status certbot.timer
```

`active` 이면 주기적으로 갱신됩니다. 수동 테스트:

```bash
sudo certbot renew --dry-run
```

에러 없이 끝나면 실제 갱신 시에도 문제없는 경우가 많습니다.

---

## 문제 해결

| 증상                                                 | 확인                                                                                                                                                                                                                                    |
| ---------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `http://jinhyuk-portfolio1.shop` 연결 안 됨          | ACG 80 포트 열림, `sudo nginx -t`, `sudo systemctl status nginx`                                                                                                                                                                        |
| **Nginx 시작 실패** (`Address family not supported`) | IPv6 비활성화 서버: `/etc/nginx/sites-available/default` 에서 `listen [::]:80` / `listen [::]:443` 앞에 `#` 추가                                                                                                                        |
| **443 포트 안 열림** (`ss` 에 :443 없음)             | Nginx 설정에 `listen [::]:443 ssl` 이 있으면 `# listen [::]:443 ssl` 로 주석 처리 후 `sudo systemctl restart nginx`                                                                                                                     |
| **Certbot 후 443 블록 없음**                         | 인증서는 발급됐으나 Nginx에 443 블록이 없을 수 있음. `docs/HTTPS_SETUP.md` 본문의 "443 블록 수동 추가" 예시대로 `/etc/nginx/sites-available/running-app` 에 HTTPS `server { listen 443 ssl; ... }` 블록 추가 (IPv6 listen 은 넣지 않음) |
| 502 Bad Gateway                                      | Spring Boot(8080) 실행 중인지: `sudo systemctl status running-app`                                                                                                                                                                      |
| Certbot 실패                                         | DNS 전파: `ping jinhyuk-portfolio1.shop` → 49.50.131.57                                                                                                                                                                                 |

---

## 요약

| 단계 | 할 일                                                                                                                              |
| ---- | ---------------------------------------------------------------------------------------------------------------------------------- |
| 1    | NCP ACG: 80, 443 Inbound 허용                                                                                                      |
| 2    | 서버 SSH 접속                                                                                                                      |
| 3    | `apt install nginx` (IPv6 오류 시 default에서 `listen [::]:80` 주석 처리)                                                          |
| 4    | `/etc/nginx/sites-available/running-app` 생성 (server_name에 루트·www·api 포함) → sites-enabled 링크 → `nginx -t` → `reload nginx` |
| 5    | `http://jinhyuk-portfolio1.shop/actuator/health` 로 확인                                                                           |
| 6    | `certbot --nginx -d jinhyuk-portfolio1.shop -d www.jinhyuk-portfolio1.shop -d api.jinhyuk-portfolio1.shop`                         |
| 7    | 443 블록이 없으면 수동 추가 (listen 443 ssl만 사용, `[::]:443` 제외) → `restart nginx`                                             |
| 8    | `https://jinhyuk-portfolio1.shop/actuator/health` 로 확인                                                                          |

완료 후 **포트 없이** `https://jinhyuk-portfolio1.shop` 또는 `https://api.jinhyuk-portfolio1.shop` 으로 API/Swagger 접속 가능합니다.
