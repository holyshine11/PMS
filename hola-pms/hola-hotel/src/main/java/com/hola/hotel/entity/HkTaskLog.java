package com.hola.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 하우스키핑 작업 상태 변경 이력 엔티티
 * BaseEntity 미상속 (이력 테이블, soft delete 불필요)
 */
@Entity
@Table(name = "hk_task_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HkTaskLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Column(name = "changed_by", nullable = false, length = 50)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();

    @Column(name = "note", length = 500)
    private String note;
}
