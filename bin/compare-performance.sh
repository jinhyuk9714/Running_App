#!/bin/bash
#
# 성능 테스트 비교 스크립트
#
# baseline-result.json과 optimized-result.json 비교하여
# 개선율을 계산하고 마크다운 리포트 생성
#
# 사용법:
#   ./bin/compare-performance.sh
#

BASELINE="k6/baseline-result.json"
OPTIMIZED="k6/optimized-result.json"
REPORT="docs/PERFORMANCE-COMPARISON.md"

# 파일 존재 확인
if [ ! -f "$BASELINE" ]; then
    echo "❌ Baseline 결과 없음: $BASELINE"
    echo "   먼저 k6 run k6/load-test.js 실행"
    exit 1
fi

if [ ! -f "$OPTIMIZED" ]; then
    echo "❌ Optimized 결과 없음: $OPTIMIZED"
    echo "   먼저 k6 run k6/optimized-test.js 실행"
    exit 1
fi

# JSON 파싱
parse_ms() {
    echo "$1" | sed 's/ms//' | tr -d ' '
}

# Baseline 데이터
B_AVG=$(jq -r '.responseTime.avg' "$BASELINE" | sed 's/ms//')
B_P95=$(jq -r '.responseTime.p95' "$BASELINE" | sed 's/ms//')
B_THROUGHPUT=$(jq -r '.throughput' "$BASELINE" | sed 's/ req\/s//')
B_LOGIN=$(jq -r '.endpoints.login' "$BASELINE" | sed 's/ms//')
B_ACTIVITIES=$(jq -r '.endpoints.activities' "$BASELINE" | sed 's/ms//')
B_SUMMARY=$(jq -r '.endpoints.summary' "$BASELINE" | sed 's/ms//')
B_CHALLENGES=$(jq -r '.endpoints.challenges' "$BASELINE" | sed 's/ms//')
B_PLANS=$(jq -r '.endpoints.plans' "$BASELINE" | sed 's/ms//')
B_REQUESTS=$(jq -r '.totalRequests' "$BASELINE")

# Optimized 데이터
O_AVG=$(jq -r '.responseTime.avg' "$OPTIMIZED" | sed 's/ms//')
O_P95=$(jq -r '.responseTime.p95' "$OPTIMIZED" | sed 's/ms//')
O_THROUGHPUT=$(jq -r '.throughput' "$OPTIMIZED" | sed 's/ req\/s//')
O_LOGIN=$(jq -r '.endpoints.login' "$OPTIMIZED" | sed 's/ms//')
O_ACTIVITIES=$(jq -r '.endpoints.activities' "$OPTIMIZED" | sed 's/ms//')
O_SUMMARY=$(jq -r '.endpoints.summary' "$OPTIMIZED" | sed 's/ms//')
O_CHALLENGES=$(jq -r '.endpoints.challenges' "$OPTIMIZED" | sed 's/ms//')
O_PLANS=$(jq -r '.endpoints.plans' "$OPTIMIZED" | sed 's/ms//')
O_REQUESTS=$(jq -r '.totalRequests' "$OPTIMIZED")

# 개선율 계산 함수 (낮을수록 좋음)
calc_improvement() {
    local baseline=$1
    local optimized=$2
    if [ -z "$baseline" ] || [ -z "$optimized" ] || [ "$baseline" = "0" ]; then
        echo "N/A"
        return
    fi
    echo "scale=1; (($baseline - $optimized) / $baseline) * 100" | bc
}

# 처리량 개선율 (높을수록 좋음)
calc_throughput_improvement() {
    local baseline=$1
    local optimized=$2
    if [ -z "$baseline" ] || [ -z "$optimized" ] || [ "$baseline" = "0" ]; then
        echo "N/A"
        return
    fi
    echo "scale=1; (($optimized - $baseline) / $baseline) * 100" | bc
}

