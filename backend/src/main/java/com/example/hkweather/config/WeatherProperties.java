package com.example.hkweather.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "weather.hko")
public class WeatherProperties {

    @NotNull
    private URI baseUrl = URI.create("https://data.weather.gov.hk/weatherAPI/smart-lamppost/smart-lamppost.php");

    @NotNull
    private URI locationUrl = URI.create("https://www.hko.gov.hk/common/hko_data/smart-lamppost/files/smart_lamppost_met_device_location.json");

    @NotNull
    private URI typeUrl = URI.create("https://www.hko.gov.hk/common/hko_data/smart-lamppost/files/smart_lamppost_met_device_type.json");

    @Min(1)
    private int fetchIntervalMinutes = 10;

    @Min(1)
    private int requestTimeoutSeconds = 10;

    private boolean syncOnStart = true;

    private int maxLampposts = 0;

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public URI getLocationUrl() {
        return locationUrl;
    }

    public void setLocationUrl(URI locationUrl) {
        this.locationUrl = locationUrl;
    }

    public URI getTypeUrl() {
        return typeUrl;
    }

    public void setTypeUrl(URI typeUrl) {
        this.typeUrl = typeUrl;
    }

    public int getFetchIntervalMinutes() {
        return fetchIntervalMinutes;
    }

    public void setFetchIntervalMinutes(int fetchIntervalMinutes) {
        this.fetchIntervalMinutes = fetchIntervalMinutes;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public boolean isSyncOnStart() {
        return syncOnStart;
    }

    public void setSyncOnStart(boolean syncOnStart) {
        this.syncOnStart = syncOnStart;
    }

    public int getMaxLampposts() {
        return maxLampposts;
    }

    public void setMaxLampposts(int maxLampposts) {
        this.maxLampposts = maxLampposts;
    }
}
