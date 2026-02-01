package com.runningapp.util;

/**
 * 레벨 계산 유틸리티
 *
 * 누적 거리(km) 기반 레벨 산정:
 * - Lv1: 0km, Lv2: 10km, Lv3: 25km, Lv4: 50km, Lv5: 100km
 * - Lv6: 200km, Lv7: 400km, Lv8: 700km, Lv9: 1000km, Lv10: 1500km
 */
public class LevelCalculator {

    private static final double[] DISTANCE_THRESHOLDS = {0, 10, 25, 50, 100, 200, 400, 700, 1000, 1500};
    private static final int MAX_LEVEL = 10;

    /**
     * 누적 거리(km)에 따른 레벨 계산
     */
    public static int calculateLevel(double totalDistance) {
        int level = 1;
        for (int i = DISTANCE_THRESHOLDS.length - 1; i >= 0; i--) {
            if (totalDistance >= DISTANCE_THRESHOLDS[i]) {
                level = i + 1;
                break;
            }
        }
        return Math.min(level, MAX_LEVEL);
    }
}
