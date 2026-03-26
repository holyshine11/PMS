package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * 룸타입별 청소 정책 오버라이드
 * null 필드 = 프로퍼티 HkConfig 기본값 사용
 */
@Entity
@Table(name = "hk_cleaning_policy",
       uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "room_type_id"}))
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HkCleaningPolicy extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "stayover_enabled")
    private Boolean stayoverEnabled;

    @Column(name = "stayover_frequency")
    private Integer stayoverFrequency;

    @Column(name = "turndown_enabled")
    private Boolean turndownEnabled;

    @Column(name = "stayover_credit", precision = 3, scale = 1)
    private BigDecimal stayoverCredit;

    @Column(name = "turndown_credit", precision = 3, scale = 1)
    private BigDecimal turndownCredit;

    @Column(name = "stayover_priority", length = 10)
    private String stayoverPriority;

    @Column(name = "dnd_policy", length = 30)
    private String dndPolicy;

    @Column(name = "dnd_max_skip_days")
    private Integer dndMaxSkipDays;

    @Column(name = "note", length = 500)
    private String note;

    /**
     * 오버라이드 설정 수정
     */
    public void update(Boolean stayoverEnabled, Integer stayoverFrequency,
                       Boolean turndownEnabled, BigDecimal stayoverCredit,
                       BigDecimal turndownCredit, String stayoverPriority,
                       String dndPolicy, Integer dndMaxSkipDays, String note) {
        this.stayoverEnabled = stayoverEnabled;
        this.stayoverFrequency = stayoverFrequency;
        this.turndownEnabled = turndownEnabled;
        this.stayoverCredit = stayoverCredit;
        this.turndownCredit = turndownCredit;
        this.stayoverPriority = stayoverPriority;
        this.dndPolicy = dndPolicy;
        this.dndMaxSkipDays = dndMaxSkipDays;
        this.note = note;
    }
}
