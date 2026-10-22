package de.tom.configurator.dao;

import de.tom.configurator.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base DAO providing database connection retrieval.
 */
public abstract class BaseDao {

    protected final DatabaseConfig databaseConfig;

    public BaseDao() {
        this(DatabaseConfig.getInstance());
    }

    public BaseDao(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    protected Connection getConnection() throws SQLException {
        return databaseConfig.getConnection();
    }
}
