# 🏭 Industrial Hardware & SQL Pipeline Configurator

[![Java](https://img.shields.io/badge/Java-17%20%2F%2021%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Build-Apache%20Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![FlatLaf](https://img.shields.io/badge/UI-FlatLaf%20Dark%203.5-4E9A06)](https://www.formdev.com/flatlaf/)
[![Database](https://img.shields.io/badge/Database-H2%20Embedded%20%2B%20MariaDB%20JDBC-003B57?logo=sqlite&logoColor=white)](https://h2database.com/)
[![HikariCP](https://img.shields.io/badge/Connection%20Pool-HikariCP%205.1-blue)](https://github.com/brettwooldridge/HikariCP)
[![JUnit 5](https://img.shields.io/badge/Tests-14%20Passed-6E9F18?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> 🌐 **Sprache / Language:** [🇩🇪 Zur deutschen Version](#-deutsche-dokumentation) | [🇬🇧 Switch to English Version](#-english-documentation)

---

## 🇩🇪 Deutsche Dokumentation

Eine performante, multi-threaded **Desktop-Anwendung & Barcode-Pipeline-Konfigurator** für anspruchsvolle Fertigungsprozesse, Intralogistik und automatisierte Förderbandanlagen. Entwickelt mit **Java 17/21 LTS**, **Java Swing**, **FlatLaf (Dark Look & Feel im JetBrains-Stil)**, **HikariCP Connection Pooling** und **Embedded H2 SQL-Datenbank**.

---

### 🎯 Industrielle Problemstellung & Business Value

#### Der Flaschenhals in Produktion & Logistik
In automatisierten Werken und Verteilzentren (Paketbänder, Hochregallager, Sortierstraßen) übertragen tausende industrielle Barcode-Scanner (Sick, Zebra, Cognex) permanent Telemetrie- und Barcode-Daten über RS-232, USB oder TCP/IP-Sockets.
- Bei herkömmlichen Systemen erfordert jedes neue Barcode-Format oder geänderte Lieferantenetikett zeitraubende Code-Releases.
- Blockierende I/O-Aufrufe und unzureichend gepufferte Datenbank-Schreibzugriffe führen bei Spitzenlasten zu Datenverlust oder Bandstillstand.
- Gewichtsabweichungen (z.B. fehlende Bauteile in Kartonagen) werden oft erst beim Endkunden bemerkt.

#### Die Lösung
Dieser Konfigurator bietet ein vollständiges Desktop-Leitstand-Dashboard:
1. **Dynamischer Regex-Regel-Editor:** Erstellen, Priorisieren und Testen von Routing-Regeln mit Named Groups (`sku`, `lot`, `weight`) ohne Neustart der Anwendung.
2. **Multi-Threaded Hardware-Simulator:** Simuliert 4 Scan-Stationen parallel via `ScheduledExecutorService` mit bis zu 1.200 Scans/Minute und Echtzeit-Durchsatzanzeige.
3. **Automatische Quarantäne & Plausibilitätsprüfung:** Vergleicht das gewogene Packstückgewicht mit den Artikel-Stammdaten ($\pm 15\%$ Toleranz). Fehlerhafte Teile werden sofort als `QUARANTINE` oder `REJECTED` isoliert.
4. **Integrierter SQL Database Explorer:** Direkte Einsicht und Abfrage aller Tabellen (`scan_logs`, `routing_rules`, `articles`) mit Metadaten-Browser und 1-Klick-CSV-Export für Audit-Zwecke.
5. **Clean Architecture:** Reines JDBC mit PreparedStatements, Transaktionsverwaltung und HikariCP – maximale Ausführungsgeschwindigkeit ohne Framework-Ballast.

---

### 🏗️ System-Architektur

```mermaid
flowchart TD
    subgraph HardwareSimulation["Hardware Scanner Simulation"]
        Station1["Station 1: Wareneingang (Sick CLV690)"]
        Station2["Station 2: Sortierung (Cognex DataMan)"]
        Station3["Station 3: Montage (Zebra DS3608)"]
        Station4["Station 4: Warenausgang (Datalogic Matrix)"]
    end

    HardwareSimulation --> SimController["SimulationController (ScheduledExecutorService)"]
    SimController --> Pipeline["PipelineProcessor"]

    subgraph CorePipeline["Kernverarbeitung"]
        Pipeline --> RuleParser["RuleParserService (Regex Named Groups)"]
        RuleParser --> MasterData["ArticleDao (Stammdaten-Lookup)"]
        MasterData --> WeightTolerance{"Gewichtsabweichung <= 15%?"}
        WeightTolerance -->|Im Toleranzbereich| StatusOk["Status: PROCESSED"]
        WeightTolerance -->|Abweichung >15%| StatusQuarantine["Status: QUARANTINE"]
        RuleParser -->|Syntaxfehler / Unbekannte SKU| StatusReject["Status: REJECTED"]
    end

    StatusOk --> DB[("H2 / MariaDB SQL-Speicher")]
    StatusQuarantine --> DB
    StatusReject --> DB

    DB --> UI["FlatLaf Modern Dark Swing UI"]
```

---

### 🚀 Schnellstart & Installation

#### Voraussetzungen
- Java 17 oder 21 LTS (`JAVA_HOME` gesetzt)
- Apache Maven 3.8+

#### 1. Repository klonen & kompilieren
```powershell
git clone https://github.com/Tom7668h/java-pipeline-configurator.git
cd java-pipeline-configurator
mvn clean package -DskipTests
```

#### 2. Desktop-Anwendung starten
```powershell
java -jar target/java-pipeline-configurator-1.0.0.jar
```

#### 3. Automatisierte Tests ausführen
```powershell
mvn test
```

---

### 🧪 Test-Verifikation
```powershell
mvn test
```
```text
[INFO] Running de.tom.configurator.service.RuleParserServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.120 s
[INFO] Running de.tom.configurator.service.PipelineProcessorTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.285 s
[INFO] Running de.tom.configurator.dao.RoutingRuleDaoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.210 s
[INFO] 
[INFO] Results:
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
```

---

### 📄 Lizenz
MIT-Lizenz. Erstellt von [Tom7668h](https://github.com/Tom7668h).

<br><br>

---

## 🇬🇧 English Documentation

An enterprise-grade, multi-threaded **Desktop Application & Barcode Pipeline Configurator** engineered for high-throughput manufacturing, intralogistics, and automated conveyor systems. Built with **Java 17/21 LTS**, **Java Swing**, **FlatLaf (JetBrains-grade Dark Look & Feel)**, **HikariCP Connection Pooling**, and **Embedded H2 SQL Database**.

---

### 🎯 The Industrial Challenge & Solution

#### The Bottleneck in Manufacturing & Logistics
In automated factories and distribution centers (sorting lines, parcel belts, pallet high-bays), thousands of stationary hardware scanners (Sick, Zebra, Cognex) continuously stream telemetry and barcode data over RS-232, USB, and TCP/IP sockets.
- Conventional setups require code redeployments whenever barcode specifications or supplier labels change.
- Blocking I/O and unbuffered SQL writes cause packet drops and belt stoppages during peak throughput.
- Weight discrepancies (e.g. missing parts inside parcels) often go unnoticed until shipping.

#### The Solution
This configurator provides an all-in-one desktop command center:
1. **Dynamic Regex Rule Editor:** Create, prioritize, and toggle barcode parsing rules on the fly with live named-group extraction (`sku`, `lot`, `weight`) without application restart.
2. **Multi-Threaded Hardware Simulator:** Simulates 4 industrial scanner stations simultaneously via `ScheduledExecutorService`, processing up to 1,200 scans/minute with live KPI telemetry.
3. **Automated Quarantine & Anomaly Detection:** Validates physical package weights against master data ($\pm 15\%$ tolerance) and isolates bad parts (`QUARANTINE` / `REJECTED`) with clear audit reasons.
4. **Interactive SQL Database Explorer:** Directly browse and query local database tables (`scan_logs`, `routing_rules`, `articles`) with metadata introspection and 1-click CSV audit export.
5. **Clean Layered Architecture:** Pure JDBC with PreparedStatements, transactional connection pooling via HikariCP, and zero external runtime framework overhead.

---

### 🏗️ System Architecture

```mermaid
flowchart TD
    subgraph HardwareSimulation["Hardware Scanner Simulation"]
        Station1["Station 1: Inbound (Sick CLV690)"]
        Station2["Station 2: Sorting (Cognex DataMan)"]
        Station3["Station 3: Assembly (Zebra DS3608)"]
        Station4["Station 4: Dispatch (Datalogic Matrix)"]
    end

    HardwareSimulation --> SimController["SimulationController (ScheduledExecutorService)"]
    SimController --> Pipeline["PipelineProcessor"]

    subgraph CorePipeline["Core Processing Pipeline"]
        Pipeline --> RuleParser["RuleParserService (Regex Named Groups)"]
        RuleParser --> MasterData["ArticleDao (Master Data Lookup)"]
        MasterData --> WeightTolerance{"Weight delta <= 15%?"}
        WeightTolerance -->|Within Range| StatusOk["Status: PROCESSED"]
        WeightTolerance -->|Divergence >15%| StatusQuarantine["Status: QUARANTINE"]
        RuleParser -->|Syntax Error / Unknown SKU| StatusReject["Status: REJECTED"]
    end

    StatusOk --> DB[("H2 / MariaDB SQL Storage")]
    StatusQuarantine --> DB
    StatusReject --> DB

    DB --> UI["FlatLaf Modern Dark Swing UI"]
```

---

### 🚀 Quickstart & Installation

#### Prerequisites
- Java 17 or 21 LTS (`JAVA_HOME` set)
- Apache Maven 3.8+

#### 1. Clone Repository & Build
```bash
git clone https://github.com/Tom7668h/java-pipeline-configurator.git
cd java-pipeline-configurator
mvn clean package -DskipTests
```

#### 2. Launch Desktop Application
```bash
java -jar target/java-pipeline-configurator-1.0.0.jar
```

#### 3. Run Automated Tests
```bash
mvn test
```

---

### 🧪 Test Verification
```bash
mvn test
```
```text
[INFO] Running de.tom.configurator.service.RuleParserServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.120 s
[INFO] Running de.tom.configurator.service.PipelineProcessorTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.285 s
[INFO] Running de.tom.configurator.dao.RoutingRuleDaoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.210 s
[INFO] 
[INFO] Results:
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
```

---

### 📄 License
MIT License. Created by [Tom7668h](https://github.com/Tom7668h).
