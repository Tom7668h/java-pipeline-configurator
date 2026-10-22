package de.tom.configurator.service;

import de.tom.configurator.config.DatabaseConfig;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for the interactive SQL Database Explorer tab.
 * Dynamically executes SQL, reads metadata, and exports query results to CSV.
 */
public class DatabaseQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseQueryService.class);
    private final DatabaseConfig databaseConfig;

    public DatabaseQueryService() {
        this(DatabaseConfig.getInstance());
    }

    public DatabaseQueryService(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public static class QueryResult {
        private final boolean success;
        private final String message;
        private final long executionTimeMs;
        private final Vector<String> columnNames;
        private final Vector<Vector<Object>> data;
        private final int rowsAffected;

        public QueryResult(boolean success, String message, long executionTimeMs, Vector<String> columnNames, Vector<Vector<Object>> data, int rowsAffected) {
            this.success = success;
            this.message = message;
            this.executionTimeMs = executionTimeMs;
            this.columnNames = columnNames;
            this.data = data;
            this.rowsAffected = rowsAffected;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public Vector<String> getColumnNames() {
            return columnNames;
        }

        public Vector<Vector<Object>> getData() {
            return data;
        }

        public int getRowsAffected() {
            return rowsAffected;
        }
    }

    public QueryResult executeQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new QueryResult(false, "Query is empty", 0, new Vector<>(), new Vector<>(), 0);
        }

        long startTime = System.currentTimeMillis();
        String trimmedSql = sql.trim();

        try (Connection conn = databaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            boolean isResultSet = stmt.execute(trimmedSql);
            long duration = System.currentTimeMillis() - startTime;

            if (isResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    Vector<String> columns = new Vector<>(colCount);
                    for (int i = 1; i <= colCount; i++) {
                        columns.add(meta.getColumnLabel(i));
                    }

                    Vector<Vector<Object>> rows = new Vector<>();
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>(colCount);
                        for (int i = 1; i <= colCount; i++) {
                            Object val = rs.getObject(i);
                            row.add(val != null ? val : "NULL");
                        }
                        rows.add(row);
                    }

                    return new QueryResult(true, "Query OK (" + rows.size() + " rows retrieved)", duration, columns, rows, rows.size());
                }
            } else {
                int updateCount = stmt.getUpdateCount();
                return new QueryResult(true, "Command executed successfully (" + updateCount + " rows affected)", duration, new Vector<>(), new Vector<>(), updateCount);
            }
        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.warn("SQL Query execution error: {}", e.getMessage());
            return new QueryResult(false, "SQL Error: " + e.getMessage(), duration, new Vector<>(), new Vector<>(), 0);
        }
    }

    public List<String> getUserTableNames() {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' ORDER BY TABLE_NAME ASC";

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to list tables", e);
        }

        if (tables.isEmpty()) {
            tables.addAll(List.of("SCAN_LOGS", "ROUTING_RULES", "ARTICLES"));
        }
        return tables;
    }

    public void exportToCsv(File targetFile, Vector<String> columnNames, Vector<Vector<Object>> data) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, StandardCharsets.UTF_8))) {
            // Write Header
            writer.write(String.join(";", columnNames));
            writer.newLine();

            // Write Rows
            for (Vector<Object> row : data) {
                List<String> values = new ArrayList<>();
                for (Object o : row) {
                    String str = o != null ? o.toString() : "";
                    if (str.contains(";") || str.contains("\"") || str.contains("\n")) {
                        str = "\"" + str.replace("\"", "\"\"") + "\"";
                    }
                    values.add(str);
                }
                writer.write(String.join(";", values));
                writer.newLine();
            }
        }
    }
}
