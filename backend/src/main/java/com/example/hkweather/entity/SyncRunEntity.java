package com.example.hkweather.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sync_runs", indexes = {
        @Index(name = "idx_sync_started_at", columnList = "started_at")
})
public class SyncRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "lamppost_count", nullable = false)
    private int lamppostCount;

    @Column(name = "fetched_count", nullable = false)
    private int fetchedCount;

    @Column(name = "saved_count", nullable = false)
    private int savedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "average_temperature_c", precision = 5, scale = 2)
    private BigDecimal averageTemperatureC;

    @Column(name = "average_humidity_percent", precision = 5, scale = 2)
    private BigDecimal averageHumidityPercent;

    @Column(name = "average_wind_speed", precision = 6, scale = 2)
    private BigDecimal averageWindSpeed;

    @Column(name = "message", length = 1000)
    private String message;

    @PrePersist
    void prePersist() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getLamppostCount() {
        return lamppostCount;
    }

    public void setLamppostCount(int lamppostCount) {
        this.lamppostCount = lamppostCount;
    }

    public int getFetchedCount() {
        return fetchedCount;
    }

    public void setFetchedCount(int fetchedCount) {
        this.fetchedCount = fetchedCount;
    }

    public int getSavedCount() {
        return savedCount;
    }

    public void setSavedCount(int savedCount) {
        this.savedCount = savedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public BigDecimal getAverageTemperatureC() {
        return averageTemperatureC;
    }

    public void setAverageTemperatureC(BigDecimal averageTemperatureC) {
        this.averageTemperatureC = averageTemperatureC;
    }

    public BigDecimal getAverageHumidityPercent() {
        return averageHumidityPercent;
    }

    public void setAverageHumidityPercent(BigDecimal averageHumidityPercent) {
        this.averageHumidityPercent = averageHumidityPercent;
    }

    public BigDecimal getAverageWindSpeed() {
        return averageWindSpeed;
    }

    public void setAverageWindSpeed(BigDecimal averageWindSpeed) {
        this.averageWindSpeed = averageWindSpeed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
