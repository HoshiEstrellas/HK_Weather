package com.example.hkweather.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WeatherObservation(
        Long id,
        String lpNumber,
        String deviceId,
        BigDecimal temperatureC,
        BigDecimal humidityPercent,
        BigDecimal windSpeed,
        Integer windDirectionDeg,
        Integer windDirectionHeight,
        LocalDateTime sourceObservedAt,
        LocalDateTime sourceProcessedAt,
        String sourceStatus,
        String version,
        LocalDateTime fetchedAt,
        String rawPayload
) {
}
