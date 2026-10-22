package de.tom.configurator.service;

import de.tom.configurator.dao.ArticleDao;
import de.tom.configurator.dao.RoutingRuleDao;
import de.tom.configurator.dao.ScanLogDao;
import de.tom.configurator.model.Article;
import de.tom.configurator.model.RoutingRule;
import de.tom.configurator.model.ScanEvent;
import de.tom.configurator.model.ScanStatus;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core Processing Pipeline.
 * Orchestrates: Barcode Regex Parsing -> Master Data Lookup -> Weight Plausibility Check -> Decision -> Persistence.
 */
public class PipelineProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineProcessor.class);

    public static final double MAX_WEIGHT_TOLERANCE_PCT = 15.0;

    private final RuleParserService parserService;
    private final RoutingRuleDao routingRuleDao;
    private final ArticleDao articleDao;
    private final ScanLogDao scanLogDao;

    public PipelineProcessor() {
        this(new RuleParserService(), new RoutingRuleDao(), new ArticleDao(), new ScanLogDao());
    }

    public PipelineProcessor(RuleParserService parserService, RoutingRuleDao routingRuleDao, ArticleDao articleDao, ScanLogDao scanLogDao) {
        this.parserService = parserService;
        this.routingRuleDao = routingRuleDao;
        this.articleDao = articleDao;
        this.scanLogDao = scanLogDao;
    }

    /**
     * Executes the end-to-end pipeline for an incoming hardware barcode read.
     */
    public ScanEvent processRawScan(String stationId, String rawPayload) {
        long startTime = System.currentTimeMillis();

        ScanEvent event = new ScanEvent(stationId, rawPayload);
        event.setScannedAt(new Timestamp(startTime));

        // 1. Fetch active rules
        List<RoutingRule> activeRules = routingRuleDao.findActiveRulesSorted();

        // 2. Syntax & Regex Matching
        Optional<RuleParserService.ParseResult> parseOpt = parserService.matchAndParse(stationId, rawPayload, activeRules);

        if (parseOpt.isEmpty()) {
            event.setStatus(ScanStatus.REJECTED);
            event.setStatusReason("ERR_SYNTAX_MISMATCH: No active routing rule matched barcode structure");
        } else {
            RuleParserService.ParseResult parsed = parseOpt.get();
            event.setExtractedSku(parsed.getSku());
            event.setExtractedLot(parsed.getLot());
            event.setExtractedWeightG(parsed.getWeightG());
            event.setMatchedRuleName(parsed.getMatchedRule().getName());
            event.setTargetAction(parsed.getMatchedRule().getTargetAction());

            // 3. Master Data Verification
            Optional<Article> articleOpt = articleDao.findBySku(parsed.getSku());

            if (articleOpt.isEmpty()) {
                event.setStatus(ScanStatus.REJECTED);
                event.setStatusReason("ERR_UNKNOWN_SKU: Part number '" + parsed.getSku() + "' not found in ERP articles");
            } else {
                Article article = articleOpt.get();
                event.setExpectedWeightG(article.getStandardWeightG());

                if (!article.isActive()) {
                    event.setStatus(ScanStatus.QUARANTINE);
                    event.setStatusReason("WARN_INACTIVE_ARTICLE: Part is decommissioned / blocked in ERP");
                } else if (parsed.getWeightG() != null && article.getStandardWeightG() > 0) {
                    // 4. Weight Plausibility Check (+/- 15%)
                    int standardWeight = article.getStandardWeightG();
                    int actualWeight = parsed.getWeightG();
                    double deltaPct = Math.abs(actualWeight - standardWeight) / (double) standardWeight * 100.0;

                    if (deltaPct > MAX_WEIGHT_TOLERANCE_PCT) {
                        event.setStatus(ScanStatus.QUARANTINE);
                        event.setStatusReason(String.format(Locale.US, "WARN_WEIGHT_DIVERGENCE: %.1f%% delta (expected %dg, scanned %dg)", deltaPct, standardWeight, actualWeight));
                    } else {
                        event.setStatus(ScanStatus.PROCESSED);
                        event.setStatusReason("OK: Routed to " + parsed.getMatchedRule().getTargetAction() + " (weight within tolerance)");
                    }
                } else {
                    // No weight captured in barcode or article has 0g
                    event.setStatus(ScanStatus.PROCESSED);
                    event.setStatusReason("OK: Routed to " + parsed.getMatchedRule().getTargetAction());
                }
            }
        }

        // 5. Finalize timing & persist
        long duration = System.currentTimeMillis() - startTime;
        event.setProcessingTimeMs(Math.max(1, duration));

        try {
            scanLogDao.insert(event);
        } catch (Exception e) {
            LOGGER.error("Failed to persist scan event log", e);
        }

        return event;
    }

    public RuleParserService getParserService() {
        return parserService;
    }

    public RoutingRuleDao getRoutingRuleDao() {
        return routingRuleDao;
    }

    public ArticleDao getArticleDao() {
        return articleDao;
    }

    public ScanLogDao getScanLogDao() {
        return scanLogDao;
    }
}
