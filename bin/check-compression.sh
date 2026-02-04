#!/bin/bash
#
# API 응답 압축 검증 스크립트
#
# 사용법:
#   ./bin/check-compression.sh [BASE_URL]
#
# 예시:
#   ./bin/check-compression.sh                          # localhost:8080
#   ./bin/check-compression.sh https://example.com      # 프로덕션
#

BASE_URL="${1:-http://localhost:8080}"
ENDPOINT="/api/challenges"

echo "=========================================="
echo "   API 응답 압축 검증"
echo "=========================================="
echo "URL: ${BASE_URL}${ENDPOINT}"
echo ""

# 1. 압축 응답 크기 측정
echo "🔹 압축 응답 (Accept-Encoding: gzip):"
COMPRESSED=$(curl -s -o /dev/null -w "%{size_download}" \
    -H "Accept-Encoding: gzip, deflate" \
    "${BASE_URL}${ENDPOINT}")
echo "   크기: ${COMPRESSED} bytes"

# Content-Encoding 헤더 확인
ENCODING=$(curl -s -I -H "Accept-Encoding: gzip, deflate" "${BASE_URL}${ENDPOINT}" | grep -i "content-encoding" || echo "   (no compression header)")
echo "   ${ENCODING}"
echo ""

# 2. 비압축 응답 크기 측정
echo "🔹 비압축 응답 (Accept-Encoding: identity):"
UNCOMPRESSED=$(curl -s -o /dev/null -w "%{size_download}" \
    -H "Accept-Encoding: identity" \
    "${BASE_URL}${ENDPOINT}")
echo "   크기: ${UNCOMPRESSED} bytes"
echo ""

# 3. 압축률 계산
if [ "$UNCOMPRESSED" -gt 0 ] && [ "$COMPRESSED" -lt "$UNCOMPRESSED" ]; then
    SAVED=$((UNCOMPRESSED - COMPRESSED))
    RATIO=$(echo "scale=1; (1 - $COMPRESSED / $UNCOMPRESSED) * 100" | bc)
    echo "=========================================="
    echo "📊 결과"
    echo "=========================================="
    echo "   원본 크기:  ${UNCOMPRESSED} bytes"
    echo "   압축 크기:  ${COMPRESSED} bytes"
    echo "   절감량:     ${SAVED} bytes"
    echo "   압축률:     ${RATIO}%"
    echo "=========================================="
else
    echo "⚠️  압축이 적용되지 않았거나 응답이 너무 작습니다."
    echo "   (min-response-size 설정 확인 필요)"
fi
