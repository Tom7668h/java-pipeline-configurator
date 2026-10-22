package de.tom.configurator.dao;

import de.tom.configurator.config.DatabaseConfig;
import de.tom.configurator.model.RoutingRule;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DAO for hardware barcode routing rules.
 */
public class RoutingRuleDao extends BaseDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutingRuleDao.class);

    public RoutingRuleDao() {
        super();
    }

    public RoutingRuleDao(DatabaseConfig databaseConfig) {
        super(databaseConfig);
    }

    public List<RoutingRule> findAll() {
        List<RoutingRule> rules = new ArrayList<>();
        String sql = "SELECT rule_id, name, regex_pattern, station_id, target_action, priority, is_active, created_at " +
                     "FROM routing_rules ORDER BY priority ASC, rule_id ASC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rules.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to query routing rules", e);
        }
        return rules;
    }

    public List<RoutingRule> findActiveRulesSorted() {
        List<RoutingRule> rules = new ArrayList<>();
        String sql = "SELECT rule_id, name, regex_pattern, station_id, target_action, priority, is_active, created_at " +
                     "FROM routing_rules WHERE is_active = TRUE ORDER BY priority ASC, rule_id ASC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rules.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to query active routing rules", e);
        }
        return rules;
    }

    public Optional<RoutingRule> findById(int ruleId) {
        String sql = "SELECT rule_id, name, regex_pattern, station_id, target_action, priority, is_active, created_at " +
                     "FROM routing_rules WHERE rule_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ruleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to find rule by id: " + ruleId, e);
        }
        return Optional.empty();
    }

    public boolean saveOrUpdate(RoutingRule rule) {
        if (rule.getRuleId() == null) {
            return insert(rule);
        } else {
            return update(rule);
        }
    }

    public boolean insert(RoutingRule rule) {
        String sql = "INSERT INTO routing_rules (name, regex_pattern, station_id, target_action, priority, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, rule.getName());
            ps.setString(2, rule.getRegexPattern());
            ps.setString(3, rule.getStationId());
            ps.setString(4, rule.getTargetAction());
            ps.setInt(5, rule.getPriority());
            ps.setBoolean(6, rule.isActive());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        rule.setRuleId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to insert routing rule", e);
        }
        return false;
    }

    public boolean update(RoutingRule rule) {
        String sql = "UPDATE routing_rules SET name = ?, regex_pattern = ?, station_id = ?, " +
                     "target_action = ?, priority = ?, is_active = ? WHERE rule_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, rule.getName());
            ps.setString(2, rule.getRegexPattern());
            ps.setString(3, rule.getStationId());
            ps.setString(4, rule.getTargetAction());
            ps.setInt(5, rule.getPriority());
            ps.setBoolean(6, rule.isActive());
            ps.setInt(7, rule.getRuleId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("Failed to update routing rule id: " + rule.getRuleId(), e);
        }
        return false;
    }

    public boolean deleteById(int ruleId) {
        String sql = "DELETE FROM routing_rules WHERE rule_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ruleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("Failed to delete routing rule id: " + ruleId, e);
        }
        return false;
    }

    public boolean toggleActive(int ruleId, boolean active) {
        String sql = "UPDATE routing_rules SET is_active = ? WHERE rule_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, active);
            ps.setInt(2, ruleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("Failed to toggle rule active state: " + ruleId, e);
        }
        return false;
    }

    private RoutingRule mapResultSet(ResultSet rs) throws SQLException {
        RoutingRule rule = new RoutingRule();
        rule.setRuleId(rs.getInt("rule_id"));
        rule.setName(rs.getString("name"));
        rule.setRegexPattern(rs.getString("regex_pattern"));
        rule.setStationId(rs.getString("station_id"));
        rule.setTargetAction(rs.getString("target_action"));
        rule.setPriority(rs.getInt("priority"));
        rule.setActive(rs.getBoolean("is_active"));
        rule.setCreatedAt(rs.getTimestamp("created_at"));
        return rule;
    }
}
