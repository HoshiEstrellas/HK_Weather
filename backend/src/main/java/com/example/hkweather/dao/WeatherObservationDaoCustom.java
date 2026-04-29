package com.example.hkweather.dao;

import com.example.hkweather.dto.WeatherObservationView;
import com.example.hkweather.model.WeatherObservation;
import java.util.List;
import java.util.Map;

public interface WeatherObservationDaoCustom {

    int upsert(WeatherObservation observation, long syncRunId);

    List<WeatherObservationView> findLatest(String keyword, int limit, int intervalMinutes);

    Long findLatestBatchSyncRunId();

    List<WeatherObservationView> findGlobalHistory(String keyword, int limit, int intervalMinutes);

    List<WeatherObservationView> findHistory(String lpNumber, String deviceId, int limit);

    Map<String, Object> latestStats(int intervalMinutes);

    Map<String, Object> dailyAverageTemperatureExtremes(int intervalMinutes);
}
