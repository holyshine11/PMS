package com.hola.support;

import com.hola.config.TestContainersConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 테스트 베이스 클래스
 * - TestContainers PostgreSQL 16
 * - Flyway 마이그레이션 적용
 * - MockMvc + @Transactional (테스트 후 롤백)
 * - 기본 인증: SUPER_ADMIN
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@Transactional
@WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
}
