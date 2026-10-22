package de.tom.configurator.service;

import de.tom.configurator.model.RoutingRule;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for regex matching and dynamic named-group extraction from hardware barcodes.
 */
public class RuleParserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleParserService.class);

    public static class ParseResult {
        private final RoutingRule matchedRule;
        private final String sku;
        private final String lot;
        private final Integer weightG;

        public ParseResult(RoutingRule matchedRule, String sku, String lot, Integer weightG) {
            this.matchedRule = matchedRule;
            this.sku = sku;
            this.lot = lot;
            this.weightG = weightG;
        }

        public RoutingRule getMatchedRule() {
            return matchedRule;
        }

        public String getSku() {
            return sku;
        }

        public String getLot() {
            return lot;
        }

        public Integer getWeightG() {
            return weightG;
        }

        @Override
        public String toString() {
            return "ParseResult{" +
                    "rule=" + matchedRule.getName() +
                    ", sku='" + sku + '\'' +
                    ", lot='" + lot + '\'' +
                    ", weightG=" + weightG +
                    '}';
        }
    }

    /**
     * Matches raw payload against active rules sorted by priority (lowest number = highest priority).
     */
    public Optional<ParseResult> matchAndParse(String stationId, String rawPayload, List<RoutingRule> rules) {
        if (rawPayload == null || rawPayload.isBlank() || rules == null || rules.isEmpty()) {
            return Optional.empty();
        }

        List<RoutingRule> sortedRules = rules.stream()
                .filter(RoutingRule::isActive)
                .filter(r -> "ALL".equalsIgnoreCase(r.getStationId()) || stationId == null || r.getStationId().equalsIgnoreCase(stationId))
                .sorted(Comparator.comparingInt(RoutingRule::getPriority))
                .toList();

        for (RoutingRule rule : sortedRules) {
            Optional<ParseResult> result = testRule(rule, rawPayload);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    /**
     * Tests a single rule against a raw payload string.
     */
    public Optional<ParseResult> testRule(RoutingRule rule, String rawPayload) {
        try {
            Pattern pattern = Pattern.compile(rule.getRegexPattern());
            Matcher matcher = pattern.matcher(rawPayload);

            if (matcher.matches()) {
                String sku = extractGroupSafe(matcher, "sku");
                String lot = extractGroupSafe(matcher, "lot");
                String weightStr = extractGroupSafe(matcher, "weight");

                Integer weightG = null;
                if (weightStr != null && !weightStr.isBlank()) {
                    try {
                        weightG = Integer.parseInt(weightStr);
                    } catch (NumberFormatException ignored) {}
                }

                return Optional.of(new ParseResult(rule, sku, lot, weightG));
            }
        } catch (PatternSyntaxException e) {
            LOGGER.warn("Invalid regex pattern in rule id {}: {}", rule.getRuleId(), rule.getRegexPattern());
        }

        return Optional.empty();
    }

    private String extractGroupSafe(Matcher matcher, String groupName) {
        try {
            return matcher.group(groupName);
        } catch (IllegalArgumentException e) {
            // Group with this name doesn't exist in the pattern
            return null;
        }
    }
}
