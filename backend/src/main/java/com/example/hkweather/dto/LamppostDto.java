package com.example.hkweather.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LamppostDto(
        String lpNumber,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal northing,
        BigDecimal easting,
        String lpType,
        String typeName,
        String deviceIds,
        LocalDateTime updatedAt
) {
}
