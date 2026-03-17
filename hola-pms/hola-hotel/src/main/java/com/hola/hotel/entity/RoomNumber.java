package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 호수 엔티티
 */
@Entity
@Table(name = "htl_room_number", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"property_id", "room_number"})
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RoomNumber extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Column(name = "description_ko", columnDefinition = "TEXT")
    private String descriptionKo;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    // 객실 상태 (프론트데스크/하우스키핑)
    @Column(name = "hk_status", nullable = false, length = 20)
    @Builder.Default
    private String hkStatus = "CLEAN";  // CLEAN, DIRTY, OOO, OOS

    @Column(name = "fo_status", nullable = false, length = 20)
    @Builder.Default
    private String foStatus = "VACANT"; // VACANT, OCCUPIED

    @Column(name = "hk_updated_at")
    private LocalDateTime hkUpdatedAt;

    @Column(name = "hk_memo", length = 500)
    private String hkMemo;

    public void update(String roomNumber, String descriptionKo, String descriptionEn) {
        this.roomNumber = roomNumber;
        this.descriptionKo = descriptionKo;
        this.descriptionEn = descriptionEn;
    }

    /**
     * 체크인: FO 상태를 OCCUPIED로 변경
     */
    public void checkIn() {
        this.foStatus = "OCCUPIED";
    }

    /**
     * 체크아웃: FO=VACANT, HK=DIRTY
     */
    public void checkOut() {
        this.foStatus = "VACANT";
        this.hkStatus = "DIRTY";
        this.hkUpdatedAt = LocalDateTime.now();
    }

    /**
     * HK 상태 변경
     */
    public void updateHkStatus(String status, String memo) {
        this.hkStatus = status;
        this.hkMemo = memo;
        this.hkUpdatedAt = LocalDateTime.now();
    }
}
