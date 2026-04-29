package com.example.hkweather.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DeviceType(
        @JsonProperty("LP_TYPE") String lpType,
        @JsonProperty("TYPE_NAME") String typeName,
        @JsonProperty("DEVICES") List<String> devices
) {
    public String deviceIdsText() {
        if (devices == null || devices.isEmpty()) {
            return "";
        }
        return String.join(",", devices);
    }
}
