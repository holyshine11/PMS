package com.hola.common.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 메뉴 마스터 엔티티
 */
@Entity
@Table(name = "sys_menu",
        uniqueConstraints = @UniqueConstraint(columnNames = {"menu_code", "target_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_code", nullable = false, length = 50)
    private String menuCode;

    @Column(name = "menu_name", nullable = false, length = 100)
    private String menuName;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "depth", nullable = false)
    @Builder.Default
    private Integer depth = 1;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "use_yn", nullable = false)
    @Builder.Default
    private Boolean useYn = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
