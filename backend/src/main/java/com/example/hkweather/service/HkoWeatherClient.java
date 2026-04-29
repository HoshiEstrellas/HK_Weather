package com.example.hkweather.service;

import com.example.hkweather.config.WeatherProperties;
import com.example.hkweather.model.DeviceType;
import com.example.hkweather.model.LamppostLocation;
import com.example.hkweather.model.WeatherObservation;
import com.example.hkweather.util.HkoTimeParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class HkoWeatherClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final WeatherProperties properties;

    public HkoWeatherClient(RestClient restClient, ObjectMapper objectMapper, WeatherProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public List<LamppostLocation> fetchLamppostLocations() {
        String payload = restClient.get()
                .uri(properties.getLocationUrl())
                .retrieve()
                .body(String.class);
        return parseListPayload(payload, new TypeReference<List<LamppostLocation>>() {
        });
    }

    public List<DeviceType> fetchDeviceTypes() {
        String payload = restClient.get()
                .uri(properties.getTypeUrl())
                .retrieve()
                .body(String.class);
        return parseListPayload(payload, new TypeReference<List<DeviceType>>() {
        });
    }

    public Optional<WeatherObservation> fetchObservation(String lpNumber, String deviceId) {
        URI uri = UriComponentsBuilder.fromUri(properties.getBaseUrl())
                .queryParam("pi", lpNumber)
                .queryParam("di", deviceId)
                .build(true)
                .toUri();
        String payload = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode hko = root.path("BODY").path("HKO");
            if (hko.isMissingNode() || hko.isNull() || hko.isEmpty()) {
                return Optional.empty();
            }

            LocalDateTime observedAt = HkoTimeParser.parse(text(hko, "TS"));
            LocalDateTime processedAt = HkoTimeParser.parse(text(hko, "TP"));
            LocalDateTime fetchedAt = LocalDateTime.now();

            return Optional.of(new WeatherObservation(
                    null,
                    firstNonBlank(text(root, "PI"), lpNumber),
                    firstNonBlank(text(root, "DI"), deviceId),
                    decimal(hko, "T0"),
                    decimal(hko, "RH"),
                    decimal(hko, "WS"),
                    integer(hko, "WD"),
                    integer(hko, "DH"),
                    observedAt == null ? fetchedAt : observedAt,
                    processedAt,
                    text(root, "TS"),
                    text(hko, "VN"),
                    fetchedAt,
                    payload
            ));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse HKO weather payload for " + lpNumber + "/" + deviceId, ex);
        }
    }

    private <T> List<T> parseListPayload(String payload, TypeReference<List<T>> typeReference) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode listNode = root.isArray() ? root : root.path("value");
            if (!listNode.isArray()) {
                return List.of();
            }
            return objectMapper.convertValue(listNode, typeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse HKO list payload", ex);
        }
    }

    private BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer integer(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }
}
