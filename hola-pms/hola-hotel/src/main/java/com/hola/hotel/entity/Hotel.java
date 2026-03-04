package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * 호텔 마스터 엔티티
 */
@Entity
@Table(name = "htl_hotel")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Hotel extends BaseEntity {

    @Column(name = "hotel_code", nullable = false, unique = true, length = 20)
    private String hotelCode;

    @Column(name = "hotel_name", nullable = false, length = 200)
    private String hotelName;

    @Column(name = "representative_name", length = 50)
    private String representativeName;

    @Column(name = "representative_name_en", length = 100)
    private String representativeNameEn;

    @Column(name = "country_code", length = 10)
    @Builder.Default
    private String countryCode = "+82";

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "address_detail", length = 500)
    private String addressDetail;

    @Column(name = "address_en", length = 500)
    private String addressEn;

    @Column(name = "address_detail_en", length = 500)
    private String addressDetailEn;

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "hotel", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Property> properties = new ArrayList<>();

    // 비즈니스 메서드
    public void update(String hotelName, String representativeName, String representativeNameEn,
                       String countryCode, String phone, String email,
                       String zipCode, String address, String addressDetail,
                       String addressEn, String addressDetailEn, String introduction) {
        this.hotelName = hotelName;
        this.representativeName = representativeName;
        this.representativeNameEn = representativeNameEn;
        this.countryCode = countryCode;
        this.phone = phone;
        this.email = email;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.addressEn = addressEn;
        this.addressDetailEn = addressDetailEn;
        this.introduction = introduction;
    }
}
