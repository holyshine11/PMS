package com.hola.common.security;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Thymeleaf 폼 로그인용 UserDetailsService
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        AdminUser adminUser = adminUserRepository.findByLoginIdAndDeletedAtIsNull(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + loginId));

        return new User(
                adminUser.getLoginId(),
                adminUser.getPassword(),
                !adminUser.getAccountLocked(), // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                !adminUser.getAccountLocked(), // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + adminUser.getRole()))
        );
    }
}
