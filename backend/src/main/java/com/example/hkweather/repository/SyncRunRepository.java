package com.example.hkweather.repository;

import com.example.hkweather.dto.SyncRunDto;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class SyncRunRepository {

    private final JdbcTemplate jdbcTemplate;

    public SyncRunRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long start() {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO sync_runs (status, message) VALUES (?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, "RUNNING");
            ps.setString(2, "Sync started");
            return ps;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    public void finish(
            long id,
            String status,
            int lamppostCount,
            int fetchedCount,
            int savedCount,
            int failedCount,
            BigDecimal averageTemperatureC,
            BigDecimal averageHumidityPercent,
            BigDecimal averageWindSpeed,
            String message
    ) {
        jdbcTemplate.update("""
                UPDATE sync_runs
                SET status = ?,
                    finished_at = CURRENT_TIMESTAMP,
                    lamppost_count = ?,
                    fetched_count = ?,
                    saved_count = ?,
                    failed_count = ?,
                    average_temperature_c = ?,
                    average_humidity_percent = ?,
                    average_wind_speed = ?,
                    message = ?
                WHERE id = ?
                """, status, lamppostCount, fetchedCount, savedCount, failedCount,
                averageTemperatureC, averageHumidityPercent, averageWindSpeed, trimMessage(message), id);
    }

    public Optional<SyncRunDto> findLatest() {
        List<SyncRunDto> rows = jdbcTemplate.query("""
                SELECT id, status, started_at, finished_at, lamppost_count, fetched_count, saved_count, failed_count,
                       average_temperature_c, average_humidity_percent, average_wind_speed, message
                FROM sync_runs
                ORDER BY id DESC
                LIMIT 1
                """, this::mapSyncRun);
        return rows.stream().findFirst();
    }

    public Optional<SyncRunDto> findById(long id) {
        List<SyncRunDto> rows = jdbcTemplate.query("""
                SELECT id, status, started_at, finished_at, lamppost_count, fetched_count, saved_count, failed_count,
                       average_temperature_c, average_humidity_percent, average_wind_speed, message
                FROM sync_runs
                WHERE id = ?
                """, this::mapSyncRun, id);
        return rows.stream().findFirst();
    }

    public List<SyncRunDto> findSuccessfulRunsBetween(LocalDateTime startAt, LocalDateTime endAt) {
        return jdbcTemplate.query("""
                SELECT id, status, started_at, finished_at, lamppost_count, fetched_count, saved_count, failed_count,
                       average_temperature_c, average_humidity_percent, average_wind_speed, message
                FROM sync_runs
                WHERE started_at >= ?
                  AND started_at < ?
                  AND status IN ('SUCCESS', 'PARTIAL_SUCCESS')
                  AND average_temperature_c IS NOT NULL
                ORDER BY started_at ASC, id ASC
                """, this::mapSyncRun, Timestamp.valueOf(startAt), Timestamp.valueOf(endAt));
    }

    private SyncRunDto mapSyncRun(ResultSet rs, int rowNum) throws SQLException {
        return new SyncRunDto(
                rs.getLong("id"),
                rs.getString("status"),
                toLocalDateTime(rs.getTimestamp("started_at")),
                toLocalDateTime(rs.getTimestamp("finished_at")),
                rs.getInt("lamppost_count"),
                rs.getInt("fetched_count"),
                rs.getInt("saved_count"),
                rs.getInt("failed_count"),
                rs.getBigDecimal("average_temperature_c"),
                rs.getBigDecimal("average_humidity_percent"),
                rs.getBigDecimal("average_wind_speed"),
                rs.getString("message")
        );
    }

    private String trimMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private long generatedId(KeyHolder keyHolder) {
        List<Map<String, Object>> keyList = keyHolder.getKeyList();
        if (keyList.isEmpty()) {
            return 0L;
        }

        Map<String, Object> keys = keyList.get(0);
        Object id = keys.getOrDefault("id", keys.get("ID"));
        if (id instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Generated sync run id was not returned");
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
