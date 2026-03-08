package com.hola.rate.repository;

import com.hola.rate.entity.RateCodeRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 레이트 코드 - 객실 타입 매핑 Repository
 */
public interface RateCodeRoomTypeRepository extends JpaRepository<RateCodeRoomType, Long> {

    List<RateCodeRoomType> findAllByRateCodeId(Long rateCodeId);

    long countByRateCodeId(Long rateCodeId);

    @Modifying
    @Query("DELETE FROM RateCodeRoomType r WHERE r.rateCodeId = :rateCodeId")
    void deleteAllByRateCodeId(@Param("rateCodeId") Long rateCodeId);
}
