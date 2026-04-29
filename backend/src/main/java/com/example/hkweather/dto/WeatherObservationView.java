package com.example.hkweather.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WeatherObservationView(
        Long id,
        String lpNumber,
        String deviceId,
        BigDecimal latitude,
        BigDecimal longitude,
        String lpType,
        String typeName,
        BigDecimal temperatureC,
        BigDecimal humidityPercent,
        BigDecimal windSpeed,
        Integer windDirectionDeg,
        Integer windDirectionHeight,
        LocalDateTime sourceObservedAt,
        LocalDateTime sourceProcessedAt,
        LocalDateTime fetchedAt
) {
}
