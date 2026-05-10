package com.project.artconnect.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Database configuration.
 * Loads credentials from database.properties to avoid hardcoding secrets.
 */
public class DatabaseConfig {
    public static String URL = "jdbc:mysql://localhost:3306/artconnect";
    public static String USER = "root";
    public static String PASSWORD = "";

    static {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                URL = prop.getProperty("db.url", URL);
                USER = prop.getProperty("db.user", USER);
                PASSWORD = prop.getProperty("db.password", PASSWORD);
            } else {
                System.err.println("Warning: database.properties not found. Using default/empty credentials.");
            }
        } catch (Exception ex) {
            System.err.println("Error loading database configuration: " + ex.getMessage());
        }
    }
}
