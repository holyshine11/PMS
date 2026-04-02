package com.hola.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @PropertyAccess AOP Aspect
 * - @PropertyAccess가 선언된 컨트롤러 메서드 실행 전에
 *   propertyId 파라미터를 자동 추출하여 프로퍼티 접근 권한 검증
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PropertyAccessAspect {

    private final AccessControlService accessControlService;

    @Before("@annotation(propertyAccess)")
    public void validatePropertyAccess(JoinPoint joinPoint, PropertyAccess propertyAccess) {
        Long propertyId = extractPropertyId(joinPoint);
        if (propertyId != null) {
            accessControlService.validatePropertyAccess(propertyId);
        }
    }

    /**
     * 메서드 파라미터에서 @PathVariable Long propertyId 값 추출
     */
    private Long extractPropertyId(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            // @PathVariable이 붙은 Long 타입 파라미터 중 이름이 propertyId인 것
            if (args[i] instanceof Long) {
                // 파라미터 이름 기반 매칭
                String paramName = parameters[i].getName();
                PathVariable pathVariable = parameters[i].getAnnotation(PathVariable.class);

                if ("propertyId".equals(paramName)) {
                    return (Long) args[i];
                }
                // @PathVariable의 value/name으로도 매칭
                if (pathVariable != null) {
                    String pvName = pathVariable.value().isEmpty() ? pathVariable.name() : pathVariable.value();
                    if ("propertyId".equals(pvName)) {
                        return (Long) args[i];
                    }
                }
            }
        }

        // 파라미터 이름으로 찾기 (컴파일러 -parameters 옵션 또는 Spring 파라미터 이름 디스커버리)
        String[] paramNames = signature.getParameterNames();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if ("propertyId".equals(paramNames[i]) && args[i] instanceof Long) {
                    return (Long) args[i];
                }
            }
        }

        log.warn("@PropertyAccess 적용됨, propertyId 파라미터를 찾을 수 없음: {}", signature.toShortString());
        return null;
    }
}
