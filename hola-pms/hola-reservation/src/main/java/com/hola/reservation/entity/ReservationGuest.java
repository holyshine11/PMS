package com.hola.reservation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 동반 투숙객 엔티티 (경량 - BaseEntity 미상속)
 */
@Entity
@Table(name = "rsv_reservation_guest", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"sub_reservation_id", "guest_seq"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReservationGuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_reservation_id", nullable = false)
    private SubReservation subReservation;

    @Column(name = "guest_seq", nullable = false)
    private Integer guestSeq;

    @Column(name = "guest_name_ko", length = 100)
    private String guestNameKo;

    @Column(name = "guest_first_name_en", length = 100)
    private String guestFirstNameEn;

    @Column(name = "guest_middle_name_en", length = 100)
    private String guestMiddleNameEn;

    @Column(name = "guest_last_name_en", length = 100)
    private String guestLastNameEn;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
