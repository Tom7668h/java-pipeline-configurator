package de.tom.configurator.service;

import de.tom.configurator.model.RoutingRule;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RuleParserService Regex Matching Tests")
public class RuleParserServiceTest {

    private RuleParserService parserService;
    private List<RoutingRule> rules;

    @BeforeEach
    public void setUp() {
        parserService = new RuleParserService();
        rules = List.of(
                new RoutingRule(1, "GS1 Inbound", "^01(?<sku>[0-9]{14})10(?<lot>[A-Z0-9]{6,10})310(?<weight>[0-9]{6})$", "STATION_01_INBOUND", "INVENTORY_BOOKING", 1, true),
                new RoutingRule(2, "Sub-Assembly", "^BC-(?<sku>[A-Z0-9]{8})-(?<lot>[0-9]{4})$", "ALL", "QUALITY_CHECK", 5, true),
                new RoutingRule(3, "Express Dispatch", "^EXP-(?<sku>[A-Z]{3}-[0-9]{4})-W(?<weight>[0-9]{4})$", "STATION_04_DISPATCH", "DISPATCH", 2, true),
                new RoutingRule(4, "Inactive Fallback", "^BC-.*", "ALL", "FALLBACK", 10, false)
        );
    }

    @Test
    @DisplayName("Should parse GS1-128 Inbound barcode with SKU, Lot and Weight")
    public void testGs1Parsing() {
        String barcode = "010401234510001810LOT8819A310000450";
        Optional<RuleParserService.ParseResult> resultOpt = parserService.matchAndParse("STATION_01_INBOUND", barcode, rules);

        assertTrue(resultOpt.isPresent(), "Barcode should match GS1 Inbound rule");
        RuleParserService.ParseResult result = resultOpt.get();
        assertEquals("04012345100018", result.getSku());
        assertEquals("LOT8819A", result.getLot());
        assertEquals(450, result.getWeightG());
        assertEquals("GS1 Inbound", result.getMatchedRule().getName());
    }

    @Test
    @DisplayName("Should parse Automotive Code-39 Sub-Assembly barcode")
    public void testSubAssemblyParsing() {
        String barcode = "BC-KNORR901-2024";
        Optional<RuleParserService.ParseResult> resultOpt = parserService.matchAndParse("STATION_03_ASSEMBLY", barcode, rules);

        assertTrue(resultOpt.isPresent());
        RuleParserService.ParseResult result = resultOpt.get();
        assertEquals("KNORR901", result.getSku());
        assertEquals("2024", result.getLot());
        assertNull(result.getWeightG());
        assertEquals("Sub-Assembly", result.getMatchedRule().getName());
    }

    @Test
    @DisplayName("Should parse Express Dispatch Label with formatted SKU and weight")
    public void testExpressDispatchParsing() {
        String barcode = "EXP-BOS-2041-W4200";
        Optional<RuleParserService.ParseResult> resultOpt = parserService.matchAndParse("STATION_04_DISPATCH", barcode, rules);

        assertTrue(resultOpt.isPresent());
        RuleParserService.ParseResult result = resultOpt.get();
        assertEquals("BOS-2041", result.getSku());
        assertEquals(4200, result.getWeightG());
    }

    @Test
    @DisplayName("Should skip inactive rules during matching")
    public void testInactiveRuleSkipping() {
        String barcode = "BC-NONMATCHING";
        Optional<RuleParserService.ParseResult> resultOpt = parserService.matchAndParse(null, barcode, rules);
        assertTrue(resultOpt.isEmpty(), "Inactive Fallback rule should not be matched");
    }

    @Test
    @DisplayName("Should return empty optional when barcode has syntax error")
    public void testCorruptBarcode() {
        String barcode = "INVALID_CORRUPT_PAYLOAD";
        Optional<RuleParserService.ParseResult> resultOpt = parserService.matchAndParse(null, barcode, rules);
        assertTrue(resultOpt.isEmpty(), "Corrupt payload should not match any rule");
    }
}
