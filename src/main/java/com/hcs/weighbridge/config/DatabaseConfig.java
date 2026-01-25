package com.hcs.weighbridge.config;

import com.fazecast.jSerialComm.SerialPort;
import com.hcs.weighbridge.util.LogUtil;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class DatabaseConfig {

    private static final Logger logger = LogUtil.getLogger(DatabaseConfig.class);
    private static Connection connection;
    private static boolean initialized = false;

    private static String DB_HOST = "localhost";
    private static String DB_PORT = "3306";
    private static String DB_NAME = "weighbridge";
    private static String DB_USER = "root";
    private static String DB_PASSWORD = "254680@Ryvk2002";
    private static String USE_SSL = "false";
    private static String USE_UNICODE = "true";
    private static String CHARACTER_ENCODING = "UTF-8";

    private static final String CONFIG_FILE_PATH = "dbconfig.properties";

    static {
        logger.trace("Static initialization block started for DatabaseConfig");
        loadConfiguration();
        initializeDatabaseAndTables();
        logger.trace("Static initialization completed for DatabaseConfig");
    }

    private DatabaseConfig() {
        logger.trace("DatabaseConfig private constructor called");
    }

    private static synchronized void loadConfiguration() {
        logger.debug("Loading database configuration...");
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE_PATH);

        if (!configFile.exists()) {
            logger.warn("Configuration file '{}' not found. Using default settings", CONFIG_FILE_PATH);
            System.out.println("Configuration file '" + CONFIG_FILE_PATH + "' not found.");
            System.out.println("Using default database configuration.");
            System.out.println("To customize, create '" + CONFIG_FILE_PATH + "' file with your settings.");
            logger.info("Default configuration: host={}, port={}, db={}, user={}",
                    DB_HOST, DB_PORT, DB_NAME, DB_USER);
            return;
        }

        logger.info("Found configuration file at: {}", configFile.getAbsolutePath());
        System.out.println("Loading database configuration from: " + configFile.getAbsolutePath());

        try (InputStream input = new FileInputStream(configFile)) {
            props.load(input);
            DB_HOST = props.getProperty("db.host", DB_HOST);
            DB_PORT = props.getProperty("db.port", DB_PORT);
            DB_NAME = props.getProperty("db.name", DB_NAME);
            DB_USER = props.getProperty("db.username", DB_USER);
            DB_PASSWORD = props.getProperty("db.password", "***[MASKED]***");
            USE_SSL = props.getProperty("db.useSSL", USE_SSL);
            USE_UNICODE = props.getProperty("db.useUnicode", USE_UNICODE);
            CHARACTER_ENCODING = props.getProperty("db.characterEncoding", CHARACTER_ENCODING);

            logger.info("Configuration loaded successfully from file");
            logger.debug("Database settings - Host: {}, Port: {}, Database: {}, User: {}, SSL: {}, Unicode: {}, Encoding: {}",
                    DB_HOST, DB_PORT, DB_NAME, DB_USER, USE_SSL, USE_UNICODE, CHARACTER_ENCODING);
            System.out.println("Database configuration loaded successfully.");

        } catch (IOException e) {
            logger.error("Failed to load configuration file: {}", e.getMessage(), e);
            System.err.println("Error loading configuration file: " + e.getMessage());
            System.out.println("Using default database configuration.");
            logger.warn("Falling back to default configuration due to load error");
        }
    }

    private static String getBaseUrl() {
        String url = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/";
        logger.trace("Generated base URL: {}", url);
        return url;
    }

    private static String getDbUrl() {
        String url = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME +
                "?useSSL=" + USE_SSL +
                "&useUnicode=" + USE_UNICODE +
                "&characterEncoding=" + CHARACTER_ENCODING;
        logger.trace("Generated database URL: {}", url);
        return url;
    }

    private static synchronized void initializeDatabaseAndTables() {
        if (initialized) {
            logger.debug("Database already initialized, skipping initialization");
            return;
        }

        logger.info("Starting database initialization...");
        try {
            Class.forName("com.mysql.jdbc.Driver");
            logger.debug("MySQL JDBC driver loaded successfully");

            try (
                    Connection baseConn = DriverManager.getConnection(getBaseUrl(), DB_USER, DB_PASSWORD);
                    Statement stmt = baseConn.createStatement()
            ) {
                String createDBSQL = String.format(
                        "CREATE DATABASE IF NOT EXISTS %s CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
                        DB_NAME
                );
                logger.debug("Executing database creation SQL: {}", createDBSQL);
                stmt.executeUpdate(createDBSQL);
                logger.info("Database '{}' checked/created successfully", DB_NAME);
                System.out.println("Database '" + DB_NAME + "' checked/created successfully.");
            }

            logger.debug("Attempting to connect to database: {}", getDbUrl());
            connection = DriverManager.getConnection(getDbUrl(), DB_USER, DB_PASSWORD);
            logger.info("Connected to database '{}' at {}:{}", DB_NAME, DB_HOST, DB_PORT);

            createTables();
            initialized = true;
            logger.info("Database and tables initialized successfully");
            System.out.println("Database and tables initialized successfully.");

        } catch (ClassNotFoundException e) {
            logger.error("MySQL JDBC Driver not found", e);
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        } catch (SQLException e) {
            logger.error("Failed to initialize database: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during database initialization", e);
            throw e;
        }
    }

    private static void createTables() throws SQLException {
        logger.debug("Creating/checking database tables...");

        String createWeighDataTable = "CREATE TABLE IF NOT EXISTS weigh_data (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "lorry_no VARCHAR(50) NOT NULL," +
                "first_weight INT," +
                "second_weight INT," +
                "net_weight INT," +
                "date_in VARCHAR(20)," +
                "date_out VARCHAR(20)," +
                "time_in VARCHAR(20)," +
                "time_out VARCHAR(20)," +
                "customer_name VARCHAR(100)," +
                "product_name VARCHAR(100)," +
                "driver_name VARCHAR(100)," +
                "status VARCHAR(20) DEFAULT 'PENDING'," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "INDEX idx_lorry_no (lorry_no)," +
                "INDEX idx_status (status)," +
                "INDEX idx_created_at (created_at)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

        String createAppConfigTable = "CREATE TABLE IF NOT EXISTS app_config (" +
                "config_key VARCHAR(50) PRIMARY KEY," +
                "config_value VARCHAR(100) NOT NULL," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "INDEX idx_config_key (config_key)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

        try (Statement stmt = connection.createStatement()) {
            logger.debug("Creating 'weigh_data' table if not exists");
            stmt.executeUpdate(createWeighDataTable);
            logger.info("Table 'weigh_data' checked/created successfully");
            System.out.println("Table 'weigh_data' checked/created successfully.");

            logger.debug("Creating 'app_config' table if not exists");
            stmt.executeUpdate(createAppConfigTable);
            logger.info("Table 'app_config' checked/created successfully");
            System.out.println("Table 'app_config' checked/created successfully.");

            insertDefaultConfigurations();
        } catch (SQLException e) {
            logger.error("Failed to create tables: {}", e.getMessage(), e);
            throw e;
        }
    }

    private static void insertDefaultConfigurations() {
        logger.debug("Inserting default configurations...");
        String[] defaultConfigs = {
                "INSERT IGNORE INTO app_config (config_key, config_value) VALUES ('com_port', 'COM1')",
                "INSERT IGNORE INTO app_config (config_key, config_value) VALUES ('baud_rate', '2400')",
                "INSERT IGNORE INTO app_config (config_key, config_value) VALUES ('data_bits', '7')",
                "INSERT IGNORE INTO app_config (config_key, config_value) VALUES ('stop_bits', '"+ SerialPort.ONE_STOP_BIT +"')",
                "INSERT IGNORE INTO app_config (config_key, config_value) VALUES ('parity', '"+SerialPort.EVEN_PARITY+"')",
                "INSERT IGNORE INTO app_config (config_key, config_value) VALUES ('ui_scale_factor', '1.0')",
                "INSERT IGNORE INTO app_config (config_key, config_value) VALUES ('receipt_counter', '1')"
        };

        try (Statement stmt = connection.createStatement()) {
            int successCount = 0;
            for (String sql : defaultConfigs) {
                try {
                    int rowsAffected = stmt.executeUpdate(sql);
                    if (rowsAffected > 0) {
                        successCount++;
                        logger.trace("Inserted default configuration: {}", sql);
                    }
                } catch (SQLException e) {
                    logger.warn("Failed to insert configuration: {}", e.getMessage());
                }
            }
            logger.info("Inserted {} default configurations into app_config", successCount);
            System.out.println("Default configurations inserted/checked.");
        } catch (SQLException e) {
            logger.warn("Could not insert default configurations: {}", e.getMessage(), e);
            System.err.println("Warning: Could not insert default configurations: " + e.getMessage());
        }
    }

    public static synchronized Connection getConnection() {
        logger.debug("getConnection() called");
        try {
            if (connection == null || connection.isClosed()) {
                logger.warn("Database connection is null or closed. Reinitializing...");
                initialized = false;
                initializeDatabaseAndTables();
            } else {
                logger.trace("Returning existing database connection");
            }
            return connection;
        } catch (SQLException e) {
            logger.error("Failed to get database connection: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    public static synchronized void closeConnection() {
        logger.debug("closeConnection() called");
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    logger.info("Database connection closed successfully");
                    System.out.println("Database connection closed.");
                } else {
                    logger.debug("Database connection was already closed");
                }
                initialized = false;
            } catch (SQLException e) {
                logger.error("Error closing database connection: {}", e.getMessage(), e);
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        } else {
            logger.debug("No database connection to close");
        }
    }

    public static String getDbHost() {
        logger.trace("getDbHost() returning: {}", DB_HOST);
        return DB_HOST;
    }

    public static String getDbPort() {
        logger.trace("getDbPort() returning: {}", DB_PORT);
        return DB_PORT;
    }

    public static String getDbName() {
        logger.trace("getDbName() returning: {}", DB_NAME);
        return DB_NAME;
    }

    public static String getDbUser() {
        logger.trace("getDbUser() returning: {}", DB_USER);
        return DB_USER;
    }

    public static String getConfigFilePath() {
        logger.trace("getConfigFilePath() returning: {}", CONFIG_FILE_PATH);
        return CONFIG_FILE_PATH;
    }

    public static synchronized void reloadConfiguration() {
        logger.info("Reloading database configuration...");
        loadConfiguration();
        closeConnection();
        initialized = false;
        logger.info("Configuration reloaded. New connection will use updated settings");
        System.out.println("Configuration reloaded. New connection will use updated settings.");
    }

    public static void logConnectionStatus() {
        try {
            if (connection != null && !connection.isClosed()) {
                logger.info("Database connection is ACTIVE - Database: {}, Host: {}, Port: {}",
                        DB_NAME, DB_HOST, DB_PORT);
            } else {
                logger.warn("Database connection is INACTIVE or CLOSED");
            }
        } catch (SQLException e) {
            logger.error("Error checking connection status: {}", e.getMessage(), e);
        }
    }
}