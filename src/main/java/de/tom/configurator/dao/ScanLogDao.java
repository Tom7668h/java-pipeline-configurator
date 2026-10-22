package de.tom.configurator.dao;

import de.tom.configurator.config.DatabaseConfig;
import de.tom.configurator.model.ScanEvent;
import de.tom.configurator.model.ScanStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DAO for hardware scan logs and audit trail.
 */
public class ScanLogDao extends BaseDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanLogDao.class);

    public ScanLogDao() {
        super();
    }

    public ScanLogDao(DatabaseConfig databaseConfig) {
        super(databaseConfig);
    }

    public boolean insert(ScanEvent event) {
        String sql = "INSERT INTO scan_logs (station_id, raw_payload, extracted_sku, extracted_lot, extracted_weight_g, " +
                     "status, status_reason, processing_time_ms, scanned_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, event.getStationId());
            ps.setString(2, event.getRawPayload());
            ps.setString(3, event.getExtractedSku());
            ps.setString(4, event.getExtractedLot());
            if (event.getExtractedWeightG() != null) {
                ps.setInt(5, event.getExtractedWeightG());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setString(6, event.getStatus().name());
            ps.setString(7, event.getStatusReason());
            ps.setLong(8, event.getProcessingTimeMs());
            ps.setTimestamp(9, event.getScannedAt() != null ? event.getScannedAt() : new Timestamp(System.currentTimeMillis()));

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        event.setLogId(generatedKeys.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to insert scan log", e);
        }
        return false;
    }

    public List<ScanEvent> findRecentLogs(int limit) {
        List<ScanEvent> list = new ArrayList<>();
        String sql = "SELECT log_id, station_id, raw_payload, extracted_sku, extracted_lot, extracted_weight_g, " +
                     "status, status_reason, processing_time_ms, scanned_at " +
                     "FROM scan_logs ORDER BY log_id DESC LIMIT ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ScanEvent event = new ScanEvent();
                    event.setLogId(rs.getLong("log_id"));
                    event.setStationId(rs.getString("station_id"));
                    event.setRawPayload(rs.getString("raw_payload"));
                    event.setExtractedSku(rs.getString("extracted_sku"));
                    event.setExtractedLot(rs.getString("extracted_lot"));
                    int w = rs.getInt("extracted_weight_g");
                    event.setExtractedWeightG(rs.wasNull() ? null : w);
                    event.setStatus(ScanStatus.valueOf(rs.getString("status")));
                    event.setStatusReason(rs.getString("status_reason"));
                    event.setProcessingTimeMs(rs.getLong("processing_time_ms"));
                    event.setScannedAt(rs.getTimestamp("scanned_at"));
                    list.add(event);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to query recent scan logs", e);
        }
        return list;
    }

    public Map<ScanStatus, Long> getStatusCounts() {
        Map<ScanStatus, Long> counts = new HashMap<>();
        for (ScanStatus s : ScanStatus.values()) {
            counts.put(s, 0L);
        }

        String sql = "SELECT status, COUNT(*) as cnt FROM scan_logs GROUP BY status";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                try {
                    ScanStatus s = ScanStatus.valueOf(rs.getString("status"));
                    counts.put(s, rs.getLong("cnt"));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to query status counts", e);
        }
        return counts;
    }

    public long getTotalCount() {
        String sql = "SELECT COUNT(*) FROM scan_logs";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            LOGGER.error("Failed to count scan logs", e);
        }
        return 0L;
    }

    public void clearAllLogs() {
        String sql = "DELETE FROM scan_logs";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Failed to clear scan logs", e);
        }
    }
}
