package com.hola.common.tenant;

/**
 * ThreadLocal 기반 테넌트 컨텍스트
 * 요청별 테넌트 스키마를 관리
 */
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static final String DEFAULT_SCHEMA = "public";

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        String tenantId = CURRENT_TENANT.get();
        return tenantId != null ? tenantId : DEFAULT_SCHEMA;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
