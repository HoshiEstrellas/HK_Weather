package com.example.hkweather.service;

import com.example.hkweather.config.WeatherProperties;
import com.example.hkweather.dto.LamppostDto;
import com.example.hkweather.dto.WeatherObservationView;
import com.example.hkweather.dto.WeatherSummaryResponse;
import com.example.hkweather.repository.LamppostRepository;
import com.example.hkweather.repository.WeatherObservationRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class WeatherQueryService {

    private final LamppostRepository lamppostRepository;
    private final WeatherObservationRepository observationRepository;
    private final WeatherProperties properties;

    public WeatherQueryService(
            LamppostRepository lamppostRepository,
            WeatherObservationRepository observationRepository,
            WeatherProperties properties
    ) {
        this.lamppostRepository = lamppostRepository;
        this.observationRepository = observationRepository;
        this.properties = properties;
    }

    public WeatherSummaryResponse summary() {
        Map<String, Object> stats = observationRepository.latestStats(properties.getFetchIntervalMinutes());
        Map<String, Object> dailyExtremes = observationRepository.dailyAverageTemperatureExtremes(
                properties.getFetchIntervalMinutes()
        );
        return new WeatherSummaryResponse(
                lamppostRepository.count(),
                observationRepository.count(),
                toBigDecimal(stats.get("average_temperature_c")),
                toBigDecimal(stats.get("average_humidity_percent")),
                toBigDecimal(stats.get("average_wind_speed")),
                toBigDecimal(dailyExtremes.get("daily_highest_average_temperature_c")),
                toBigDecimal(dailyExtremes.get("daily_lowest_average_temperature_c")),
                toLocalDateTime(stats.get("latest_observed_at")),
                toLocalDateTime(stats.get("last_fetched_at")),
                toInteger(stats.get("latest_batch_count")),
                properties.getFetchIntervalMinutes()
        );
    }

    public List<WeatherObservationView> latest(String keyword, int limit) {
        return observationRepository.findLatest(keyword, limit, properties.getFetchIntervalMinutes());
    }

    public List<WeatherObservationView> globalHistory(String keyword, int limit) {
        return observationRepository.findGlobalHistory(keyword, limit, properties.getFetchIntervalMinutes());
    }

    public List<LamppostDto> lampposts() {
        return lamppostRepository.findAll();
    }

    public Optional<LamppostDto> lamppost(String lpNumber) {
        return lamppostRepository.findByLpNumber(lpNumber);
    }

    public List<WeatherObservationView> history(String lpNumber, String deviceId, int limit) {
        return observationRepository.findHistory(lpNumber, deviceId, limit);
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
}
