package com.example.hkweather.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lampposts", indexes = {
        @Index(name = "idx_lamppost_type", columnList = "lp_type")
})
public class LamppostEntity {

    @Id
    @Column(name = "lp_number", nullable = false, length = 32)
    private String lpNumber;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column(name = "northing", precision = 12, scale = 2)
    private BigDecimal northing;

    @Column(name = "easting", precision = 12, scale = 2)
    private BigDecimal easting;

    @Column(name = "lp_type", nullable = false, length = 8)
    private String lpType;

    @Column(name = "type_name", length = 64)
    private String typeName;

    @Column(name = "device_ids", nullable = false, length = 64)
    private String deviceIds;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getLpNumber() {
        return lpNumber;
    }

    public void setLpNumber(String lpNumber) {
        this.lpNumber = lpNumber;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getNorthing() {
        return northing;
    }

    public void setNorthing(BigDecimal northing) {
        this.northing = northing;
    }

    public BigDecimal getEasting() {
        return easting;
    }

    public void setEasting(BigDecimal easting) {
        this.easting = easting;
    }

    public String getLpType() {
        return lpType;
    }

    public void setLpType(String lpType) {
        this.lpType = lpType;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(String deviceIds) {
        this.deviceIds = deviceIds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
