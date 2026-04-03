package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * 워크스테이션 엔티티 (VAN 카드 단말기 연결 정보)
 */
@Entity
@Table(name = "htl_workstation", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"property_id", "ws_no"})
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Workstation extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "ws_no", nullable = false, length = 20)
    private String wsNo;

    @Column(name = "ws_name", length = 100)
    private String wsName;

    @Column(name = "kpsp_host", nullable = false, length = 255)
    @Builder.Default
    private String kpspHost = "localhost";

    @Column(name = "kpsp_port", nullable = false)
    @Builder.Default
    private Integer kpspPort = 19090;

    @Column(name = "terminal_id", length = 50)
    private String terminalId;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
