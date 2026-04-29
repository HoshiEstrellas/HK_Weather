package com.example.hkweather.repository;

import com.example.hkweather.dto.SyncRunDto;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
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
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, "RUNNING");
            ps.setString(2, "Sync started");
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
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

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
