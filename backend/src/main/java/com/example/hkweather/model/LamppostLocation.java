package com.example.hkweather.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record LamppostLocation(
        @JsonProperty("LP_NUMBER") String lpNumber,
        @JsonProperty("LP_LATITUDE") BigDecimal latitude,
        @JsonProperty("LP_LONGITUDE") BigDecimal longitude,
        @JsonProperty("LP_NORTH") BigDecimal northing,
        @JsonProperty("LP_EAST") BigDecimal easting,
        @JsonProperty("LP_TYPE") String lpType
) {
}
