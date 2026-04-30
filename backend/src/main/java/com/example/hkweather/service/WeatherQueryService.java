package com.example.hkweather.service;

import com.example.hkweather.config.WeatherProperties;
import com.example.hkweather.dto.LamppostDto;
import com.example.hkweather.dto.SyncRunDto;
import com.example.hkweather.dto.WeatherObservationView;
import com.example.hkweather.dto.WeatherSummaryResponse;
import com.example.hkweather.repository.LamppostRepository;
import com.example.hkweather.repository.SyncRunRepository;
import com.example.hkweather.repository.WeatherObservationRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class WeatherQueryService {

    private static final ZoneId HONG_KONG_ZONE = ZoneId.of("Asia/Hong_Kong");

    private final LamppostRepository lamppostRepository;
    private final WeatherObservationRepository observationRepository;
    private final SyncRunRepository syncRunRepository;
    private final WeatherProperties properties;

    public WeatherQueryService(
            LamppostRepository lamppostRepository,
            WeatherObservationRepository observationRepository,
            SyncRunRepository syncRunRepository,
            WeatherProperties properties
    ) {
        this.lamppostRepository = lamppostRepository;
        this.observationRepository = observationRepository;
        this.syncRunRepository = syncRunRepository;
        this.properties = properties;
    }

    public WeatherSummaryResponse summary() {
        TimeWindow currentWindow = currentIntervalWindow();
        Map<String, Object> stats = observationRepository.latestStats(currentWindow.startAt(), currentWindow.endAt());
        TemperatureExtremes dailyExtremes = dailyAverageTemperatureExtremes();
        return new WeatherSummaryResponse(
                lamppostRepository.count(),
                observationRepository.count(),
                toBigDecimal(stats.get("average_temperature_c")),
                toBigDecimal(stats.get("average_humidity_percent")),
                toBigDecimal(stats.get("average_wind_speed")),
                dailyExtremes.highest(),
                dailyExtremes.lowest(),
                toLocalDateTime(stats.get("latest_observed_at")),
                toLocalDateTime(stats.get("last_fetched_at")),
                toInteger(stats.get("latest_batch_count")),
                properties.getFetchIntervalMinutes()
        );
    }

    public List<WeatherObservationView> latest(String keyword, int limit) {
        TimeWindow currentWindow = currentIntervalWindow();
        return observationRepository.findLatest(keyword, limit, currentWindow.startAt(), currentWindow.endAt());
    }

    public List<WeatherObservationView> globalHistory(String keyword, int limit) {
        TimeWindow historyWindow = previousHourHistoryWindow();
        return observationRepository.findGlobalHistory(keyword, limit, historyWindow.startAt(), historyWindow.endAt());
    }

    public List<LamppostDto> lampposts() {
        return lamppostRepository.findAll();
    }

    public Optional<LamppostDto> lamppost(String lpNumber) {
        return lamppostRepository.findByLpNumber(lpNumber);
    }

    public List<WeatherObservationView> history(String lpNumber, String deviceId, int limit) {
        TimeWindow historyWindow = previousHourHistoryWindow();
        return observationRepository.findHistory(lpNumber, deviceId, limit, historyWindow.startAt(), historyWindow.endAt());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return LocalDateTime.parse(value.toString());
    }

    private int toInteger(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private TimeWindow currentIntervalWindow() {
        int intervalMinutes = Math.max(1, properties.getFetchIntervalMinutes());
        LocalDateTime now = LocalDateTime.now(HONG_KONG_ZONE);
        int flooredMinute = (now.getMinute() / intervalMinutes) * intervalMinutes;
        LocalDateTime startAt = now.withMinute(flooredMinute).withSecond(0).withNano(0);
        return new TimeWindow(startAt, startAt.plusMinutes(intervalMinutes));
    }

    private TimeWindow previousHourHistoryWindow() {
        LocalDateTime endAt = currentIntervalWindow().startAt();
        return new TimeWindow(endAt.minusHours(1), endAt);
    }

    private TemperatureExtremes dailyAverageTemperatureExtremes() {
        int intervalMinutes = Math.max(1, properties.getFetchIntervalMinutes());
        LocalDate today = LocalDate.now(HONG_KONG_ZONE);
        List<SyncRunDto> runs = syncRunRepository.findSuccessfulRunsBetween(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        BigDecimal highest = null;
        BigDecimal lowest = null;
        for (SyncRunDto run : runs) {
            BigDecimal averageTemperature = run.averageTemperatureC();
            LocalDateTime startedAt = run.startedAt();
            if (averageTemperature == null || startedAt == null || startedAt.getMinute() % intervalMinutes != 0) {
                continue;
            }
            LocalDateTime requiredPreviousBoundary = startedAt.minusMinutes(intervalMinutes);
            boolean hasFullInterval = runs.stream()
                    .anyMatch(previous -> previous.startedAt() != null
                            && !previous.startedAt().isAfter(requiredPreviousBoundary));
            if (!hasFullInterval) {
                continue;
            }
            highest = highest == null || averageTemperature.compareTo(highest) > 0 ? averageTemperature : highest;
            lowest = lowest == null || averageTemperature.compareTo(lowest) < 0 ? averageTemperature : lowest;
        }
        return new TemperatureExtremes(highest, lowest);
    }

    private record TimeWindow(LocalDateTime startAt, LocalDateTime endAt) {
    }

    private record TemperatureExtremes(BigDecimal highest, BigDecimal lowest) {
    }
}
