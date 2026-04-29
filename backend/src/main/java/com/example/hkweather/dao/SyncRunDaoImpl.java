package com.example.hkweather.dao;

import com.example.hkweather.dto.SyncRunDto;
import com.example.hkweather.entity.SyncRunEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SyncRunDaoImpl implements SyncRunDaoCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public long start() {
        SyncRunEntity entity = new SyncRunEntity();
        entity.setStatus("RUNNING");
        entity.setMessage("Sync started");
        entityManager.persist(entity);
        entityManager.flush();
        return entity.getId() == null ? 0L : entity.getId();
    }

    @Override
    @Transactional
    public void finish(
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
    ) {
        Query query = entityManager.createNativeQuery("""
                UPDATE sync_runs
                SET status = ?,
                    finished_at = CURRENT_TIMESTAMP,
                    lamppost_count = ?,
                    fetched_count = ?,
                    saved_count = ?,
                    failed_count = ?,
                    average_temperature_c = ?,
                    average_humidity_percent = ?,
                    average_wind_speed = ?,
                    message = ?
                WHERE id = ?
                """);
        query.setParameter(1, status);
        query.setParameter(2, lamppostCount);
        query.setParameter(3, fetchedCount);
        query.setParameter(4, savedCount);
        query.setParameter(5, failedCount);
        query.setParameter(6, averageTemperatureC);
        query.setParameter(7, averageHumidityPercent);
        query.setParameter(8, averageWindSpeed);
        query.setParameter(9, trimMessage(message));
        query.setParameter(10, id);
        query.executeUpdate();
    }

    @Override
    public Optional<SyncRunDto> findLatestDto() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id, status, started_at, finished_at, lamppost_count, fetched_count, saved_count, failed_count,
                       average_temperature_c, average_humidity_percent, average_wind_speed, message
                FROM sync_runs
                ORDER BY id DESC
                LIMIT 1
                """).getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapSyncRun(rows.get(0)));
    }

    @Override
    public Optional<SyncRunDto> findByIdDto(long id) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id, status, started_at, finished_at, lamppost_count, fetched_count, saved_count, failed_count,
                       average_temperature_c, average_humidity_percent, average_wind_speed, message
                FROM sync_runs
                WHERE id = ?
                """).setParameter(1, id).getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapSyncRun(rows.get(0)));
    }

    private SyncRunDto mapSyncRun(Object[] row) {
        return new SyncRunDto(
                toLong(row[0]),
                row[1] == null ? null : row[1].toString(),
                toLocalDateTime(row[2]),
                toLocalDateTime(row[3]),
                toInt(row[4]),
                toInt(row[5]),
                toInt(row[6]),
                toInt(row[7]),
                toBigDecimal(row[8]),
                toBigDecimal(row[9]),
                toBigDecimal(row[10]),
                row[11] == null ? null : row[11].toString()
        );
    }

    private String trimMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return LocalDateTime.parse(value.toString());
    }
}
