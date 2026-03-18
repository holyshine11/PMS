package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

/**
 * OOO/OOS 객실 관리 엔티티
 */
@Entity
@Table(name = "htl_room_unavailable")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RoomUnavailable extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "room_number_id", nullable = false)
    private Long roomNumberId;

    @Column(name = "unavailable_type", nullable = false, length = 10)
    private String unavailableType; // OOO, OOS

    @Column(name = "reason_code", length = 20)
    private String reasonCode;

    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "through_date", nullable = false)
    private LocalDate throughDate;

    @Column(name = "return_status", length = 20)
    @Builder.Default
    private String returnStatus = "DIRTY";

    public void update(String reasonCode, String reasonDetail,
                       LocalDate fromDate, LocalDate throughDate,
                       String returnStatus) {
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
        this.fromDate = fromDate;
        this.throughDate = throughDate;
        this.returnStatus = returnStatus;
    }
}
