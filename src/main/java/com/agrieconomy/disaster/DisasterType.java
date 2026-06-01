package com.agrieconomy.disaster;

/**
 * 재해 종류.
 *
 * <ul>
 *   <li>DROUGHT: 가뭄 — SMALL 증가, LARGE 감소</li>
 *   <li>PEST: 병충해 — SMALL 증가, PREMIUM 감소</li>
 *   <li>COLD: 냉해 — 성장 자체 억제 (추가 로직)</li>
 *   <li>BLESSING: 풍년 — LARGE·PREMIUM 증가</li>
 * </ul>
 */
public enum DisasterType {
    DROUGHT,
    PEST,
    COLD,
    BLESSING;

    public static DisasterType fromString(String value) {
        try {
            return DisasterType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}