# 개선율 계산
IMP_AVG=$(calc_improvement "$B_AVG" "$O_AVG")
IMP_P95=$(calc_improvement "$B_P95" "$O_P95")
IMP_THROUGHPUT=$(calc_throughput_improvement "$B_THROUGHPUT" "$O_THROUGHPUT")
IMP_LOGIN=$(calc_improvement "$B_LOGIN" "$O_LOGIN")
IMP_ACTIVITIES=$(calc_improvement "$B_ACTIVITIES" "$O_ACTIVITIES")
IMP_SUMMARY=$(calc_improvement "$B_SUMMARY" "$O_SUMMARY")
IMP_CHALLENGES=$(calc_improvement "$B_CHALLENGES" "$O_CHALLENGES")
IMP_PLANS=$(calc_improvement "$B_PLANS" "$O_PLANS")

# 리포트 생성
mkdir -p docs

cat > "$REPORT" << EOF
# 🏃 Running App 성능 최적화 비교 리포트

> 생성일: $(date '+%Y-%m-%d %H:%M:%S')

## 📊 종합 요약

| 지표 | Baseline | Optimized | 개선율 |
|------|----------|-----------|--------|
| **평균 응답시간** | ${B_AVG}ms | ${O_AVG}ms | **${IMP_AVG}%** ⬇️ |
| **P95 응답시간** | ${B_P95}ms | ${O_P95}ms | **${IMP_P95}%** ⬇️ |
| **처리량 (TPS)** | ${B_THROUGHPUT} req/s | ${O_THROUGHPUT} req/s | **${IMP_THROUGHPUT}%** ⬆️ |
| **총 요청 수** | ${B_REQUESTS} | ${O_REQUESTS} | - |

## 🔍 엔드포인트별 성능

| API | Baseline | Optimized | 개선율 |
|-----|----------|-----------|--------|
| POST /api/auth/login | ${B_LOGIN}ms | ${O_LOGIN}ms | ${IMP_LOGIN}% |
| GET /api/activities | ${B_ACTIVITIES}ms | ${O_ACTIVITIES}ms | ${IMP_ACTIVITIES}% |
| GET /api/activities/summary | ${B_SUMMARY}ms | ${O_SUMMARY}ms | ${IMP_SUMMARY}% |
| GET /api/challenges | ${B_CHALLENGES}ms | ${O_CHALLENGES}ms | ${IMP_CHALLENGES}% |
| GET /api/plans | ${B_PLANS}ms | ${O_PLANS}ms | ${IMP_PLANS}% |

## 🛠️ 적용된 최적화

1. **Redis 캐싱** - 자주 조회되는 데이터 캐싱
2. **비동기 이벤트** - 활동 저장 후 레벨/챌린지 업데이트 비동기 처리
3. **N+1 쿼리 최적화** - Fetch Join, EntityGraph 적용
4. **데이터베이스 인덱스** - 자주 조회되는 컬럼 인덱스 추가
5. **Connection Pool 튜닝** - HikariCP 최적화
6. **Rate Limiting** - Bucket4j 기반 요청 제한
7. **구조화된 로깅** - 효율적인 로그 처리

## 📈 테스트 환경

- **부하 패턴**: 10 → 50 → 100 VUs (3분 30초)
- **테스트 도구**: K6
- **서버**: Spring Boot 3.3 + H2 (테스트)
- **측정일**:
  - Baseline: $(jq -r '.timestamp' "$BASELINE")
  - Optimized: $(jq -r '.timestamp' "$OPTIMIZED")

---

*이 리포트는 \`./bin/compare-performance.sh\` 스크립트로 자동 생성되었습니다.*
EOF

echo "✅ 리포트 생성 완료: $REPORT"
echo ""
echo "=========================================="
echo "         성능 개선 요약"
echo "=========================================="
echo "평균 응답시간: ${B_AVG}ms → ${O_AVG}ms (${IMP_AVG}% 개선)"
echo "P95 응답시간:  ${B_P95}ms → ${O_P95}ms (${IMP_P95}% 개선)"
echo "처리량:        ${B_THROUGHPUT} → ${O_THROUGHPUT} req/s (${IMP_THROUGHPUT}% 증가)"
echo "=========================================="
