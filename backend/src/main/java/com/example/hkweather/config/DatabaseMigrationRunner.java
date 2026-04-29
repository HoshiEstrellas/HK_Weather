package com.example.hkweather.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("weather_observations", "sync_run_id",
                "ALTER TABLE weather_observations ADD COLUMN sync_run_id BIGINT NULL AFTER id");
        addIndexIfMissing("weather_observations", "idx_weather_sync_run",
                "ALTER TABLE weather_observations ADD KEY idx_weather_sync_run (sync_run_id)");
        addColumnIfMissing("sync_runs", "average_temperature_c",
                "ALTER TABLE sync_runs ADD COLUMN average_temperature_c DECIMAL(5, 2) NULL AFTER failed_count");
        addColumnIfMissing("sync_runs", "average_humidity_percent",
                "ALTER TABLE sync_runs ADD COLUMN average_humidity_percent DECIMAL(5, 2) NULL AFTER average_temperature_c");
        addColumnIfMissing("sync_runs", "average_wind_speed",
                "ALTER TABLE sync_runs ADD COLUMN average_wind_speed DECIMAL(6, 2) NULL AFTER average_humidity_percent");
    }

    private void addColumnIfMissing(String tableName, String columnName, String sql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(sql);
        }
    }

    private void addIndexIfMissing(String tableName, String indexName, String sql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """, Integer.class, tableName, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(sql);
        }
    }
}
