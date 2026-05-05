package com.ssafy.modu.external.tourapi.util;

import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class TourApiJsonExtractor {

    private static final DateTimeFormatter TOUR_API_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public List<JsonNode> items(JsonNode root) {
        JsonNode itemNode = root.path("response").path("body").path("items").path("item");
        List<JsonNode> result = new ArrayList<>();

        if (itemNode.isMissingNode() || itemNode.isNull()) {
            return result;
        }
        if (itemNode.isArray()) {
            itemNode.forEach(result::add);
            return result;
        }
        if (itemNode.isObject()) {
            result.add(itemNode);
        }
        return result;
    }

    public JsonNode firstItem(JsonNode root) {
        List<JsonNode> items = items(root);
        return items.isEmpty() ? null : items.get(0);
    }

    public int totalCount(JsonNode root) {
        return asInt(root.path("response").path("body"), "totalCount", 0);
    }

    public String resultCode(JsonNode root) {
        return text(root.path("response").path("header"), "resultCode");
    }

    public String resultMsg(JsonNode root) {
        return text(root.path("response").path("header"), "resultMsg");
    }

    public String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    public BigDecimal decimal(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Boolean boolByOneZero(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (value == null) {
            return null;
        }
        return "1".equals(value) || "Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }

    public LocalDateTime dateTime(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (value == null || value.length() != 14) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, TOUR_API_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    private int asInt(JsonNode node, String fieldName, int defaultValue) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return defaultValue;
        }
        return value.asInt(defaultValue);
    }
}
