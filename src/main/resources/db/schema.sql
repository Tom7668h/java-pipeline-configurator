-- 1. Tabelle: Stammdaten Artikel
CREATE TABLE IF NOT EXISTS articles (
    sku VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    standard_weight_g INT NOT NULL,
    category VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- 2. Tabelle: Hardware Routing-Regeln
CREATE TABLE IF NOT EXISTS routing_rules (
    rule_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    regex_pattern VARCHAR(255) NOT NULL,
    station_id VARCHAR(50) NOT NULL,          -- z.B. 'STATION_01_INBOUND', 'ALL'
    target_action VARCHAR(50) NOT NULL,       -- 'INVENTORY_BOOKING', 'QUALITY_CHECK', 'DISPATCH'
    priority INT NOT NULL DEFAULT 10,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabelle: Revisionssicheres Scan-Protokoll (Audit Logs)
CREATE TABLE IF NOT EXISTS scan_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    station_id VARCHAR(50) NOT NULL,
    raw_payload VARCHAR(255) NOT NULL,
    extracted_sku VARCHAR(64),
    extracted_lot VARCHAR(50),
    extracted_weight_g INT,
    status VARCHAR(30) NOT NULL,               -- 'PROCESSED', 'QUARANTINE', 'REJECTED'
    status_reason VARCHAR(255),
    processing_time_ms BIGINT NOT NULL,
    scanned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scan_status ON scan_logs(status);
CREATE INDEX IF NOT EXISTS idx_scan_station ON scan_logs(station_id);
CREATE INDEX IF NOT EXISTS idx_scan_timestamp ON scan_logs(scanned_at);
