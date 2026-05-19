package com.ssafy.modu.domain.node.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class NodeUpdateItemRequest {

    private Long nodeId;
    private Integer visitOrder;
    private LocalDate visitDate;
}
