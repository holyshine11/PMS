package com.hola.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 프로퍼티 접근 권한 검증 AOP 어노테이션
 * - 메서드 파라미터 중 @PathVariable Long propertyId를 자동 추출하여 검증
 * - accessControlService.validatePropertyAccess(propertyId) 호출 대체
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyAccess {
}
