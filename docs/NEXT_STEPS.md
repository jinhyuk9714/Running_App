# 다음 단계 (자동 배포 설정)

`main` 푸시 시 NCP 서버로 자동 배포되도록 하려면 아래를 **순서대로** 진행하세요.

---

## 1단계: GitHub Secrets 등록

1. GitHub 저장소 **Running_App** → **Settings** → **Secrets and variables** → **Actions**
2. **New repository secret** 클릭 후 아래 3개 시크릿을 **각각** 추가합니다.

| Name             | Value                             | 비고                                   |
| ---------------- | --------------------------------- | -------------------------------------- |
| `DEPLOY_HOST`    | 서버 공인 IP (예: `49.50.131.57`) | NCP 서버 IP                            |
| `DEPLOY_USER`    | SSH 로그인 사용자 (예: `ubuntu`)  |                                        |
| `DEPLOY_SSH_KEY` | `.pem` 파일 **전체 내용**         | `-----BEGIN ... END ...` 포함해서 복사 |

**`DEPLOY_SSH_KEY` 넣는 방법**

1. NCP 서버 접속할 때 쓰는 **비공개 키 파일**을 찾습니다. (예: `your-key.pem`, `running-app.pem`)
2. **텍스트 에디터**로 그 파일을 엽니다. (VS Code, 메모장, Cursor 등)
3. **처음부터 끝까지 전부** 선택해서 복사합니다.
   - 반드시 `-----BEGIN ... PRIVATE KEY-----` 로 **시작**하고
   - `-----END ... PRIVATE KEY-----` 로 **끝나야** 합니다.
   - 그 사이 줄바꿈도 그대로 두고 복사합니다.
4. GitHub → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**
5. **Name**: `DEPLOY_SSH_KEY`  
   **Secret**: 방금 복사한 내용 **전체**를 붙여넣기 → **Add secret**

- 터미널에서 복사하려면:
  - macOS: `cat your-key.pem | pbcopy`
  - Windows: `type your-key.pem | clip`
  - Linux: `xclip -sel clip < your-key.pem` 또는 `cat your-key.pem` 후 출력 전체 복사

---

## 2단계: 서버에서 sudo 설정 (비밀번호 없이 restart 허용)

NCP 서버에 SSH로 접속한 뒤 **아래 둘 중 하나**를 선택해 실행합니다.

### 방법 A: 서버에서 한 번에 설정 (저장소 없이)

서버에 SSH 접속한 뒤 **아래 명령만 순서대로** 실행합니다. (`ubuntu` 는 배포에 쓰는 사용자명으로 바꾸세요.)

```bash
# 배포 사용자명 (GitHub DEPLOY_USER 와 동일하게)
DEPLOY_USER=root

echo "${DEPLOY_USER} ALL=(ALL) NOPASSWD: /bin/systemctl restart running-app" | sudo tee /etc/sudoers.d/running-app-deploy
sudo chmod 440 /etc/sudoers.d/running-app-deploy
sudo visudo -c -f /etc/sudoers.d/running-app-deploy
```

- 성공하면 아무 메시지 없이 끝납니다. `visudo -c` 가 문법 오류 있으면 에러를 냅니다.

### 방법 A-2: 저장소 클론 후 스크립트 실행

서버에 이 저장소를 클론해 둔 경우에만 사용:

```bash
# 1. SSH 접속 (로컬에서)
ssh -i your-key.pem ubuntu@<서버공인IP>

# 2. 서버에서 (클론한 경우)
cd Running_App && sudo bash scripts/setup-deploy-sudo.sh

# 또는 사용자명 지정
sudo bash scripts/setup-deploy-sudo.sh ubuntu
```

### 방법 B: 수동으로 visudo 편집

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

---

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

## Permission denied (publickey) 나올 때

`scp` / `ssh` 단계에서 **Permission denied (publickey)** 가 나오면 아래를 확인하세요.

| 확인                     | 내용                                                                                                                                                                                                  |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **DEPLOY_USER**          | 서버에 **그 키로 로그인 가능한 사용자**와 같아야 합니다. NCP Ubuntu는 보통 **ubuntu** 로 SSH 접속하고, root SSH는 비활성인 경우가 많습니다.                                                           |
| **DEPLOY_SSH_KEY**       | `.pem` **비공개 키 전체**가 들어가 있어야 합니다. 공개 키(.pub)가 아닌 **비공개 키**이고, `-----BEGIN ...` ~ `-----END ...` 까지 줄바꿈 포함해서 복사했는지 확인하세요.                               |
| **서버 authorized_keys** | 해당 사용자(예: ubuntu)의 `~/.ssh/authorized_keys` 에 **이 .pem 키의 공개 키**가 등록되어 있어야 합니다. 로컬에서 `ssh-keygen -y -f your-key.pem` 으로 공개 키를 뽑아서 서버에 붙여 넣을 수 있습니다. |

- **root** 로 배포하려면: 서버에서 root SSH 로그인이 허용되어 있고, root의 `~/.ssh/authorized_keys` 에 이 키의 공개 키가 있어야 합니다.
- **ubuntu** 로 배포하려면: GitHub Secrets 의 **DEPLOY_USER** 를 `ubuntu` 로 두고, 서버의 `/home/ubuntu/.ssh/authorized_keys` 에 이 .pem 의 공개 키가 들어가 있어야 합니다.

---

## 완료 후

- 이후에는 **`main` 브랜치에 push**만 하면 CI가 JAR을 빌드하고 서버에 복사한 뒤 `systemctl restart running-app`을 실행합니다.
- Health 확인: `https://<도메인>/actuator/health` 또는 `http://<서버IP>:8080/actuator/health`
