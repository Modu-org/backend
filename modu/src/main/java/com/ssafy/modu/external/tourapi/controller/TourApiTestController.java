package com.ssafy.modu.external.tourapi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.external.tourapi.TourApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/tour")
public class TourApiTestController {

    private final TourApiService tourApiService;

    @GetMapping("/common/{contentId}")
    public JsonNode common(@PathVariable String contentId) {
        return tourApiService.getCommon(contentId);
    }

    @GetMapping("/accessibility/{contentId}")
    public JsonNode accessibility(@PathVariable String contentId) {
        return tourApiService.getAccessibility(contentId);
    }
}