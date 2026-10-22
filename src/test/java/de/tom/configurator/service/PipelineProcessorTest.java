package de.tom.configurator.service;

import de.tom.configurator.config.DatabaseConfig;
import de.tom.configurator.dao.ArticleDao;
import de.tom.configurator.dao.RoutingRuleDao;
import de.tom.configurator.dao.ScanLogDao;
import de.tom.configurator.model.Article;
import de.tom.configurator.model.RoutingRule;
import de.tom.configurator.model.ScanEvent;
import de.tom.configurator.model.ScanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PipelineProcessor End-to-End Ingestion Tests")
public class PipelineProcessorTest {

    private DatabaseConfig dbConfig;
    private PipelineProcessor processor;
    private ArticleDao articleDao;
    private RoutingRuleDao routingRuleDao;
    private ScanLogDao scanLogDao;

    @BeforeEach
    public void setUp() {
        // Use unique in-memory H2 database per test
        String memUrl = "jdbc:h2:mem:pipeline_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;MODE=MySQL";
        dbConfig = new DatabaseConfig(memUrl);

        articleDao = new ArticleDao(dbConfig);
        routingRuleDao = new RoutingRuleDao(dbConfig);
        scanLogDao = new ScanLogDao(dbConfig);
        processor = new PipelineProcessor(new RuleParserService(), routingRuleDao, articleDao, scanLogDao);

        // Seed test article and rules
        articleDao.save(new Article("04012345100018", "ZF Getriebefilter", 450, "Antrieb", true));
        articleDao.save(new Article("04012345100025", "Knorr Bremsmodul", 3800, "Bremsen", true));
        articleDao.save(new Article("EXP-BOS-2041", "Bosch Kraftstoffpumpe", 4200, "Kraftstoff", true));

        routingRuleDao.saveOrUpdate(new RoutingRule(null, "GS1 Inbound", "^01(?<sku>[0-9]{14})10(?<lot>[A-Z0-9]{6,10})310(?<weight>[0-9]{6})$", "ALL", "INVENTORY_BOOKING", 1, true));
        routingRuleDao.saveOrUpdate(new RoutingRule(null, "Express Dispatch", "^EXP-(?<sku>[A-Z]{3}-[0-9]{4})-W(?<weight>[0-9]{4})$", "ALL", "DISPATCH", 2, true));
    }

    @Test
    @DisplayName("Valid scan within weight tolerance should be marked PROCESSED")
    public void testValidScanProcessed() {
        String payload = "010401234510001810LOT8819A310000450"; // 450g == 450g (0% delta)
        ScanEvent event = processor.processRawScan("STATION_01_INBOUND", payload);

        assertEquals(ScanStatus.PROCESSED, event.getStatus());
        assertEquals("04012345100018", event.getExtractedSku());
        assertNotNull(event.getLogId());
        assertTrue(event.getStatusReason().contains("OK"));
    }

    @Test
    @DisplayName("Scan with weight deviation > 15% should be routed to QUARANTINE")
    public void testWeightDivergenceQuarantine() {
        String payload = "010401234510001810LOT8819A310000750"; // 750g vs 450g (+66% delta)
        ScanEvent event = processor.processRawScan("STATION_01_INBOUND", payload);

        assertEquals(ScanStatus.QUARANTINE, event.getStatus());
        assertTrue(event.getStatusReason().contains("WARN_WEIGHT_DIVERGENCE"));
        assertTrue(event.getStatusReason().contains("66.7%"));
    }

    @Test
    @DisplayName("Unknown SKU not found in ERP articles should be REJECTED")
    public void testUnknownSkuRejected() {
        String payload = "EXP-NON-9999-W4200"; // SKU NON-9999 not in articles
        ScanEvent event = processor.processRawScan("STATION_04_DISPATCH", payload);

        assertEquals(ScanStatus.REJECTED, event.getStatus());
        assertTrue(event.getStatusReason().contains("ERR_UNKNOWN_SKU"));
    }

    @Test
    @DisplayName("Corrupt barcode syntax should be REJECTED")
    public void testCorruptSyntaxRejected() {
        String payload = "CORRUPT_UNKNOWN_PAYLOAD_12345";
        ScanEvent event = processor.processRawScan("STATION_02_SORTING", payload);

        assertEquals(ScanStatus.REJECTED, event.getStatus());
        assertTrue(event.getStatusReason().contains("ERR_SYNTAX_MISMATCH"));
    }

    @Test
    @DisplayName("Scan log must be properly persisted into database")
    public void testLogPersistence() {
        long initialCount = scanLogDao.getTotalCount();
        processor.processRawScan("STATION_01_INBOUND", "010401234510001810LOT8819A310000450");
        long newCount = scanLogDao.getTotalCount();

        assertEquals(initialCount + 1, newCount);
    }
}
