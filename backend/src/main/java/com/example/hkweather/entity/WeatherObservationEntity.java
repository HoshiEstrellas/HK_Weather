package com.example.hkweather.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_observations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_weather_source", columnNames = {
                        "lp_number", "device_id", "source_observed_at"
                })
        },
        indexes = {
                @Index(name = "idx_weather_sync_run", columnList = "sync_run_id"),
                @Index(name = "idx_weather_lp_time", columnList = "lp_number,source_observed_at"),
                @Index(name = "idx_weather_observed_at", columnList = "source_observed_at")
        })
public class WeatherObservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "sync_run_id")
    private Long syncRunId;

    @Column(name = "lp_number", nullable = false, length = 32)
    private String lpNumber;

    @Column(name = "device_id", nullable = false, length = 8)
    private String deviceId;

    @Column(name = "temperature_c", precision = 5, scale = 2)
    private BigDecimal temperatureC;

    @Column(name = "humidity_percent", precision = 5, scale = 2)
    private BigDecimal humidityPercent;

    @Column(name = "wind_speed", precision = 6, scale = 2)
    private BigDecimal windSpeed;

    @Column(name = "wind_direction_deg")
    private Integer windDirectionDeg;

    @Column(name = "wind_direction_height")
    private Integer windDirectionHeight;

    @Column(name = "source_observed_at", nullable = false)
    private LocalDateTime sourceObservedAt;

    @Column(name = "source_processed_at")
    private LocalDateTime sourceProcessedAt;

    @Column(name = "source_status", length = 16)
    private String sourceStatus;

    @Column(name = "version", length = 16)
    private String version;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @PrePersist
    void prePersist() {
        if (fetchedAt == null) {
            fetchedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSyncRunId() {
        return syncRunId;
    }

    public void setSyncRunId(Long syncRunId) {
        this.syncRunId = syncRunId;
    }

    public String getLpNumber() {
        return lpNumber;
    }

    public void setLpNumber(String lpNumber) {
        this.lpNumber = lpNumber;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public BigDecimal getTemperatureC() {
        return temperatureC;
    }

    public void setTemperatureC(BigDecimal temperatureC) {
        this.temperatureC = temperatureC;
    }

    public BigDecimal getHumidityPercent() {
        return humidityPercent;
    }

    public void setHumidityPercent(BigDecimal humidityPercent) {
        this.humidityPercent = humidityPercent;
    }

    public BigDecimal getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(BigDecimal windSpeed) {
        this.windSpeed = windSpeed;
    }

    public Integer getWindDirectionDeg() {
        return windDirectionDeg;
    }

    public void setWindDirectionDeg(Integer windDirectionDeg) {
        this.windDirectionDeg = windDirectionDeg;
    }

    public Integer getWindDirectionHeight() {
        return windDirectionHeight;
    }

    public void setWindDirectionHeight(Integer windDirectionHeight) {
        this.windDirectionHeight = windDirectionHeight;
    }

    public LocalDateTime getSourceObservedAt() {
        return sourceObservedAt;
    }

    public void setSourceObservedAt(LocalDateTime sourceObservedAt) {
        this.sourceObservedAt = sourceObservedAt;
    }

    public LocalDateTime getSourceProcessedAt() {
        return sourceProcessedAt;
    }

    public void setSourceProcessedAt(LocalDateTime sourceProcessedAt) {
        this.sourceProcessedAt = sourceProcessedAt;
    }

    public String getSourceStatus() {
        return sourceStatus;
    }

    public void setSourceStatus(String sourceStatus) {
        this.sourceStatus = sourceStatus;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }
}
