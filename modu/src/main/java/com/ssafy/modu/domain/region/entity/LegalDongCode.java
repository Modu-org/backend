package com.ssafy.modu.domain.region.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "legal_dong_code",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_legal_dong_code",
                        columnNames = {"region_code", "district_code"}
                )
        }
)
public class LegalDongCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_code", nullable = false, length = 10)
    private String regionCode;

    @Column(name = "region_name", nullable = false, length = 50)
    private String regionName;

    @Column(name = "district_code", nullable = false, length = 10)
    private String districtCode;

    @Column(name = "district_name", nullable = false, length = 50)
    private String districtName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LegalDongCode(
            String regionCode,
            String regionName,
            String districtCode,
            String districtName
    ) {
        this.regionCode = regionCode;
        this.regionName = regionName;
        this.districtCode = districtCode;
        this.districtName = districtName;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String regionName, String districtName) {
        this.regionName = regionName;
        this.districtName = districtName;
        this.updatedAt = LocalDateTime.now();
    }
}