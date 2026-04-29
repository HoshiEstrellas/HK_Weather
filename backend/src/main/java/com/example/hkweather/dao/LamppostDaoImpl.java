package com.example.hkweather.dao;

import com.example.hkweather.dto.LamppostDto;
import com.example.hkweather.model.DeviceType;
import com.example.hkweather.model.LamppostLocation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class LamppostDaoImpl implements LamppostDaoCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void upsertAll(List<LamppostLocation> lampposts, Map<String, DeviceType> deviceTypes) {
        if (lampposts == null || lampposts.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO lampposts (
                    lp_number, latitude, longitude, northing, easting, lp_type, type_name, device_ids
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    latitude = VALUES(latitude),
                    longitude = VALUES(longitude),
                    northing = VALUES(northing),
                    easting = VALUES(easting),
                    lp_type = VALUES(lp_type),
                    type_name = VALUES(type_name),
                    device_ids = VALUES(device_ids),
                    updated_at = CURRENT_TIMESTAMP
                """;
        for (LamppostLocation item : lampposts) {
            DeviceType type = deviceTypes.get(item.lpType());
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, item.lpNumber());
            query.setParameter(2, item.latitude());
            query.setParameter(3, item.longitude());
            query.setParameter(4, item.northing());
            query.setParameter(5, item.easting());
            query.setParameter(6, item.lpType());
            query.setParameter(7, type == null ? null : type.typeName());
            query.setParameter(8, type == null ? "" : type.deviceIdsText());
            query.executeUpdate();
        }
    }

    @Override
    public List<LamppostDto> findAllViews() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT lp_number, latitude, longitude, northing, easting, lp_type, type_name, device_ids, updated_at
                FROM lampposts
                ORDER BY lp_number ASC
                """).getResultList();
        return rows.stream().map(this::mapLamppost).toList();
    }

    @Override
    public Optional<LamppostDto> findByLpNumberView(String lpNumber) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT lp_number, latitude, longitude, northing, easting, lp_type, type_name, device_ids, updated_at
                FROM lampposts
                WHERE lp_number = ?
                """).setParameter(1, lpNumber).getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapLamppost(rows.get(0)));
    }

    private LamppostDto mapLamppost(Object[] row) {
        return new LamppostDto(
                row[0] == null ? null : row[0].toString(),
                toBigDecimal(row[1]),
                toBigDecimal(row[2]),
                toBigDecimal(row[3]),
                toBigDecimal(row[4]),
                row[5] == null ? null : row[5].toString(),
                row[6] == null ? null : row[6].toString(),
                row[7] == null ? null : row[7].toString(),
                toLocalDateTime(row[8])
        );
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
