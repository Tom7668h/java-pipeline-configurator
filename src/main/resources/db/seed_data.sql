-- Standard-Regeln (Seeds)
MERGE INTO routing_rules (rule_id, name, regex_pattern, station_id, target_action, priority, is_active)
KEY (rule_id)
VALUES 
(1, 'GS1 128 Standard Inbound', '^01(?<sku>[0-9]{14})10(?<lot>[A-Z0-9]{6,10})310(?<weight>[0-9]{6})$', 'STATION_01_INBOUND', 'INVENTORY_BOOKING', 1, TRUE),
(2, 'Automotive Sub-Assembly', '^BC-(?<sku>[A-Z0-9]{8})-(?<lot>[0-9]{4})$', 'ALL', 'QUALITY_CHECK', 5, TRUE),
(3, 'Express Dispatch Label', '^EXP-(?<sku>[A-Z]{3}-[0-9]{4})-W(?<weight>[0-9]{4})$', 'STATION_04_DISPATCH', 'DISPATCH', 2, TRUE);

-- Demo-Artikel Stammdaten
MERGE INTO articles (sku, name, standard_weight_g, category, is_active)
KEY (sku)
VALUES
('04012345100018', 'ZF 8HP Automatik-Getriebefilter', 450, 'Antriebsstrang', TRUE),
('04012345100025', 'Knorr-Bremse EBS Bremsmodul', 3800, 'Bremssysteme', TRUE),
('BC-KNORR901', 'Druckluft-Regelventil 12V', 850, 'Pneumatik', TRUE),
('EXP-BOS-2041', 'Bosch Common-Rail Hochdruckpumpe CP4', 4200, 'Kraftstoffsystem', TRUE);
