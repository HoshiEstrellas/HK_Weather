package com.example.hkweather.repository;

import com.example.hkweather.dto.LamppostDto;
import com.example.hkweather.model.DeviceType;
import com.example.hkweather.model.LamppostLocation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LamppostRepository {

    private final JdbcTemplate jdbcTemplate;

    public LamppostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertAll(List<LamppostLocation> lampposts, Map<String, DeviceType> deviceTypes) {
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

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
                LamppostLocation item = lampposts.get(i);
                DeviceType type = deviceTypes.get(item.lpType());
                ps.setString(1, item.lpNumber());
                ps.setBigDecimal(2, item.latitude());
                ps.setBigDecimal(3, item.longitude());
                ps.setBigDecimal(4, item.northing());
                ps.setBigDecimal(5, item.easting());
                ps.setString(6, item.lpType());
                ps.setString(7, type == null ? null : type.typeName());
                ps.setString(8, type == null ? "" : type.deviceIdsText());
            }

            @Override
            public int getBatchSize() {
                return lampposts.size();
            }
        });
    }

    public List<LamppostDto> findAll() {
        return jdbcTemplate.query("""
                SELECT lp_number, latitude, longitude, northing, easting, lp_type, type_name, device_ids, updated_at
                FROM lampposts
                ORDER BY lp_number ASC
                """, this::mapLamppost);
    }

    public Optional<LamppostDto> findByLpNumber(String lpNumber) {
        List<LamppostDto> items = jdbcTemplate.query("""
                SELECT lp_number, latitude, longitude, northing, easting, lp_type, type_name, device_ids, updated_at
                FROM lampposts
                WHERE lp_number = ?
                """, this::mapLamppost, lpNumber);
        return items.stream().findFirst();
    }

    public int count() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lampposts", Integer.class);
        return count == null ? 0 : count;
    }

    private LamppostDto mapLamppost(ResultSet rs, int rowNum) throws SQLException {
        return new LamppostDto(
                rs.getString("lp_number"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                rs.getBigDecimal("northing"),
                rs.getBigDecimal("easting"),
                rs.getString("lp_type"),
                rs.getString("type_name"),
                rs.getString("device_ids"),
                toLocalDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
