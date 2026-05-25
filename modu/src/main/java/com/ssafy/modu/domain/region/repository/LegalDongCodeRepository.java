package com.ssafy.modu.domain.region.repository;

import com.ssafy.modu.domain.region.entity.LegalDongCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LegalDongCodeRepository extends JpaRepository<LegalDongCode, Long> {

    List<LegalDongCode> findAllByOrderByRegionCodeAscDistrictCodeAsc();

    Optional<LegalDongCode> findByRegionCodeAndDistrictCode(
            String regionCode,
            String districtCode
    );
}