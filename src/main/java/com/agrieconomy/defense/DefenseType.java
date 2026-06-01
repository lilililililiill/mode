package com.agrieconomy.defense;

import com.agrieconomy.disaster.DisasterType;

/**
 * 방어 시설 종류.
 *
 * <ul>
 *   <li>PESTICIDE: 살충제 살포기 → PEST 방어</li>
 *   <li>IRRIGATION: 관개 장치 → DROUGHT 방어</li>
 *   <li>GREENHOUSE: 온실기 → COLD 방어</li>
 * </ul>
 */
public enum DefenseType {

    PESTICIDE(DisasterType.PEST),
    IRRIGATION(DisasterType.DROUGHT),
    GREENHOUSE(DisasterType.COLD);

    private final DisasterType counters;

    DefenseType(DisasterType counters) {
        this.counters = counters;
    }

    /** 이 시설이 해당 재해를 막을 수 있는지 반환한다. */
    public boolean counters(DisasterType disaster) {
        return this.counters == disaster;
    }

    public DisasterType getCounteredDisaster() { return counters; }

    public static DefenseType fromString(String value) {
        try {
            return DefenseType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 재해 타입에 대응하는 방어 시설을 반환한다. */
    public static DefenseType forDisaster(DisasterType disasterType) {
        for (DefenseType dt : values()) {
            if (dt.counters == disasterType) return dt;
        }
        return null;
    }
}
