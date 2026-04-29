package com.example.hkweather.controller;

import com.example.hkweather.dto.ApiResponse;
import com.example.hkweather.dto.LamppostDto;
import com.example.hkweather.dto.SyncRunDto;
import com.example.hkweather.dto.WeatherObservationView;
import com.example.hkweather.dto.WeatherSummaryResponse;
import com.example.hkweather.service.WeatherQueryService;
import com.example.hkweather.service.WeatherSyncService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherQueryService queryService;
    private final WeatherSyncService syncService;

    public WeatherController(WeatherQueryService queryService, WeatherSyncService syncService) {
        this.queryService = queryService;
        this.syncService = syncService;
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("ok");
    }

    @GetMapping("/summary")
    public ApiResponse<WeatherSummaryResponse> summary() {
        return ApiResponse.ok(queryService.summary());
    }

    @GetMapping("/latest")
    public ApiResponse<List<WeatherObservationView>> latest(
            @RequestParam(name = "q", defaultValue = "") String keyword,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(queryService.latest(keyword, limit));
    }

    @GetMapping("/history")
    public ApiResponse<List<WeatherObservationView>> globalHistory(
            @RequestParam(name = "q", defaultValue = "") String keyword,
            @RequestParam(name = "limit", defaultValue = "200") int limit
    ) {
        return ApiResponse.ok(queryService.globalHistory(keyword, limit));
    }

    @GetMapping("/lampposts")
    public ApiResponse<List<LamppostDto>> lampposts() {
        return ApiResponse.ok(queryService.lampposts());
    }

    @GetMapping("/lampposts/{lpNumber}")
    public ApiResponse<LamppostDto> lamppost(@PathVariable String lpNumber) {
        LamppostDto lamppost = queryService.lamppost(lpNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lamppost not found"));
        return ApiResponse.ok(lamppost);
    }

    @GetMapping("/lampposts/{lpNumber}/history")
    public ApiResponse<List<WeatherObservationView>> history(
            @PathVariable String lpNumber,
            @RequestParam(name = "deviceId", defaultValue = "") String deviceId,
            @RequestParam(name = "limit", defaultValue = "24") int limit
    ) {
        return ApiResponse.ok(queryService.history(lpNumber, deviceId, limit));
    }

    @PostMapping("/sync")
    public ApiResponse<SyncRunDto> syncNow() {
        return ApiResponse.ok(syncService.syncNow(), "Sync requested");
    }

    @GetMapping("/sync/latest")
    public ApiResponse<SyncRunDto> latestSync() {
        return ApiResponse.ok(syncService.latestSyncRun().orElse(null));
    }
}
