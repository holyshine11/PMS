package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * 마켓코드 엔티티
 */
@Entity
@Table(name = "htl_market_code", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"property_id", "market_code"})
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MarketCode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "market_code", nullable = false, length = 20)
    private String marketCode;

    @Column(name = "market_name", nullable = false, length = 200)
    private String marketName;

    @Column(name = "description_ko", columnDefinition = "TEXT")
    private String descriptionKo;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    public void update(String marketCode, String marketName, String descriptionKo, String descriptionEn) {
        this.marketCode = marketCode;
        this.marketName = marketName;
        this.descriptionKo = descriptionKo;
        this.descriptionEn = descriptionEn;
    }
}
