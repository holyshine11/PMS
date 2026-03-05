package com.hola.common.auth.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * 관리자 사용자 엔티티
 */
@Entity
@Table(name = "sys_admin_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AdminUser extends BaseEntity {

    /** 로그인 실패 잠금 임계값 */
    public static final int MAX_LOGIN_FAIL_COUNT = 5;

    /** 역할 상수 */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_HOTEL_ADMIN = "HOTEL_ADMIN";
    public static final String ROLE_PROPERTY_ADMIN = "PROPERTY_ADMIN";

    /** SUPER_ADMIN 여부 */
    public boolean isSuperAdmin() {
        return ROLE_SUPER_ADMIN.equals(this.role);
    }

    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "login_fail_count")
    @Builder.Default
    private Integer loginFailCount = 0;

    @Column(name = "account_locked")
    @Builder.Default
    private Boolean accountLocked = false;

    // === 호텔 관리자 확장 필드 ===

    @Column(name = "member_number", unique = true, length = 20)
    private String memberNumber;

    @Column(name = "account_type", length = 20)
    @Builder.Default
    private String accountType = "BLUEWAVE_ADMIN";

    @Column(name = "hotel_id")
    private Long hotelId;

    @Column(name = "mobile_country_code", length = 10)
    private String mobileCountryCode;

    @Column(name = "mobile", length = 20)
    private String mobile;

    @Column(name = "phone_country_code", length = 10)
    private String phoneCountryCode;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "position", length = 100)
    private String position;

    @Column(name = "role_name", length = 100)
    private String roleName;

    @Column(name = "role_id")
    private Long roleId;

    public void incrementLoginFailCount() {
        this.loginFailCount++;
        if (this.loginFailCount >= MAX_LOGIN_FAIL_COUNT) {
            this.accountLocked = true;
        }
    }

    public void resetLoginFailCount() {
        this.loginFailCount = 0;
        this.accountLocked = false;
    }

    /**
     * 호텔 관리자 프로필 수정
     */
    public void updateProfile(String userName, String email, String phone,
                              String mobileCountryCode, String mobile,
                              String phoneCountryCode,
                              String department, String position,
                              String roleName, Long roleId, Boolean useYn) {
        this.userName = userName;
        this.email = email;
        this.phone = phone;
        this.mobileCountryCode = mobileCountryCode;
        this.mobile = mobile;
        this.phoneCountryCode = phoneCountryCode;
        this.department = department;
        this.position = position;
        this.roleName = roleName;
        this.roleId = roleId;
        if (useYn != null) {
            if (useYn) activate();
            else deactivate();
        }
    }

    /**
     * 비밀번호 초기화
     */
    public void resetPassword(String encodedPassword) {
        this.password = encodedPassword;
        resetLoginFailCount();
    }
}
