package de.tom.configurator.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enterprise Database Configuration & HikariCP Connection Pool Manager.
 * Handles database pooling, automatic DDL schema execution, and initial data seeding.
 */
public class DatabaseConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfig.class);
    private static DatabaseConfig instance;

    private final HikariDataSource dataSource;

    public DatabaseConfig() {
        this(null);
    }

    public DatabaseConfig(String overrideJdbcUrl) {
        Properties props = loadProperties();

        HikariConfig config = new HikariConfig();
        String jdbcUrl = overrideJdbcUrl != null ? overrideJdbcUrl : props.getProperty("db.url", "jdbc:h2:./data/pipeline_configurator;DB_CLOSE_DELAY=-1;MODE=MySQL");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(props.getProperty("db.user", "sa"));
        config.setPassword(props.getProperty("db.password", ""));
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maxSize", "10")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "2")));
        config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connectionTimeout", "10000")));
        config.setPoolName("PipelineConfiguratorPool");

        this.dataSource = new HikariDataSource(config);
        LOGGER.info("HikariCP Connection Pool initialized for URL: {}", jdbcUrl);

        initSchemaAndSeed();
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    public static synchronized void resetInstance(String overrideJdbcUrl) {
        if (instance != null) {
            instance.close();
        }
        instance = new DatabaseConfig(overrideJdbcUrl);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not read application.properties, using defaults", e);
        }
        return properties;
    }

    private void initSchemaAndSeed() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 1. Execute schema.sql
            String schemaSql = readClasspathFile("db/schema.sql");
            if (schemaSql != null && !schemaSql.isBlank()) {
                executeSqlStatements(stmt, schemaSql);
                LOGGER.info("Database schema initialized successfully.");
            }

            // 2. Execute seed_data.sql
            String seedSql = readClasspathFile("db/seed_data.sql");
            if (seedSql != null && !seedSql.isBlank()) {
                executeSqlStatements(stmt, seedSql);
                LOGGER.info("Initial seed data applied successfully.");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize database schema or seed data", e);
        }
    }

    private void executeSqlStatements(Statement stmt, String fullSql) throws SQLException {
        // Strip line comments (-- ...)
        StringBuilder cleanSql = new StringBuilder();
        for (String line : fullSql.split("\\r?\\n")) {
            String trimmedLine = line.trim();
            if (!trimmedLine.startsWith("--")) {
                cleanSql.append(line).append("\n");
            }
        }

        String[] statements = cleanSql.toString().split(";");
        for (String s : statements) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                stmt.execute(trimmed);
            }
        }
    }

    private String readClasspathFile(String resourcePath) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load classpath resource: {}", resourcePath, e);
            return null;
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("HikariCP Connection Pool closed.");
        }
    }
}
