package com.project.artconnect.util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Utility class to manage JDBC connections.
 * TODO: Students must implementation the getConnection logic.
 */
public class ConnectionManager {

    /**
     * Provides a connection to the MySQL database.
     * 
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        // TODO: Students should implement this using DatabaseConfig properties
        // return DriverManager.getConnection(DatabaseConfig.URL, DatabaseConfig.USER,
        // DatabaseConfig.PASSWORD);
        throw new UnsupportedOperationException("Database connection logic not yet implemented.");
    }
}
