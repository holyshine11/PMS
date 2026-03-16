package com.hola.rate.repository;

import com.hola.rate.entity.RateCodePaidService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 레이트 코드 - 유료 서비스 옵션 매핑 Repository
 */
public interface RateCodePaidServiceRepository extends JpaRepository<RateCodePaidService, Long> {

    List<RateCodePaidService> findAllByRateCodeId(Long rateCodeId);

    List<RateCodePaidService> findAllByRateCodeIdIn(List<Long> rateCodeIds);

    @Modifying
    @Query("DELETE FROM RateCodePaidService r WHERE r.rateCodeId = :rateCodeId")
    void deleteAllByRateCodeId(@Param("rateCodeId") Long rateCodeId);
}
