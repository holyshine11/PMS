package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * 예약채널 엔티티
 */
@Entity
@Table(name = "htl_reservation_channel", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"property_id", "channel_code"})
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReservationChannel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "channel_code", nullable = false, length = 20)
    private String channelCode;

    @Column(name = "channel_name", nullable = false, length = 200)
    private String channelName;

    @Column(name = "channel_type", nullable = false, length = 20)
    private String channelType;

    @Column(name = "description_ko", columnDefinition = "TEXT")
    private String descriptionKo;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    public void update(String channelName, String channelType, String descriptionKo, String descriptionEn) {
        this.channelName = channelName;
        this.channelType = channelType;
        this.descriptionKo = descriptionKo;
        this.descriptionEn = descriptionEn;
    }
}
