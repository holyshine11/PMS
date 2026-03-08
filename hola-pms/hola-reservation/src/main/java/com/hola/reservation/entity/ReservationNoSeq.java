package com.hola.reservation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 예약번호 시퀀스 엔티티
 */
@Entity
@Table(name = "rsv_reservation_no_seq", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"property_id", "seq_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReservationNoSeq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "seq_date", nullable = false)
    private LocalDate seqDate;

    @Column(name = "last_seq", nullable = false)
    @Builder.Default
    private Integer lastSeq = 0;

    /**
     * 시퀀스 증가
     */
    public int incrementAndGet() {
        this.lastSeq++;
        return this.lastSeq;
    }
}
