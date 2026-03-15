package com.hola.config;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * TestContainers 설정 마커
 * application-test.yml의 jdbc:tc:postgresql URL이 자동으로 컨테이너를 관리함
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {
}
