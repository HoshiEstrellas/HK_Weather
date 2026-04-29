package com.example.hkweather.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SyncRunDto(
        Long id,
        String status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        int lamppostCount,
        int fetchedCount,
        int savedCount,
        int failedCount,
        BigDecimal averageTemperatureC,
        BigDecimal averageHumidityPercent,
        BigDecimal averageWindSpeed,
        String message
) {
}
