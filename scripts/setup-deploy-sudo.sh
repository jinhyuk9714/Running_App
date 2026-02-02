#!/bin/bash
# NCP 서버에서 실행: 배포 사용자가 비밀번호 없이 systemctl restart running-app 할 수 있도록 설정
# 사용법: 서버에 SSH 접속 후  sudo bash setup-deploy-sudo.sh
# 또는: sudo bash setup-deploy-sudo.sh ubuntu  (사용자명 지정 시)

set -e

# 인자 없이 sudo로 실행 시: sudo를 호출한 사용자(예: ubuntu). 인자 있으면 해당 사용자.
USER="${1:-${SUDO_USER:-$(whoami)}}"
SUDOERS_FILE="/etc/sudoers.d/running-app-deploy"
RULE="${USER} ALL=(ALL) NOPASSWD: /bin/systemctl restart running-app"

if [ "$(id -u)" -ne 0 ]; then
  echo "root 권한이 필요합니다. sudo 로 실행하세요: sudo bash $0 [$USER]"
  exit 1
fi

echo "배포 사용자: $USER"
echo "규칙: $RULE"

echo "$RULE" > "$SUDOERS_FILE"
chmod 0440 "$SUDOERS_FILE"
visudo -c -f "$SUDOERS_FILE" || { rm -f "$SUDOERS_FILE"; exit 1; }

echo "설정 완료: $SUDOERS_FILE"
echo "확인: su - $USER -c 'sudo systemctl restart running-app'"
