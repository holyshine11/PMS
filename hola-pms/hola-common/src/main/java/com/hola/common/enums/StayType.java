package com.hola.common.enums;

/**
 * 숙박유형 — OVERNIGHT(숙박), DAY_USE(데이유즈)
 */
public enum StayType {
    OVERNIGHT,
    DAY_USE;

    /**
     * Dayuse 여부
     */
    public boolean isDayUse() {
        return this == DAY_USE;
    }
}
