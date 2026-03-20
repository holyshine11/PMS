package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 하우스키핑 작업 이슈/메모 엔티티
 */
@Entity
@Table(name = "hk_task_issue")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HkTaskIssue extends BaseEntity {

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "room_number_id", nullable = false)
    private Long roomNumberId;

    @Column(name = "issue_type", nullable = false, length = 20)
    private String issueType;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "resolved")
    @Builder.Default
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;

    /**
     * 이슈 해결 처리
     */
    public void resolve(String resolvedBy) {
        this.resolved = true;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = resolvedBy;
    }
}
