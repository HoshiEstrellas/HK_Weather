package com.example.hkweather.dao;

import com.example.hkweather.dto.WeatherObservationView;
import com.example.hkweather.model.WeatherObservation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class WeatherObservationDaoImpl implements WeatherObservationDaoCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public int upsert(WeatherObservation observation, long syncRunId) {
        String sql = """
                INSERT INTO weather_observations (
                    sync_run_id, lp_number, device_id, temperature_c, humidity_percent, wind_speed, wind_direction_deg,
                    wind_direction_height, source_observed_at, source_processed_at, source_status, version,
                    fetched_at, raw_payload
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    sync_run_id = VALUES(sync_run_id),
                    temperature_c = VALUES(temperature_c),
                    humidity_percent = VALUES(humidity_percent),
                    wind_speed = VALUES(wind_speed),
                    wind_direction_deg = VALUES(wind_direction_deg),
                    wind_direction_height = VALUES(wind_direction_height),
                    source_processed_at = VALUES(source_processed_at),
                    source_status = VALUES(source_status),
                    version = VALUES(version),
                    fetched_at = VALUES(fetched_at),
                    raw_payload = VALUES(raw_payload)
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, syncRunId);
        query.setParameter(2, observation.lpNumber());
        query.setParameter(3, observation.deviceId());
        query.setParameter(4, observation.temperatureC());
        query.setParameter(5, observation.humidityPercent());
        query.setParameter(6, observation.windSpeed());
        query.setParameter(7, observation.windDirectionDeg());
        query.setParameter(8, observation.windDirectionHeight());
        query.setParameter(9, observation.sourceObservedAt());
        query.setParameter(10, observation.sourceProcessedAt());
        query.setParameter(11, observation.sourceStatus());
        query.setParameter(12, observation.version());
        query.setParameter(13, observation.fetchedAt());
        query.setParameter(14, observation.rawPayload());
        return query.executeUpdate();
    }

    @Override
    public List<WeatherObservationView> findLatest(String keyword, int limit, int intervalMinutes) {
        String sql = """
                WITH interval_window AS (
                    SELECT
                        FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(CURRENT_TIMESTAMP) / (:intervalSeconds)) * (:intervalSeconds)) AS start_at,
                        TIMESTAMPADD(
                            SECOND,
                            :intervalSeconds,
                            FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(CURRENT_TIMESTAMP) / (:intervalSeconds)) * (:intervalSeconds))
                        ) AS end_at
                )
                SELECT
                    wo.id, wo.lp_number, wo.device_id, l.latitude, l.longitude, l.lp_type, l.type_name,
                    wo.temperature_c, wo.humidity_percent, wo.wind_speed, wo.wind_direction_deg,
                    wo.wind_direction_height, wo.source_observed_at, wo.source_processed_at, wo.fetched_at
                FROM weather_observations wo
                JOIN lampposts l ON l.lp_number = wo.lp_number
                CROSS JOIN interval_window iw
                WHERE wo.fetched_at >= iw.start_at
                  AND wo.fetched_at < iw.end_at
                  AND (:keyword = '' OR wo.lp_number LIKE CONCAT('%', :keyword, '%'))
                ORDER BY wo.fetched_at DESC, wo.source_observed_at DESC, wo.id DESC
                LIMIT :limit
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("keyword", keyword == null ? "" : keyword.trim());
        query.setParameter("limit", Math.max(1, Math.min(limit, 200)));
        query.setParameter("intervalSeconds", intervalSeconds(intervalMinutes));
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::mapView).toList();
    }

    @Override
    public Long findLatestBatchSyncRunId() {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT sync_run_id
                FROM weather_observations
                WHERE sync_run_id IS NOT NULL
                GROUP BY sync_run_id
                ORDER BY MAX(fetched_at) DESC, sync_run_id DESC
                LIMIT 1
                """).getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        Object value = rows.get(0);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    @Override
    public List<WeatherObservationView> findGlobalHistory(String keyword, int limit, int intervalMinutes) {
        String sql = """
                WITH interval_window AS (
                    SELECT FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(CURRENT_TIMESTAMP) / (:intervalSeconds)) * (:intervalSeconds)) AS start_at
                )
                SELECT
                    wo.id, wo.lp_number, wo.device_id, l.latitude, l.longitude, l.lp_type, l.type_name,
                    wo.temperature_c, wo.humidity_percent, wo.wind_speed, wo.wind_direction_deg,
                    wo.wind_direction_height, wo.source_observed_at, wo.source_processed_at, wo.fetched_at
                FROM weather_observations wo
                JOIN lampposts l ON l.lp_number = wo.lp_number
                CROSS JOIN interval_window iw
                WHERE wo.fetched_at < iw.start_at
                  AND (:keyword = '' OR wo.lp_number LIKE CONCAT('%', :keyword, '%'))
                ORDER BY wo.fetched_at DESC, wo.source_observed_at DESC, wo.id DESC
                LIMIT :limit
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("keyword", keyword == null ? "" : keyword.trim());
        query.setParameter("limit", Math.max(1, Math.min(limit, 500)));
        query.setParameter("intervalSeconds", intervalSeconds(intervalMinutes));
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::mapView).toList();
    }

    @Override
    public List<WeatherObservationView> findHistory(String lpNumber, String deviceId, int limit) {
        String sql = """
                SELECT
                    wo.id, wo.lp_number, wo.device_id, l.latitude, l.longitude, l.lp_type, l.type_name,
                    wo.temperature_c, wo.humidity_percent, wo.wind_speed, wo.wind_direction_deg,
                    wo.wind_direction_height, wo.source_observed_at, wo.source_processed_at, wo.fetched_at
                FROM weather_observations wo
                JOIN lampposts l ON l.lp_number = wo.lp_number
                WHERE wo.lp_number = :lpNumber
                  AND (:deviceId = '' OR wo.device_id = :deviceId)
                ORDER BY wo.source_observed_at DESC, wo.id DESC
                LIMIT :limit
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("lpNumber", lpNumber);
        query.setParameter("deviceId", deviceId == null ? "" : deviceId.trim());
        query.setParameter("limit", Math.max(1, Math.min(limit, 200)));
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::mapView).toList();
    }

    @Override
    public Map<String, Object> latestStats(int intervalMinutes) {
        String sql = """
                WITH interval_window AS (
                    SELECT
                        FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(CURRENT_TIMESTAMP) / (:intervalSeconds)) * (:intervalSeconds)) AS start_at,
                        TIMESTAMPADD(
                            SECOND,
                            :intervalSeconds,
                            FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(CURRENT_TIMESTAMP) / (:intervalSeconds)) * (:intervalSeconds))
                        ) AS end_at
                )
                SELECT
                    COUNT(*) AS latest_batch_count,
                    AVG(temperature_c) AS average_temperature_c,
                    AVG(humidity_percent) AS average_humidity_percent,
                    AVG(wind_speed) AS average_wind_speed,
                    MAX(source_observed_at) AS latest_observed_at,
                    MAX(fetched_at) AS last_fetched_at
                FROM weather_observations wo
                CROSS JOIN interval_window iw
                WHERE wo.fetched_at >= iw.start_at
                  AND wo.fetched_at < iw.end_at
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("intervalSeconds", intervalSeconds(intervalMinutes));
        Object[] row = (Object[]) query.getSingleResult();
        Map<String, Object> result = new HashMap<>();
        result.put("latest_batch_count", row[0]);
        result.put("average_temperature_c", row[1]);
        result.put("average_humidity_percent", row[2]);
        result.put("average_wind_speed", row[3]);
        result.put("latest_observed_at", row[4]);
        result.put("last_fetched_at", row[5]);
        return result;
    }

    @Override
    public Map<String, Object> dailyAverageTemperatureExtremes(int intervalMinutes) {
        Query query = entityManager.createNativeQuery("""
                WITH candidate_runs AS (
                    SELECT id, started_at, average_temperature_c
                    FROM sync_runs
                    WHERE started_at >= CURRENT_DATE
                      AND started_at < CURRENT_DATE + INTERVAL 1 DAY
                      AND status IN ('SUCCESS', 'PARTIAL_SUCCESS')
                      AND average_temperature_c IS NOT NULL
                      AND MOD(MINUTE(started_at), :intervalMinutes) = 0
                ),
                eligible_runs AS (
                    SELECT c.average_temperature_c
                    FROM candidate_runs c
                    WHERE EXISTS (
                        SELECT 1
                        FROM sync_runs p
                        WHERE p.started_at >= CURRENT_DATE
                          AND p.started_at <= TIMESTAMPADD(MINUTE, -1 * :intervalMinutes, c.started_at)
                          AND p.status IN ('SUCCESS', 'PARTIAL_SUCCESS')
                          AND p.average_temperature_c IS NOT NULL
                    )
                )
                SELECT
                    MAX(average_temperature_c) AS daily_highest_average_temperature_c,
                    MIN(average_temperature_c) AS daily_lowest_average_temperature_c
                FROM eligible_runs
                """);
        query.setParameter("intervalMinutes", intervalMinutes);
        Object[] row = (Object[]) query.getSingleResult();
        Map<String, Object> result = new HashMap<>();
        result.put("daily_highest_average_temperature_c", row[0]);
        result.put("daily_lowest_average_temperature_c", row[1]);
        return result;
    }

    private WeatherObservationView mapView(Object[] row) {
        return new WeatherObservationView(
                toLong(row[0]),
                row[1] == null ? null : row[1].toString(),
                row[2] == null ? null : row[2].toString(),
                toBigDecimal(row[3]),
                toBigDecimal(row[4]),
                row[5] == null ? null : row[5].toString(),
                row[6] == null ? null : row[6].toString(),
                toBigDecimal(row[7]),
                toBigDecimal(row[8]),
                toBigDecimal(row[9]),
                toInteger(row[10]),
                toInteger(row[11]),
                toLocalDateTime(row[12]),
                toLocalDateTime(row[13]),
                toLocalDateTime(row[14])
        );
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
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

    private int intervalSeconds(int intervalMinutes) {
        return Math.max(1, intervalMinutes) * 60;
    }
}
