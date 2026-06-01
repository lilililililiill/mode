package com.agrieconomy.crop;

/**
 * 작물 등급.
 * 각 등급은 가격 배율과 기본 추첨 확률을 가진다.
 * 실제 확률은 AgriConfig에서 서버 설정으로 덮어쓸 수 있다.
 */
public enum CropGrade {

    /** 작음: 기본 확률 20%, 가격 배율 0.8 */
    SMALL(0.8f),

    /** 보통: 기본 확률 60%, 가격 배율 1.0 */
    NORMAL(1.0f),

    /** 큼: 기본 확률 17%, 가격 배율 1.3 */
    LARGE(1.3f),

    /** 특급: 기본 확률 3%, 가격 배율 2.0 */
    PREMIUM(2.0f);

    /** NBT 키 상수 */
    public static final String NBT_KEY = "CropGrade";

    private final float priceMultiplier;

    CropGrade(float priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }

    public float getPriceMultiplier() {
        return priceMultiplier;
    }

    /**
     * 문자열로부터 안전하게 변환. 알 수 없는 값이면 NORMAL 반환.
     */
    public static CropGrade fromString(String value) {
        try {
            return CropGrade.valueOf(value);
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}