package de.tom.configurator.dao;

import de.tom.configurator.config.DatabaseConfig;
import de.tom.configurator.model.RoutingRule;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoutingRuleDao CRUD & Persistence Tests")
public class RoutingRuleDaoTest {

    private RoutingRuleDao dao;

    @BeforeEach
    public void setUp() {
        String memUrl = "jdbc:h2:mem:dao_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;MODE=MySQL";
        DatabaseConfig dbConfig = new DatabaseConfig(memUrl);
        dao = new RoutingRuleDao(dbConfig);
    }

    @Test
    @DisplayName("Should insert new rule and assign generated rule_id")
    public void testInsertRule() {
        RoutingRule rule = new RoutingRule(null, "Test Rule", "^TEST-.*", "STATION_01", "INSPECT", 3, true);
        boolean inserted = dao.insert(rule);

        assertTrue(inserted);
        assertNotNull(rule.getRuleId());

        Optional<RoutingRule> fetched = dao.findById(rule.getRuleId());
        assertTrue(fetched.isPresent());
        assertEquals("Test Rule", fetched.get().getName());
        assertEquals("^TEST-.*", fetched.get().getRegexPattern());
    }

    @Test
    @DisplayName("Should update existing rule attributes")
    public void testUpdateRule() {
        RoutingRule rule = new RoutingRule(null, "Original", "^ORIG-.*", "ALL", "ACTION", 5, true);
        dao.insert(rule);

        rule.setName("Updated Name");
        rule.setPriority(1);
        boolean updated = dao.update(rule);

        assertTrue(updated);
        Optional<RoutingRule> fetched = dao.findById(rule.getRuleId());
        assertTrue(fetched.isPresent());
        assertEquals("Updated Name", fetched.get().getName());
        assertEquals(1, fetched.get().getPriority());
    }

    @Test
    @DisplayName("Should toggle rule active state")
    public void testToggleActive() {
        RoutingRule rule = new RoutingRule(null, "Toggle Test", "^TOG-.*", "ALL", "ACTION", 5, true);
        dao.insert(rule);

        dao.toggleActive(rule.getRuleId(), false);
        Optional<RoutingRule> fetched = dao.findById(rule.getRuleId());
        assertTrue(fetched.isPresent());
        assertFalse(fetched.get().isActive());
    }

    @Test
    @DisplayName("Should filter and sort active rules by priority")
    public void testFindActiveRulesSorted() {
        dao.insert(new RoutingRule(null, "Prio 10", "^P10-.*", "ALL", "ACT", 10, true));
        dao.insert(new RoutingRule(null, "Prio 1", "^P1-.*", "ALL", "ACT", 1, true));
        dao.insert(new RoutingRule(null, "Prio 2 Inactive", "^P2-.*", "ALL", "ACT", 2, false));

        List<RoutingRule> active = dao.findActiveRulesSorted();
        assertTrue(active.size() >= 2);

        // First item should be lowest priority number
        assertEquals(1, active.get(0).getPriority());
        assertTrue(active.stream().noneMatch(r -> "Prio 2 Inactive".equals(r.getName())));
    }
}
