package com.example.hkweather.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WeatherSummaryResponse(
        int lamppostCount,
        int observationCount,
        BigDecimal averageTemperatureC,
        BigDecimal averageHumidityPercent,
        BigDecimal averageWindSpeed,
        BigDecimal dailyHighestAverageTemperatureC,
        BigDecimal dailyLowestAverageTemperatureC,
        LocalDateTime latestObservedAt,
        LocalDateTime lastFetchedAt,
        int latestBatchCount,
        int fetchIntervalMinutes
) {
}
