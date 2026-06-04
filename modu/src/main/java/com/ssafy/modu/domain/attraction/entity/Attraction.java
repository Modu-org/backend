package com.ssafy.modu.domain.attraction.entity;

import com.ssafy.modu.domain.accessibility.entity.AccessibilityInfo;
import com.ssafy.modu.domain.attraction.entity.enums.TourDetailLoadStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "attraction",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attraction_content_id", columnNames = "content_id")
        },
        indexes = {
                @Index(name = "idx_attraction_content_type", columnList = "content_type_id"),
                @Index(name = "idx_attraction_ldong", columnList = "l_dong_regn_cd,l_dong_signgu_cd"),
                @Index(name = "idx_attraction_location", columnList = "latitude,longitude")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attraction_id")
    private Long id;

    @Column(name = "content_id", nullable = false, length = 30)
    private String contentId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "address_detail")
    private String addressDetail;

    @Column(name = "zipcode", length = 20)
    private String zipcode;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "content_type_id", nullable = false, length = 10)
    private String contentTypeId;

    @Column(name = "tel", length = 100)
    private String tel;

    @Column(name = "first_image_url", length = 500)
    private String firstImageUrl;

    @Column(name = "thumbnail_image_url", length = 500)
    private String thumbnailImageUrl;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String overview;


    @Column(name = "l_dong_regn_cd", nullable = false, length = 10)
    private String lDongRegnCd;

    @Column(name = "l_dong_signgu_cd", nullable = false, length = 10)
    private String lDongSignguCd;

    @Column(name = "lcls_systm1", length = 20)
    private String lclsSystm1;

    @Column(name = "lcls_systm2", length = 20)
    private String lclsSystm2;

    @Column(name = "lcls_systm3", length = 20)
    private String lclsSystm3;

    @Column(name = "api_created_time", nullable = false)
    private LocalDateTime apiCreatedTime;

    @Column(name = "api_modified_time", nullable = false)
    private LocalDateTime apiModifiedTime;

    @Lob
    @Column(name = "homepage")
    private String homepage;

    @Column(name = "show_flag", nullable = false)
    private Boolean showFlag = true;

    @Column(name = "copyright_type", length = 20)
    private String copyrightType;

    @Column(name = "api_removed", nullable = false)
    private boolean apiRemoved = false;

    /**
     * detailCommon2(일반 관광 API 공통 상세) 호출 결과 상태.
     * SUCCESS: item 존재 및 저장 완료, NO_DATA: item 없음(더 이상 재호출하지 않음).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "common_detail_status", nullable = false, length = 30)
    private TourDetailLoadStatus commonDetailStatus = TourDetailLoadStatus.NOT_STARTED;

    /**
     * detailWithTour2(무장애 관광 API 상세) 호출 결과 상태.
     * SUCCESS: item 존재 및 저장 완료, NO_DATA: item 없음(더 이상 재호출하지 않음).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "accessibility_status", nullable = false, length = 30)
    private TourDetailLoadStatus accessibilityStatus = TourDetailLoadStatus.NOT_STARTED;

    /**
     * 무장애(KorWithService2) 목록에 등장한 관광지 여부.
     *
     * 이 값이 true이면 "무장애 상세(detailWithTour2) 및 무장애 공통상세(detailCommon2)를 우선 적재" 대상이 됨
     */
    @Column(name = "accessible_candidate", nullable = false)
    private boolean accessibleCandidate = false;

    @Column(name = "common_detail_loaded_at")
    private LocalDateTime commonDetailLoadedAt;

    @Column(name = "accessibility_loaded_at")
    private LocalDateTime accessibilityLoadedAt;

    /**
     * 목록(areaBasedSyncList2) upsert 시각.
     *
     * 상세 호출 여부와 무관하게, "목록 동기화"가 실행되었음을 기록
     */
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "attraction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccessibilityInfo> accessibilityInfos = new ArrayList<>();

    public static Attraction create(String contentId, String contentTypeId) {
        Attraction attraction = new Attraction();
        attraction.contentId = contentId;
        attraction.contentTypeId = contentTypeId;
        attraction.name = "";
        attraction.address = "";
        attraction.latitude = BigDecimal.ZERO;
        attraction.longitude = BigDecimal.ZERO;
        attraction.lDongRegnCd = "";
        attraction.lDongSignguCd = "";
        attraction.apiCreatedTime = LocalDateTime.now();
        attraction.apiModifiedTime = LocalDateTime.now();
        attraction.showFlag = true;
        attraction.commonDetailStatus = TourDetailLoadStatus.NOT_STARTED;
        attraction.accessibilityStatus = TourDetailLoadStatus.NOT_STARTED;
        attraction.accessibleCandidate = false;
        return attraction;
    }
    // 기본 메타 정보 동기화
    public void updateFromApi(
            String name,
            String address,
            String addressDetail,
            String zipcode,
            BigDecimal latitude,
            BigDecimal longitude,
            String contentTypeId,
            String tel,
            String firstImageUrl,
            String thumbnailImageUrl,
            String overview,
            String lDongRegnCd,
            String lDongSignguCd,
            String lclsSystm1,
            String lclsSystm2,
            String lclsSystm3,
            LocalDateTime apiCreatedTime,
            LocalDateTime apiModifiedTime,
            String homepage,
            Boolean showFlag,
            String copyrightType
    ) {
        this.name = defaultIfBlank(name, this.name);
        this.address = defaultIfBlank(address, "");
        this.addressDetail = emptyToNull(addressDetail);
        this.zipcode = emptyToNull(zipcode);
        this.latitude = latitude == null ? this.latitude : latitude;
        this.longitude = longitude == null ? this.longitude : longitude;
        this.contentTypeId = defaultIfBlank(contentTypeId, this.contentTypeId);
        this.tel = emptyToNull(tel);
        this.firstImageUrl = emptyToNull(firstImageUrl);
        this.thumbnailImageUrl = emptyToNull(thumbnailImageUrl);
        this.overview = emptyToNull(overview);
        this.lDongRegnCd = defaultIfBlank(lDongRegnCd, "");
        this.lDongSignguCd = defaultIfBlank(lDongSignguCd, "");
        this.lclsSystm1 = emptyToNull(lclsSystm1);
        this.lclsSystm2 = emptyToNull(lclsSystm2);
        this.lclsSystm3 = emptyToNull(lclsSystm3);
        this.apiCreatedTime = apiCreatedTime == null ? this.apiCreatedTime : apiCreatedTime;
        this.apiModifiedTime = apiModifiedTime == null ? this.apiModifiedTime : apiModifiedTime;
        this.homepage = emptyToNull(homepage);
        this.showFlag = showFlag == null ? Boolean.TRUE : showFlag;
        this.copyrightType = emptyToNull(copyrightType);
    }
    // 상세 메타 데이터 업데이트
    public void updateCommonDetail(String homepage, String overview, String tel, String firstImageUrl, String thumbnailImageUrl) {
        if (!isBlank(homepage)) this.homepage = homepage;
        if (!isBlank(overview)) this.overview = overview;
        if (!isBlank(tel)) this.tel = tel;
        if (!isBlank(firstImageUrl)) this.firstImageUrl = firstImageUrl;
        if (!isBlank(thumbnailImageUrl)) this.thumbnailImageUrl = thumbnailImageUrl;
    }

    public void markAccessibleCandidate() {
        this.accessibleCandidate = true;
    }
    // 일반 정보 적재 완료 처리
    public void markCommonDetailLoaded() {
        this.commonDetailStatus = TourDetailLoadStatus.SUCCESS;
        this.commonDetailLoadedAt = LocalDateTime.now();
    }
    // 무장애 정보 적재 완료 처리
    public void markAccessibilityLoaded() {
        this.accessibilityStatus = TourDetailLoadStatus.SUCCESS;
        this.accessibilityLoadedAt = LocalDateTime.now();
    }

    // 애초에 items/item이 없는 기본 정보들은, 데이터 없음 처리
    public void markCommonDetailNoData() {
        this.commonDetailStatus = TourDetailLoadStatus.NO_DATA;
        this.commonDetailLoadedAt = LocalDateTime.now();
    }

    // 애초에 items/item이 없는 무장애 정보들도 데이터 없음 처리
    public void markAccessibilityNoData() {
        this.accessibilityStatus = TourDetailLoadStatus.NO_DATA;
        this.accessibilityLoadedAt = LocalDateTime.now();
    }
    // 동기화 시각 설정
    public void markSyncedNow() {
        this.lastSyncedAt = LocalDateTime.now();
    }



    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private static String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public boolean isApiModifiedTimeChanged(LocalDateTime newModifiedTime) {
        return newModifiedTime != null
                && this.apiModifiedTime != null
                && newModifiedTime.isAfter(this.apiModifiedTime);
    }

    public void resetDetailStatusForModifiedApiData() {
        this.commonDetailStatus = TourDetailLoadStatus.NOT_STARTED;
        this.accessibilityStatus = TourDetailLoadStatus.NOT_STARTED;
        this.commonDetailLoadedAt = null;
        this.accessibilityLoadedAt = null;
    }
    public void markRemovedFromApi() {
        this.apiRemoved = true;
        this.accessibleCandidate = false;
    }

    public void markRestoredFromApi() {
        this.apiRemoved = false;
    }

    public boolean isAvailableForNewSchedule() {
        return !this.apiRemoved && Boolean.TRUE.equals(this.showFlag);
    }
}
