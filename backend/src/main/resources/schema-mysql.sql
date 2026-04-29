CREATE TABLE IF NOT EXISTS lampposts (
    lp_number VARCHAR(32) NOT NULL PRIMARY KEY,
    latitude DECIMAL(10, 6) NOT NULL,
    longitude DECIMAL(10, 6) NOT NULL,
    northing DECIMAL(12, 2) NULL,
    easting DECIMAL(12, 2) NULL,
    lp_type VARCHAR(8) NOT NULL,
    type_name VARCHAR(64) NULL,
    device_ids VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_lamppost_type (lp_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS weather_observations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sync_run_id BIGINT NULL,
    lp_number VARCHAR(32) NOT NULL,
    device_id VARCHAR(8) NOT NULL,
    temperature_c DECIMAL(5, 2) NULL,
    humidity_percent DECIMAL(5, 2) NULL,
    wind_speed DECIMAL(6, 2) NULL,
    wind_direction_deg SMALLINT NULL,
    wind_direction_height SMALLINT NULL,
    source_observed_at DATETIME NOT NULL,
    source_processed_at DATETIME NULL,
    source_status VARCHAR(16) NULL,
    version VARCHAR(16) NULL,
    fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    raw_payload JSON NULL,
    UNIQUE KEY uk_weather_source (lp_number, device_id, source_observed_at),
    KEY idx_weather_sync_run (sync_run_id),
    KEY idx_weather_lp_time (lp_number, source_observed_at DESC),
    KEY idx_weather_observed_at (source_observed_at DESC),
    CONSTRAINT fk_weather_lamppost
        FOREIGN KEY (lp_number) REFERENCES lampposts (lp_number)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sync_runs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(24) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL,
    lamppost_count INT NOT NULL DEFAULT 0,
    fetched_count INT NOT NULL DEFAULT 0,
    saved_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    average_temperature_c DECIMAL(5, 2) NULL,
    average_humidity_percent DECIMAL(5, 2) NULL,
    average_wind_speed DECIMAL(6, 2) NULL,
    message VARCHAR(1000) NULL,
    KEY idx_sync_started_at (started_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
