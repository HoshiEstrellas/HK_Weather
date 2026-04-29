package com.example.hkweather.repository;

import com.example.hkweather.dto.WeatherObservationView;
import com.example.hkweather.model.WeatherObservation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WeatherObservationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public WeatherObservationRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

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

        return jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, syncRunId);
            ps.setString(2, observation.lpNumber());
            ps.setString(3, observation.deviceId());
            ps.setBigDecimal(4, observation.temperatureC());
            ps.setBigDecimal(5, observation.humidityPercent());
            ps.setBigDecimal(6, observation.windSpeed());
            setInteger(ps, 7, observation.windDirectionDeg());
            setInteger(ps, 8, observation.windDirectionHeight());
            ps.setTimestamp(9, toTimestamp(observation.sourceObservedAt()));
            ps.setTimestamp(10, toTimestamp(observation.sourceProcessedAt()));
            ps.setString(11, observation.sourceStatus());
            ps.setString(12, observation.version());
            ps.setTimestamp(13, toTimestamp(observation.fetchedAt()));
            ps.setString(14, observation.rawPayload());
            return ps;
        });
    }

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
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("keyword", keyword == null ? "" : keyword.trim())
                .addValue("limit", Math.max(1, Math.min(limit, 200)))
                .addValue("intervalSeconds", intervalSeconds(intervalMinutes));
        return namedJdbcTemplate.query(sql, params, this::mapView);
    }

    public Long findLatestBatchSyncRunId() {
        List<Long> rows = jdbcTemplate.queryForList("""
                SELECT sync_run_id
                FROM weather_observations
                WHERE sync_run_id IS NOT NULL
                GROUP BY sync_run_id
                ORDER BY MAX(fetched_at) DESC, sync_run_id DESC
                LIMIT 1
                """, Long.class);
        return rows.isEmpty() ? null : rows.get(0);
    }

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
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("keyword", keyword == null ? "" : keyword.trim())
                .addValue("limit", Math.max(1, Math.min(limit, 500)))
                .addValue("intervalSeconds", intervalSeconds(intervalMinutes));
        return namedJdbcTemplate.query(sql, params, this::mapView);
    }

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
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("lpNumber", lpNumber)
                .addValue("deviceId", deviceId == null ? "" : deviceId.trim())
                .addValue("limit", Math.max(1, Math.min(limit, 200)));
        return namedJdbcTemplate.query(sql, params, this::mapView);
    }

    public int count() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM weather_observations", Integer.class);
        return count == null ? 0 : count;
    }

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
        return namedJdbcTemplate.queryForMap(
                sql,
                new MapSqlParameterSource("intervalSeconds", intervalSeconds(intervalMinutes))
        );
    }

    public Map<String, Object> dailyAverageTemperatureExtremes(int intervalMinutes) {
        return namedJdbcTemplate.queryForMap("""
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
                """, new MapSqlParameterSource("intervalMinutes", intervalMinutes));
    }

    private WeatherObservationView mapView(ResultSet rs, int rowNum) throws SQLException {
        return new WeatherObservationView(
                rs.getLong("id"),
                rs.getString("lp_number"),
                rs.getString("device_id"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                rs.getString("lp_type"),
                rs.getString("type_name"),
                rs.getBigDecimal("temperature_c"),
                rs.getBigDecimal("humidity_percent"),
                rs.getBigDecimal("wind_speed"),
                getNullableInteger(rs, "wind_direction_deg"),
                getNullableInteger(rs, "wind_direction_height"),
                toLocalDateTime(rs.getTimestamp("source_observed_at")),
                toLocalDateTime(rs.getTimestamp("source_processed_at")),
                toLocalDateTime(rs.getTimestamp("fetched_at"))
        );
    }

    private void setInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setObject(index, null);
        } else {
            ps.setInt(index, value);
        }
    }

    private Integer getNullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private int intervalSeconds(int intervalMinutes) {
        return Math.max(1, intervalMinutes) * 60;
    }
}
