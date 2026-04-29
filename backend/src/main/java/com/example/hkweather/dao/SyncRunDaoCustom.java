package com.example.hkweather.dao;

import com.example.hkweather.dto.SyncRunDto;
import java.math.BigDecimal;
import java.util.Optional;

public interface SyncRunDaoCustom {

    long start();

    void finish(
            long id,
            String status,
            int lamppostCount,
            int fetchedCount,
            int savedCount,
            int failedCount,
            BigDecimal averageTemperatureC,
            BigDecimal averageHumidityPercent,
            BigDecimal averageWindSpeed,
            String message
    );

    Optional<SyncRunDto> findLatestDto();

    Optional<SyncRunDto> findByIdDto(long id);
}
