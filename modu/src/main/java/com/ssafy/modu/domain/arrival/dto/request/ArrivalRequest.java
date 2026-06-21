package com.ssafy.modu.domain.arrival.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ArrivalRequest {

    @NotNull(message = "위도는 필수입니다.")
    @DecimalMin(value = "-90.0", message = "위도 값이 올바르지 않습니다.")
    @DecimalMax(value = "90.0", message = "위도 값이 올바르지 않습니다.")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다.")
    @DecimalMin(value = "-180.0", message = "경도 값이 올바르지 않습니다.")
    @DecimalMax(value = "180.0", message = "경도 값이 올바르지 않습니다.")
    private Double longitude;
}