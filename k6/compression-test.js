/**
 * API 응답 압축 효과 측정 스크립트
 *
 * 측정 항목:
 * - 압축/비압축 응답 크기 비교
 * - 네트워크 전송 시간 비교
 * - 압축률 계산
 *
 * 실행:
 *   k6 run k6/compression-test.js
 *
 * 또는 curl로 간단히 확인:
 *   # 압축 응답 (실제 전송 크기)
 *   curl -s -o /dev/null -w "%{size_download}" -H "Accept-Encoding: gzip" http://localhost:8080/api/challenges
 *
 *   # 비압축 응답 (원본 크기)
 *   curl -s -o /dev/null -w "%{size_download}" http://localhost:8080/api/challenges
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// 커스텀 메트릭
const compressedSize = new Trend('compressed_response_size', true);
const uncompressedSize = new Trend('uncompressed_response_size', true);
const compressionRatio = new Trend('compression_ratio');

export const options = {
    scenarios: {
        // 압축 테스트
        compression_test: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 10,
            maxDuration: '30s',
        },
    },
    thresholds: {
        'compression_ratio': ['avg>0.3'], // 평균 30% 이상 압축
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    // 1. 압축 요청 (Accept-Encoding: gzip)
    const compressedRes = http.get(`${BASE_URL}/api/challenges`, {
        headers: {
            'Accept-Encoding': 'gzip, deflate',
        },
        // K6는 자동으로 압축 해제하지만, 원본 크기 확인 가능
    });

    // 2. 비압축 요청 (Accept-Encoding 없음)
    const uncompressedRes = http.get(`${BASE_URL}/api/challenges`, {
        headers: {
            'Accept-Encoding': 'identity', // 압축 거부
        },
    });

    // 응답 크기 기록
    const compSize = parseInt(compressedRes.headers['Content-Length'] || compressedRes.body.length);
    const uncompSize = parseInt(uncompressedRes.headers['Content-Length'] || uncompressedRes.body.length);

    compressedSize.add(compSize);
    uncompressedSize.add(uncompSize);

    // 압축률 계산 (1 - 압축크기/원본크기)
    if (uncompSize > 0) {
        const ratio = 1 - (compSize / uncompSize);
        compressionRatio.add(ratio);
    }

    // 검증
    check(compressedRes, {
        'status is 200': (r) => r.status === 200,
        'content-type is json': (r) => r.headers['Content-Type']?.includes('application/json'),
    });

    check(uncompressedRes, {
        'uncompressed status is 200': (r) => r.status === 200,
    });

    sleep(0.5);
}

export function handleSummary(data) {
    const avgCompressed = data.metrics.compressed_response_size?.values?.avg || 0;
    const avgUncompressed = data.metrics.uncompressed_response_size?.values?.avg || 0;
    const avgRatio = data.metrics.compression_ratio?.values?.avg || 0;

    console.log('\n========================================');
    console.log('   API 응답 압축 측정 결과');
    console.log('========================================');
    console.log(`압축 응답 크기 (평균):   ${Math.round(avgCompressed)} bytes`);
    console.log(`비압축 응답 크기 (평균): ${Math.round(avgUncompressed)} bytes`);
    console.log(`압축률:                  ${(avgRatio * 100).toFixed(1)}%`);
    console.log(`절감량:                  ${Math.round(avgUncompressed - avgCompressed)} bytes`);
    console.log('========================================\n');

    return {
        'stdout': JSON.stringify(data, null, 2),
    };
}
