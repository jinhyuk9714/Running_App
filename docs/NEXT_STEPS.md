# 다음 단계 (자동 배포 설정)

`main` 푸시 시 NCP 서버로 자동 배포되도록 하려면 아래를 **순서대로** 진행하세요.

---

## 1단계: GitHub Secrets 등록

1. GitHub 저장소 **Running_App** → **Settings** → **Secrets and variables** → **Actions**
2. **New repository secret** 클릭 후 아래 3개 시크릿을 **각각** 추가합니다.

| Name | Value | 비고 |
|------|--------|------|
| `DEPLOY_HOST` | 서버 공인 IP (예: `49.50.131.57`) | NCP 서버 IP |
| `DEPLOY_USER` | SSH 로그인 사용자 (예: `ubuntu`) | |
| `DEPLOY_SSH_KEY` | `.pem` 파일 **전체 내용** | `-----BEGIN ... END ...` 포함해서 복사 |

- `DEPLOY_SSH_KEY`: 서버 접속용 **비공개 키** 전체를 붙여넣기 (줄바꿈 유지)

---

## 2단계: 서버에서 sudo 설정 (비밀번호 없이 restart 허용)

NCP 서버에 SSH로 접속한 뒤 아래를 실행합니다.

```bash
# 1. SSH 접속 (로컬에서)
ssh -i your-key.pem ubuntu@<서버공인IP>

# 2. sudoers 편집 (서버에서)
sudo visudo
```

**visudo**가 열리면 **파일 맨 아래**에 다음 한 줄을 추가하고 저장합니다.

```
ubuntu ALL=(ALL) NOPASSWD: /bin/systemctl restart running-app
```

- `ubuntu` 대신 실제 배포에 쓰는 사용자명을 넣으면 됩니다.
- 저장: `Ctrl+O` → Enter → `Ctrl+X` (nano 기준)

**확인 (선택):** 서버에서 아래를 실행해 비밀번호 없이 동작하는지 봅니다.

```bash
sudo systemctl restart running-app
sudo systemctl status running-app
```

---

## 3단계: 자동 배포 확인

1. GitHub 저장소 → **Actions** 탭
2. 방금 푸시한 워크플로우 실행이 보이면 **Deploy to NCP** job이 **성공**했는지 확인
3. 실패 시 해당 job 로그에서 `DEPLOY_HOST`, `DEPLOY_SSH_KEY` 등이 올바른지 확인

---

## 완료 후

- 이후에는 **`main` 브랜치에 push**만 하면 CI가 JAR을 빌드하고 서버에 복사한 뒤 `systemctl restart running-app`을 실행합니다.
- Health 확인: `http://<서버IP>:8080/actuator/health`
