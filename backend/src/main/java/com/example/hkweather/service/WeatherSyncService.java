package com.example.hkweather.service;

import com.example.hkweather.config.WeatherProperties;
import com.example.hkweather.dto.SyncRunDto;
import com.example.hkweather.model.DeviceType;
import com.example.hkweather.model.LamppostLocation;
import com.example.hkweather.model.WeatherObservation;
import com.example.hkweather.repository.LamppostRepository;
import com.example.hkweather.repository.SyncRunRepository;
import com.example.hkweather.repository.WeatherObservationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WeatherSyncService {

    private static final Logger log = LoggerFactory.getLogger(WeatherSyncService.class);
    private static final ZoneId HONG_KONG_ZONE = ZoneId.of("Asia/Hong_Kong");

    private final HkoWeatherClient hkoWeatherClient;
    private final LamppostRepository lamppostRepository;
    private final WeatherObservationRepository observationRepository;
    private final SyncRunRepository syncRunRepository;
    private final WeatherProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public WeatherSyncService(
            HkoWeatherClient hkoWeatherClient,
            LamppostRepository lamppostRepository,
            WeatherObservationRepository observationRepository,
            SyncRunRepository syncRunRepository,
            WeatherProperties properties
    ) {
        this.hkoWeatherClient = hkoWeatherClient;
        this.lamppostRepository = lamppostRepository;
        this.observationRepository = observationRepository;
        this.syncRunRepository = syncRunRepository;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStart() {
        if (!properties.isSyncOnStart()) {
            return;
        }
        try {
            syncNow();
        } catch (Exception ex) {
            log.warn("Initial weather sync failed: {}", ex.getMessage(), ex);
        }
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Hong_Kong")
    public void scheduledSync() {
        if (!isIntervalBoundary(LocalDateTime.now(HONG_KONG_ZONE))) {
            return;
        }
        try {
            syncNow();
        } catch (Exception ex) {
            log.warn("Scheduled weather sync failed: {}", ex.getMessage(), ex);
        }
    }

    public SyncRunDto syncNow() {
        if (!running.compareAndSet(false, true)) {
            return syncRunRepository.findLatest()
                    .orElse(new SyncRunDto(null, "RUNNING", null, null, 0, 0, 0, 0,
                            null, null, null, "Sync already running"));
        }

        long syncRunId = syncRunRepository.start();
        int lamppostCount = 0;
        int fetchedCount = 0;
        int savedCount = 0;
        int failedCount = 0;
        String message = "Sync completed";
        AverageAccumulator temperatureAverage = new AverageAccumulator();
        AverageAccumulator humidityAverage = new AverageAccumulator();
        AverageAccumulator windSpeedAverage = new AverageAccumulator();

        try {
            List<DeviceType> deviceTypes = hkoWeatherClient.fetchDeviceTypes();
            Map<String, DeviceType> deviceTypeMap = deviceTypes.stream()
                    .collect(Collectors.toMap(DeviceType::lpType, Function.identity(), (left, right) -> left));

            List<LamppostLocation> lampposts = hkoWeatherClient.fetchLamppostLocations();
            if (properties.getMaxLampposts() > 0 && lampposts.size() > properties.getMaxLampposts()) {
                lampposts = lampposts.subList(0, properties.getMaxLampposts());
            }

            lamppostRepository.upsertAll(lampposts, deviceTypeMap);
            lamppostCount = lampposts.size();

            for (LamppostLocation lamppost : lampposts) {
                DeviceType deviceType = deviceTypeMap.get(lamppost.lpType());
                if (deviceType == null || deviceType.devices() == null || deviceType.devices().isEmpty()) {
                    failedCount++;
                    continue;
                }

                for (String deviceId : deviceType.devices()) {
                    try {
                        Optional<WeatherObservation> observation = hkoWeatherClient.fetchObservation(lamppost.lpNumber(), deviceId);
                        if (observation.isPresent()) {
                            WeatherObservation weatherObservation = observation.get();
                            fetchedCount++;
                            temperatureAverage.add(weatherObservation.temperatureC());
                            humidityAverage.add(weatherObservation.humidityPercent());
                            windSpeedAverage.add(weatherObservation.windSpeed());
                            int affectedRows = observationRepository.upsert(weatherObservation, syncRunId);
                            if (affectedRows > 0) {
                                savedCount++;
                            }
                        }
                    } catch (Exception ex) {
                        failedCount++;
                        log.warn("Failed to sync lamppost {} device {}: {}", lamppost.lpNumber(), deviceId, ex.getMessage());
                    }
                }
            }

            String status = failedCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS";
            if (failedCount > 0) {
                message = "Sync completed with " + failedCount + " failed request(s)";
            }
            syncRunRepository.finish(syncRunId, status, lamppostCount, fetchedCount, savedCount, failedCount,
                    temperatureAverage.average(2), humidityAverage.average(2), windSpeedAverage.average(2), message);
            log.info("Weather sync finished: lampposts={}, fetched={}, saved={}, failed={}",
                    lamppostCount, fetchedCount, savedCount, failedCount);
            return syncRunRepository.findById(syncRunId)
                    .orElseThrow(() -> new IllegalStateException("Sync run not found after finish"));
        } catch (Exception ex) {
            syncRunRepository.finish(syncRunId, "FAILED", lamppostCount, fetchedCount, savedCount, failedCount,
                    temperatureAverage.average(2), humidityAverage.average(2), windSpeedAverage.average(2), ex.getMessage());
            throw ex;
        } finally {
            running.set(false);
        }
    }

    public Optional<SyncRunDto> latestSyncRun() {
        return syncRunRepository.findLatest();
    }

    private boolean isIntervalBoundary(LocalDateTime now) {
        int intervalMinutes = properties.getFetchIntervalMinutes();
        return intervalMinutes > 0 && now.getMinute() % intervalMinutes == 0;
    }

    private static class AverageAccumulator {
        private BigDecimal total = BigDecimal.ZERO;
        private int count = 0;

        void add(BigDecimal value) {
            if (value == null) {
                return;
            }
            total = total.add(value);
            count++;
        }

        BigDecimal average(int scale) {
            if (count == 0) {
                return null;
            }
            return total.divide(BigDecimal.valueOf(count), scale, RoundingMode.HALF_UP);
        }
    }
}
